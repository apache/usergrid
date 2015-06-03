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
public class EntityLoadVerifyFilter extends AbstractFilter<FilterResult<Id>, FilterResult<Entity>>{

    private final EntityCollectionManagerFactory entityCollectionManagerFactory;


    @Inject
    public EntityLoadVerifyFilter( final EntityCollectionManagerFactory entityCollectionManagerFactory ) {
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
    }


    @Override
    public Observable<FilterResult<Entity>> call( final Observable<FilterResult<Id>> filterResultObservable ) {


        final EntityCollectionManager entityCollectionManager =
            entityCollectionManagerFactory.createCollectionManager( pipelineContext.getApplicationScope() );

        //it's more efficient to make 1 network hop to get everything, then drop our results if required
        final Observable<FilterResult<Entity>> entityObservable =
            filterResultObservable.buffer( pipelineContext.getLimit() ).flatMap( bufferedIds -> {

                    final Observable<EntitySet> entitySetObservable =
                        Observable.from( bufferedIds ).map( filterResultId -> filterResultId.getValue() ).toList()
                                  .flatMap( ids -> entityCollectionManager.load( ids ) );


                    //now we have a collection, validate our canidate set is correct.

                    return entitySetObservable.map( entitySet -> new EntityVerifier( entitySet, bufferedIds ) )
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


        public EntityVerifier( final EntitySet entitySet, final List<FilterResult<Id>> candidateResults ) {
            this.entitySet = entitySet;
            this.candidateResults = candidateResults;
            this.results = new ArrayList<>( entitySet.size() );
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
                logger.warn( "Read graph edge and received candidate with entityId {}, yet was not found in cassandra."
                        + "  Ignoring since this could be a region sync issue", candidateId );


                //TODO trigger an audit after a fail count where we explicitly try to repair from other regions

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
