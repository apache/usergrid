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

package org.apache.usergrid.corepersistence.pipeline.read.elasticsearch;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.pipeline.read.AbstractPipelineOperation;
import org.apache.usergrid.corepersistence.pipeline.read.Collector;
import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.CandidateResult;
import org.apache.usergrid.persistence.index.CandidateResults;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.uuid.UUIDComparator;
import com.google.inject.Inject;

import rx.Observable;


/**
 * Loads entities from an incoming CandidateResults object and return them as results
 */
public class CandidateResultsEntityResultsCollector extends AbstractPipelineOperation<CandidateResults, ResultsPage>
    implements Collector<CandidateResults, ResultsPage> {

    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final EntityIndexFactory entityIndexFactory;


    @Inject
    public CandidateResultsEntityResultsCollector( final EntityCollectionManagerFactory entityCollectionManagerFactory,
                                                   final EntityIndexFactory entityIndexFactory ) {
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.entityIndexFactory = entityIndexFactory;
    }


    @Override
    public Observable<ResultsPage> call( final Observable<CandidateResults> candidateResultsObservable ) {


        /**
         * A bit kludgy from old 1.0 -> 2.0 apis.  Refactor this as we clean up our lower levels and create new results
         * objects
         */

        final ApplicationScope applicationScope = pipelineContext.getApplicationScope();

        final EntityCollectionManager entityCollectionManager =
            entityCollectionManagerFactory.createCollectionManager( applicationScope );


        final ApplicationEntityIndex applicationIndex =
            entityIndexFactory.createApplicationEntityIndex( applicationScope );

        final Observable<ResultsPage> searchIdSetObservable = candidateResultsObservable.flatMap( candidateResults -> {
            //flatten toa list of ids to load
            final Observable<List<Id>> candidateIds =
                Observable.from( candidateResults ).map( candidate -> candidate.getId() ).toList();

            //load the ids
            final Observable<EntitySet> entitySetObservable =
                candidateIds.flatMap( ids -> entityCollectionManager.load( ids ) );

            //now we have a collection, validate our canidate set is correct.

            return entitySetObservable
                .map( entitySet -> new EntityCollector( applicationIndex.createBatch(), entitySet, candidateResults ) )
                .doOnNext( entityCollector -> entityCollector.merge() )
                .map( entityCollector -> entityCollector.getResults() );
        } );

        //if we filter all our results, we want to continue to try the next page
        return searchIdSetObservable;
    }


    /**
     * Our collector to collect entities.  Not quite a true collector, but works within our operational flow as this state is mutable and difficult to represent functionally
     */
    private static final class EntityCollector {

        private static final Logger logger = LoggerFactory.getLogger( EntityCollector.class );
        private List<Entity> results = new ArrayList<>();

        private final EntityIndexBatch batch;
        private final CandidateResults candidateResults;
        private final EntitySet entitySet;


        public EntityCollector( final EntityIndexBatch batch, final EntitySet entitySet,
                                final CandidateResults candidateResults ) {
            this.batch = batch;
            this.entitySet = entitySet;
            this.candidateResults = candidateResults;
            this.results = new ArrayList<>( entitySet.size() );
        }


        /**
         * Merge our candidates and our entity set into results
         */
        public void merge() {

            for ( final CandidateResult candidateResult : candidateResults ) {
                validate( candidateResult );
            }

            batch.execute();
        }


        public ResultsPage getResults() {
            return new ResultsPage( results );
        }


        public EntityIndexBatch getBatch() {
            return batch;
        }


        private void validate( final CandidateResult candidateResult ) {

            final Id candidateId = candidateResult.getId();
            final UUID candidateVersion = candidateResult.getVersion();


            final MvccEntity entity = entitySet.getEntity( candidateId );


            //doesn't exist warn and drop
            if ( entity == null ) {
                logger.warn(
                    "Searched and received candidate with entityId {} and version {}, yet was not found in cassandra."
                        + "  Ignoring since this could be a region sync issue",
                    candidateId, candidateVersion );


                //TODO trigger an audit after a fail count where we explicitly try to repair from other regions

                return;

            }


            final UUID entityVersion = entity.getVersion();


            //entity is newer than ES version, could be an update or the entity is marked as deleted
            if ( UUIDComparator.staticCompare( entityVersion, candidateVersion ) > 0) {

                final Id entityId = entity.getId();
                final SearchEdge searchEdge = candidateResults.getSearchEdge();

                logger.warn( "Deindexing stale entity on edge {} for entityId {} and version {}",
                    new Object[] { searchEdge, entityId, entityVersion } );
                batch.deindex( searchEdge, entityId, entityVersion );
                return;
            }

            //ES is newer than cass, it means we haven't repaired the record in Cass, we don't want to
            //remove the ES record, since the read in cass should cause a read repair, just ignore
            if ( UUIDComparator.staticCompare( candidateVersion, entityVersion ) > 0 ) {

                final Id entityId = entity.getId();
                final SearchEdge searchEdge = candidateResults.getSearchEdge();

                logger.warn(
                    "Found a newer version in ES over cassandra for edge {} for entityId {} and version {}.  Repair "
                        + "should be run", new Object[] { searchEdge, entityId, entityVersion } );

                  //TODO trigger an audit after a fail count where we explicitly try to repair from other regions

                return;
            }

            //they're the same add it


            results.add( entity.getEntity().get() );
        }
    }
}
