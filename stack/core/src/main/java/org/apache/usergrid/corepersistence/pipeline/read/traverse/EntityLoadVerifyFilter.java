/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.pipeline.read.traverse;


import java.util.ArrayList;
import java.util.List;

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.*;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.pipeline.read.AbstractFilter;
import org.apache.usergrid.corepersistence.pipeline.read.EdgePath;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import rx.Observable;


/**
 * Loads entities from a set of Ids. and verify they are valid
 *
 * TODO refactor this into a common command that both ES search and graphSearch can use for repair and verification
 */
public class EntityLoadVerifyFilter extends AbstractFilter<FilterResult<Id>, FilterResult<Entity>> {

    private static final Logger logger = LoggerFactory.getLogger( EntityLoadVerifyFilter.class );

    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final GraphManagerFactory graphManagerFactory;
    private final ReadRepairFig readRepairFig;


    @Inject
    public EntityLoadVerifyFilter( final EntityCollectionManagerFactory entityCollectionManagerFactory,
                                   final GraphManagerFactory graphManagerFactory,
                                   final ReadRepairFig readRepairFig) {
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.graphManagerFactory = graphManagerFactory;
        this.readRepairFig = readRepairFig;
    }


    @Override
    public Observable<FilterResult<Entity>> call( final Observable<FilterResult<Id>> filterResultObservable ) {


        final ApplicationScope applicationScope = pipelineContext.getApplicationScope();
        final EntityCollectionManager entityCollectionManager =
            entityCollectionManagerFactory.createCollectionManager( applicationScope );

        //it's more efficient to make 1 network hop to get everything, then drop our results if required
        final Observable<FilterResult<Entity>> entityObservable =
            filterResultObservable.buffer( pipelineContext.getLimit() ).flatMap( bufferedIds -> {

                if (logger.isTraceEnabled()) {
                    logger.trace("Attempting to batch load ids {}", bufferedIds);
                }

                final Observable<EntitySet> entitySetObservable =
                    Observable.from( bufferedIds ).map( filterResultId -> filterResultId.getValue() ).toList()
                              .flatMap( ids -> entityCollectionManager.load( ids ) );


                //now we have a collection, validate our candidate set is correct.
                GraphManager graphManager = graphManagerFactory.createEdgeManager(applicationScope);
                return entitySetObservable.map( entitySet -> new EntityVerifier( applicationScope, graphManager,
                    entitySet, bufferedIds, readRepairFig ) )
                                          .doOnNext( entityCollector -> entityCollector.merge() ).flatMap(
                        entityCollector -> Observable.from( entityCollector.getResults() ) );
            } );

        return entityObservable;
    }


    /**
     * Our collector to collect entities.  Not quite a true collector, but works within our operational flow as this
     * state is mutable and difficult to represent functionally
     */
    private static final class EntityVerifier {

        private static final Logger logger = LoggerFactory.getLogger( EntityVerifier.class );
        private List<FilterResult<Entity>> results = new ArrayList<>();

        private final List<FilterResult<Id>> candidateResults;
        private final EntitySet entitySet;
        private final GraphManager graphManager;
        private final ApplicationScope applicationScope;
        private final ReadRepairFig readRepairFig;


        public EntityVerifier( final ApplicationScope applicationScope, final GraphManager graphManager,
                               final EntitySet entitySet, final List<FilterResult<Id>> candidateResults,
                               final ReadRepairFig readRepairFig) {
            this.applicationScope = applicationScope;
            this.graphManager = graphManager;
            this.entitySet = entitySet;
            this.candidateResults = candidateResults;
            this.results = new ArrayList<>( entitySet.size() );
            this.readRepairFig = readRepairFig;
        }


        /**
         * Merge our candidates and our entity set into results
         */
        public void merge() {

            for ( final FilterResult<Id> candidateResult : candidateResults ) {
                validate( candidateResult );
            }
        }


        public List<FilterResult<Entity>> getResults() {
            return results;
        }


        private void validate( final FilterResult<Id> filterResult ) {

            final Id candidateId = filterResult.getValue();


            final MvccEntity entity = entitySet.getEntity( candidateId );


            //doesn't exist warn and drop
            if ( entity == null || !entity.getEntity().isPresent() ) {

                // look for orphaned edges
                String edgeTypeName = CpNamingUtils.getEdgeTypeFromCollectionName(Schema.defaultCollectionName(candidateId.getType()));
                final SearchByEdge searchByEdge =
                    new SimpleSearchByEdge( applicationScope.getApplication(), edgeTypeName, candidateId, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                        Optional.absent() );

                int edgesDeleted = 0;
                List<MarkedEdge> edgeList = graphManager.loadEdgeVersions(searchByEdge).toList().toBlocking().last();
                if (edgeList.size() > 0) {
                    MarkedEdge firstEdge = edgeList.get(0);
                    long currentTimestamp = CpNamingUtils.createGraphOperationTimestamp();
                    long edgeTimestamp = firstEdge.getTimestamp();
                    long timestampDiff = currentTimestamp - edgeTimestamp;
                    long orphanDelaySecs = readRepairFig.getEdgeOrphanDelaySecs();
                    // timestamps are in 100 nanoseconds, convert from seconds
                    long allowedDiff = orphanDelaySecs * 1000L * 1000L * 10L;
                    if (timestampDiff > allowedDiff) {
                        // edges must be orphans, delete edges
                        for (MarkedEdge edge: edgeList) {
                            MarkedEdge markedEdge = graphManager.markEdge(edge).toBlocking().lastOrDefault(null);
                            if (markedEdge != null) {
                                graphManager.deleteEdge(markedEdge).toBlocking().lastOrDefault(null);
                                edgesDeleted += 1;
                            }
                        }
                    }
                }

                if (edgesDeleted > 0) {
                    logger.warn("Read graph edge and received candidate with entityId {}, yet was not found in cassandra."
                        + "  Deleted {} edges.", candidateId, edgesDeleted);
                } else {
                    logger.warn("Read graph edge and received candidate with entityId {}, yet was not found in cassandra."
                        + "  Ignoring since this could be a region sync issue", candidateId);
                }



                return;
            }

            //it exists, add it

            final Entity returnEntity = entity.getEntity().get();

            final Optional<EdgePath> parent = filterResult.getPath();

            final FilterResult<Entity> toReturn = new FilterResult<>( returnEntity, parent );

            results.add( toReturn );
        }
    }
}
