/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.results;


import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.uuid.UUIDComparator;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;


public class FilteringLoader implements ResultsLoader {

    private static final Logger logger = LoggerFactory.getLogger( FilteringLoader.class );

    private final ManagerCache managerCache;
    private final ResultsVerifier resultsVerifier;
    private final ApplicationScope applicationScope;
    private final IndexScope indexScope;
    private final EntityIndexBatch indexBatch;


    /**
     * Create an instance of a filter loader
     *
     * @param managerCache The manager cache to load
     * @param resultsVerifier The verifier to verify the candidate results
     * @param applicationScope The application scope to perform the load
     * @param indexScope The index scope used in the search
     */
    protected FilteringLoader( final ManagerCache managerCache, final ResultsVerifier resultsVerifier,
                               final ApplicationScope applicationScope, final IndexScope indexScope ) {

        this.managerCache = managerCache;
        this.resultsVerifier = resultsVerifier;
        this.applicationScope = applicationScope;
        this.indexScope = indexScope;

        final ApplicationEntityIndex index = managerCache.getEntityIndex( applicationScope );

        indexBatch = index.createBatch();
    }


    @Override
    public Results loadResults( final CandidateResults crs ) {


        if ( crs.size() == 0 ) {
            return new Results();
        }


        // For each entity, holds the index it appears in our candidates for keeping ordering correct
        final Map<Id, Integer> orderIndex = new HashMap<>( crs.size() );

        // Maps the entity ids to our candidates
        final Map<Id, CandidateResult> maxCandidateMapping = new HashMap<>( crs.size() );


        final Iterator<CandidateResult> iter = crs.iterator();


        // TODO, in this case we're "optimizing" due to the limitations of collection scope.
        // Perhaps  we should change the API to just be an application, then an "owner" scope?

        // Go through the candidates and group them by scope for more efficient retrieval.
        // Also remove duplicates before we even make a network call
        for ( int i = 0; iter.hasNext(); i++ ) {

            final CandidateResult currentCandidate = iter.next();

            final Id entityId = currentCandidate.getId();

            //check if we've seen this candidate by id
            final CandidateResult previousMax = maxCandidateMapping.get( entityId );

            //its not been seen, save it
            if ( previousMax == null ) {
                maxCandidateMapping.put( entityId, currentCandidate );
                orderIndex.put( entityId, i );
                continue;
            }

            //we have seen it, compare them

            final UUID previousMaxVersion = previousMax.getVersion();

            final UUID currentVersion = currentCandidate.getVersion();


            final CandidateResult toRemove;
            final CandidateResult toKeep;

            //current is newer than previous.  Remove previous and keep current
            if ( UUIDComparator.staticCompare( currentVersion, previousMaxVersion ) > 0 ) {
                toRemove = previousMax;
                toKeep = currentCandidate;
            }
            //previously seen value is newer than current.  Remove the current and keep the previously seen value
            else {
                toRemove = currentCandidate;
                toKeep = previousMax;
            }

            //this is a newer version, we know we already have a stale entity, add it to be cleaned up


            //de-index it
            logger.warn( "Stale version of Entity uuid:{} type:{}, stale v:{}, latest v:{}", new Object[] {
                    entityId.getUuid(), entityId.getType(), toRemove.getVersion(), toKeep.getVersion()
                } );

            //deindex this document, and remove the previous maxVersion
            //we have to deindex this from our ownerId, since this is what gave us the reference
            indexBatch.deindex( indexScope, toRemove );


            //TODO, fire the entity repair cleanup task here instead of de-indexing

            //replace the value with a more current version
            maxCandidateMapping.put( entityId, toKeep );
            orderIndex.put( entityId, i );
        }


        //now everything is ordered, and older versions are removed.  Batch fetch versions to verify
        // existence and correct versions

        final TreeMap<Integer, Id> sortedResults = new TreeMap<>();


        final Collection<Id> idsToLoad =
            Collections2.transform( maxCandidateMapping.values(), new Function<CandidateResult, Id>() {
                @Nullable
                @Override
                public Id apply( @Nullable final CandidateResult input ) {
                    //NOTE this is never null, we won't need to check
                    return input.getId();
                }
            } );


        //now using the scope, load the collection

        // Get the collection scope and batch load all the versions.  We put all entities in
        // app/app for easy retrieval/ unless persistence changes, we never want to read from
        // any scope other than the app, app, scope name scope
        //            final CollectionScope collScope = new CollectionScopeImpl(
        //                applicationScope.getApplication(), applicationScope.getApplication(), scopeName);

        final EntityCollectionManager ecm = managerCache.getEntityCollectionManager( applicationScope );


        //load the results into the loader for this scope for validation
        resultsVerifier.loadResults( idsToLoad, ecm );

        //now let the loader validate each candidate.  For instance, the "max" in this candidate
        //could still be a stale result, so it needs validated
        for ( final Id requestedId : idsToLoad ) {

            final CandidateResult cr = maxCandidateMapping.get( requestedId );

            //ask the loader if this is valid, if not discard it and de-index it
            if ( !resultsVerifier.isValid( cr ) ) {
                indexBatch.deindex( indexScope, cr );
                continue;
            }

            //if we get here we're good, we need to add this to our results
            final int candidateIndex = orderIndex.get( requestedId );

            sortedResults.put( candidateIndex, requestedId );
        }


        // NOTE DO NOT execute the batch here.
        // It changes the results and we need consistent paging until we aggregate all results
        return resultsVerifier.getResults( sortedResults.values() );
    }


    @Override
    public void postProcess() {
        this.indexBatch.execute();
    }
}
