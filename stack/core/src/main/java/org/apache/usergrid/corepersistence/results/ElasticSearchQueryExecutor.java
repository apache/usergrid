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

package org.apache.usergrid.corepersistence.results;


import java.util.Iterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.SearchTypes;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.index.query.Query;


public class ElasticSearchQueryExecutor implements QueryExecutor {

    private static final Logger logger = LoggerFactory.getLogger( ElasticSearchQueryExecutor.class );

    private final ResultsLoaderFactory resultsLoaderFactory;

    private final ApplicationScope applicationScope;

    private final ApplicationEntityIndex entityIndex;

    private final IndexScope indexScope;

    private final SearchTypes types;

    private final Query query;


    private Results currentResults;

    private boolean moreToLoad = true;


    public ElasticSearchQueryExecutor( final ResultsLoaderFactory resultsLoaderFactory, final ApplicationEntityIndex entityIndex,
                                       final ApplicationScope applicationScope, final IndexScope indexScope,
                                       final SearchTypes types, final Query query ) {
        this.resultsLoaderFactory = resultsLoaderFactory;
        this.applicationScope = applicationScope;
        this.entityIndex = entityIndex;
        this.indexScope = indexScope;
        this.types = types;

        //we must deep copy the query passed.  Otherwise we will modify it's state with cursors.  Won't fix, not relevant
        //once we start subscribing to streams.
        this.query = new Query(query);
    }


    @Override
    public Iterator<Results> iterator() {
        return this;
    }


    private void loadNextPage() {
        // Because of possible stale entities, which are filtered out by buildResults(),
        // we loop until the we've got enough results to satisfy the query limit.

        final int maxQueries = 10; // max re-queries to satisfy query limit

        final int originalLimit = query.getLimit();

        Results results = null;
        int queryCount = 0;

        boolean satisfied = false;


        while ( !satisfied && queryCount++ < maxQueries ) {

            CandidateResults crs = entityIndex.search( indexScope, types, query );

            logger.debug( "Calling build results 1" );
            results = buildResults( indexScope, query, crs );

            if ( crs.isEmpty() || !crs.hasCursor() ) { // no results, no cursor, can't get more
                satisfied = true;
            }

            /**
             * In an edge case where we delete stale entities, we could potentially get less results than expected.
             * This will only occur once during the repair phase.
             * We need to ensure that we short circuit before we overflow the requested limit during a repair.
             */
            else if ( results.size() > 0 ) { // got what we need
                satisfied = true;
            }
            //we didn't load anything, but there was a cursor, this means a read repair occured.  We have to short
            //circuit to avoid over returning the result set
            else if ( crs.hasCursor() ) {
                satisfied = false;

                // need to query for more
                // ask for just what we need to satisfy, don't want to exceed limit
                query.setCursor( results.getCursor() );
                query.setLimit( originalLimit - results.size() );

                logger.warn( "Satisfy query limit {}, new limit {} query count {}", new Object[] {
                    originalLimit, query.getLimit(), queryCount
                } );
            }
        }

        //now set our cursor if we have one for the next iteration
        if ( results.hasCursor() ) {
            query.setCursor( results.getCursor() );
            moreToLoad = true;
        }

        else {
            moreToLoad = false;
        }


        //set our current results and the flag
        this.currentResults = results;
    }


    /**
     * Build results from a set of candidates, and discard those that represent stale indexes.
     *
     * @param query Query that was executed
     * @param crs Candidates to be considered for results
     */
    private Results buildResults( final IndexScope indexScope, final Query query, final CandidateResults crs ) {

        logger.debug( "buildResults()  from {} candidates", crs.size() );

        //get an instance of our results loader
        final ResultsLoader resultsLoader =
            this.resultsLoaderFactory.getLoader( applicationScope, indexScope, query.getResultsLevel() );

        //load the results
        final Results results = resultsLoader.loadResults( crs );

        //signal for post processing
        resultsLoader.postProcess();


        results.setCursor( crs.getCursor() );

        //ugly and tight coupling, but we don't have a choice until we finish some refactoring
        results.setQueryExecutor( this );

        logger.debug( "Returning results size {}", results.size() );

        return results;
    }


    @Override
    public boolean hasNext() {

        //we've tried to load and it's empty and we have more to load, load the next page
        if ( currentResults == null ) {
            //there's nothing left to load, nothing to do
            if ( !moreToLoad ) {
                return false;
            }

            //load the page

            loadNextPage();
        }


        //see if our current results are not null
        return currentResults != null;
    }


    @Override
    public Results next() {
        if ( !hasNext() ) {
            throw new NoSuchElementException( "No more results present" );
        }

        final Results toReturn = currentResults;

        currentResults = null;

        return toReturn;
    }

    @Override
    public void remove() {
        throw new RuntimeException("Remove not implemented!!");
    }
}
