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

package org.apache.usergrid.corepersistence.pipeline.read.search;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactory;
import org.apache.usergrid.persistence.index.*;
import org.apache.usergrid.persistence.index.impl.IndexProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.pipeline.read.AbstractFilter;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.VersionSet;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.uuid.UUIDComparator;
import com.google.inject.Inject;

import rx.Observable;


/**
 * Responsible for verifying candidate result versions, then emitting the Ids of these versions Input is a batch of
 * candidate results, output is a stream of validated Ids
 */
public class CandidateIdFilter extends AbstractFilter<FilterResult<Candidate>, FilterResult<Id>> {

    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final EntityIndexFactory entityIndexFactory;
    private final IndexLocationStrategyFactory indexLocationStrategyFactory;
    private final IndexProducer indexProducer;


    @Inject
    public CandidateIdFilter( final EntityCollectionManagerFactory entityCollectionManagerFactory,
                              final EntityIndexFactory entityIndexFactory,
                              final IndexLocationStrategyFactory indexLocationStrategyFactory,
                              final IndexProducer indexProducer) {
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.entityIndexFactory = entityIndexFactory;
        this.indexLocationStrategyFactory = indexLocationStrategyFactory;
        this.indexProducer = indexProducer;
    }


    @Override
    public Observable<FilterResult<Id>> call( final Observable<FilterResult<Candidate>> filterResultObservable ) {


        /**
         * A bit kludgy from old 1.0 -> 2.0 apis.  Refactor this as we clean up our lower levels and create new results
         * objects
         */

        final ApplicationScope applicationScope = pipelineContext.getApplicationScope();

        final EntityCollectionManager entityCollectionManager =
            entityCollectionManagerFactory.createCollectionManager( applicationScope );


        final EntityIndex applicationIndex =
            entityIndexFactory.createEntityIndex(indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope));

        final Observable<FilterResult<Id>> searchIdSetObservable =
            filterResultObservable.buffer( pipelineContext.getLimit() ).flatMap( candidateResults -> {
                    //flatten toa list of ids to load
                    final Observable<List<Id>> candidateIds = Observable.from( candidateResults ).map(
                        candidate -> candidate.getValue().getCandidateResult().getId() ).toList();

                    //load the ids
                    final Observable<VersionSet> versionSetObservable =
                        candidateIds.flatMap( ids -> entityCollectionManager.getLatestVersion( ids ) );

                    //now we have a collection, validate our canidate set is correct.

                    return versionSetObservable.map(
                        entitySet -> new EntityCollector( applicationIndex.createBatch(), entitySet,
                            candidateResults, indexProducer ) ).doOnNext( entityCollector -> entityCollector.merge() ).flatMap(
                        entityCollector -> Observable.from( entityCollector.collectResults() ) );
                } );

        return searchIdSetObservable;
    }


    /**
     * Map a new cp entity to an old entity.  May be null if not present
     */
    private static final class EntityCollector {

        private static final Logger logger = LoggerFactory.getLogger( EntityCollector.class );
        private List<FilterResult<Id>> results = new ArrayList<>();

        private final EntityIndexBatch batch;
        private final List<FilterResult<Candidate>> candidateResults;
        private final IndexProducer indexProducer;
        private final VersionSet versionSet;


        public EntityCollector( final EntityIndexBatch batch, final VersionSet versionSet,
                                final List<FilterResult<Candidate>> candidateResults, final IndexProducer indexProducer ) {
            this.batch = batch;
            this.versionSet = versionSet;
            this.candidateResults = candidateResults;
            this.indexProducer = indexProducer;
            this.results = new ArrayList<>( versionSet.size() );
        }


        /**
         * Merge our candidates and our entity set into results
         */
        public void merge() {

            for ( final FilterResult<Candidate> candidateResult : candidateResults ) {
                validate( candidateResult );
            }

            indexProducer.put( batch.build()).toBlocking().lastOrDefault(null);//want to rethrow if batch fails

        }


        public List<FilterResult<Id>> collectResults() {
            return results;
        }


        /**
         * Validate each candidate results vs the data loaded from cass
         */
        private void validate( final FilterResult<Candidate> filterCandidate ) {

            final CandidateResult candidateResult = filterCandidate.getValue().getCandidateResult();

            final SearchEdge searchEdge = filterCandidate.getValue().getSearchEdge();

            final MvccLogEntry logEntry = versionSet.getMaxVersion( candidateResult.getId() );

            final UUID candidateVersion = candidateResult.getVersion();

            final UUID entityVersion = logEntry.getVersion();

            final Id entityId = logEntry.getEntityId();

            //entity is newer than ES version
            if ( UUIDComparator.staticCompare( entityVersion, candidateVersion ) > 0 ) {

                logger.warn( "Deindexing stale entity on edge {} for entityId {} and version {}",
                    new Object[] { searchEdge, entityId, entityVersion } );
                batch.deindex( searchEdge, entityId, entityVersion );
                return;
            }

            //ES is newer than cass, it means we haven't repaired the record in Cass, we don't want to
            //remove the ES record, since the read in cass should cause a read repair, just ignore
            if ( UUIDComparator.staticCompare( candidateVersion, entityVersion ) > 0 ) {

                logger.warn(
                    "Found a newer version in ES over cassandra for edge {} for entityId {} and version {}.  Repair "
                        + "should be run", new Object[] { searchEdge, entityId, entityVersion } );
            }

            //they're the same add it

            final FilterResult<Id> result = new FilterResult<>( entityId, filterCandidate.getPath() );

            results.add( result );
        }
    }
}
