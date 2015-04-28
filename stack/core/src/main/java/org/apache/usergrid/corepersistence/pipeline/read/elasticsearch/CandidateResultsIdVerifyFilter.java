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
import org.apache.usergrid.corepersistence.pipeline.read.PipelineOperation;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.VersionSet;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.CandidateResult;
import org.apache.usergrid.persistence.index.CandidateResults;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.uuid.UUIDComparator;
import com.google.inject.Inject;

import rx.Observable;


/**
 * Responsible for verifying candidate result versions, then emitting the Ids of these versions
 * Input is a batch of candidate results, output is a stream of validated Ids
 */
public class CandidateResultsIdVerifyFilter extends AbstractPipelineOperation<CandidateResults, Id>
    implements PipelineOperation<CandidateResults, Id> {

    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final EntityIndexFactory entityIndexFactory;


    @Inject
    public CandidateResultsIdVerifyFilter( final EntityCollectionManagerFactory entityCollectionManagerFactory,
                                           final EntityIndexFactory entityIndexFactory ) {
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.entityIndexFactory = entityIndexFactory;
    }



    @Override
    public Observable<Id> call( final Observable<CandidateResults> observable ) {


        /**
         * A bit kludgy from old 1.0 -> 2.0 apis.  Refactor this as we clean up our lower levels and create new results
         * objects
         */

        final ApplicationScope applicationScope = pipelineContext.getApplicationScope();

        final EntityCollectionManager entityCollectionManager =
            entityCollectionManagerFactory.createCollectionManager( applicationScope );


        final ApplicationEntityIndex applicationIndex =
            entityIndexFactory.createApplicationEntityIndex( applicationScope );

        final Observable<Id> searchIdSetObservable = observable.flatMap( candidateResults -> {
            //flatten toa list of ids to load
            final Observable<List<Id>> candidateIds =
                Observable.from( candidateResults ).map( candidate -> candidate.getId() ).toList();

            //load the ids
            final Observable<VersionSet> versionSetObservable =
                candidateIds.flatMap( ids -> entityCollectionManager.getLatestVersion( ids ) );

            //now we have a collection, validate our canidate set is correct.

            return versionSetObservable
                .map( entitySet -> new EntityCollector( applicationIndex.createBatch(), entitySet, candidateResults ) )
                .doOnNext( entityCollector -> entityCollector.merge() )
                .flatMap( entityCollector -> Observable.from(  entityCollector.collectResults() ) );
        } );

        return searchIdSetObservable;
    }


    /**
     * Map a new cp entity to an old entity.  May be null if not present
     */
    private static final class EntityCollector {

        private static final Logger logger = LoggerFactory.getLogger( EntityCollector.class );
        private List<Id> results = new ArrayList<>();

        private final EntityIndexBatch batch;
        private final CandidateResults candidateResults;
        private final VersionSet versionSet;


        public EntityCollector( final EntityIndexBatch batch, final VersionSet versionSet,
                                final CandidateResults candidateResults ) {
            this.batch = batch;
            this.versionSet = versionSet;
            this.candidateResults = candidateResults;
            this.results = new ArrayList<>( versionSet.size() );
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


        public List<Id> collectResults() {
            return results;
        }


        /**
         * Validate each candidate results vs the data loaded from cass
         * @param candidateResult
         */
        private void validate( final CandidateResult candidateResult ) {

            final MvccLogEntry logEntry = versionSet.getMaxVersion( candidateResult.getId() );

            final UUID candidateVersion = candidateResult.getVersion();

            final UUID entityVersion = logEntry.getVersion();

            final Id entityId = logEntry.getEntityId();

            //entity is newer than ES version
            if ( UUIDComparator.staticCompare( entityVersion, candidateVersion ) > 0 ) {

                final SearchEdge searchEdge = candidateResults.getSearchEdge();

                logger.warn( "Deindexing stale entity on edge {} for entityId {} and version {}",
                    new Object[] { searchEdge, entityId, entityVersion } );
                batch.deindex( searchEdge, entityId, entityVersion );
                return;
            }

            //ES is newer than cass, it means we haven't repaired the record in Cass, we don't want to
            //remove the ES record, since the read in cass should cause a read repair, just ignore
            if ( UUIDComparator.staticCompare( candidateVersion, entityVersion ) > 0 ) {

                final SearchEdge searchEdge = candidateResults.getSearchEdge();

                logger.warn(
                    "Found a newer version in ES over cassandra for edge {} for entityId {} and version {}.  Repair "
                        + "should be run", new Object[] { searchEdge, entityId, entityVersion } );
            }

            //they're the same add it

            results.add( entityId );
        }
    }


}
