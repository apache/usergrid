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

package org.apache.usergrid.corepersistence.pipeline.read.elasticsearch.impl;


import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.usergrid.persistence.index.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import com.google.common.base.Optional;


public class ElasticSearchQueryExecutor implements Iterable<Results>, Iterator<Results> {

    private static final Logger logger = LoggerFactory.getLogger( ElasticSearchQueryExecutor.class );

    private final ResultsLoaderFactory resultsLoaderFactory;

    private final ApplicationScope applicationScope;

    private final ApplicationEntityIndex entityIndex;

    private final SearchEdge indexScope;

    private final SearchTypes types;

    private final String query;

    private final Optional<Integer> setOffsetFromCursor;

    private final int limit;

    private int offset;


    private Results currentResults;

    private boolean moreToLoad = true;




    public ElasticSearchQueryExecutor( final ResultsLoaderFactory resultsLoaderFactory, final ApplicationEntityIndex entityIndex,
                                       final ApplicationScope applicationScope, final SearchEdge indexScope,
                                       final SearchTypes types, final String query, final Optional<Integer> setOffsetFromCursor, final int limit ) {
        this.resultsLoaderFactory = resultsLoaderFactory;
        this.applicationScope = applicationScope;
        this.entityIndex = entityIndex;
        this.indexScope = indexScope;
        this.types = types;
        this.setOffsetFromCursor = setOffsetFromCursor;

        //we must deep copy the query passed.  Otherwise we will modify it's state with cursors.  Won't fix, not relevant
        //once we start subscribing to streams.
        this.query = query;
        this.limit = limit;
    }


    @Override
    public Iterator<Results> iterator() {
        return this;
    }


    private void loadNextPage() {
        // Because of possible stale entities, which are filtered out by buildResults(),
        // we loop until the we've got enough results to satisfy the query limit.

        final int maxQueries = 10; // max re-queries to satisfy query limit


        Results results = null;
        int queryCount = 0;


        CandidateResults crs = null;

        int newLimit = limit;

        while ( queryCount++ < maxQueries ) {

            crs = entityIndex.search( indexScope, types, query, newLimit , offset);


            logger.debug( "Calling build results with crs {}", crs );
            results = buildResults( indexScope, crs );

            /**
             * In an edge case where we delete stale entities, we could potentially get less results than expected.
             * This will only occur once during the repair phase.
             * We need to ensure that we short circuit before we overflow the requested limit during a repair.
             */
            if ( crs.isEmpty() || !crs.hasOffset() || results.size() > 0 ) { // no results, no cursor, can't get more
                break;
            }


            //we didn't load anything, but there was a cursor, this means a read repair occured.  We have to short
            //circuit to avoid over returning the result set


            // need to query for more
            // ask for just what we need to satisfy, don't want to exceed limit
            newLimit =  newLimit - results.size();

            logger.warn( "Satisfy query limit {}, new limit {} query count {}", new Object[] {
                limit, newLimit, queryCount
            } );
        }

        //now set our cursor if we have one for the next iteration
        if ( results.hasCursor() ) {
            moreToLoad = true;
        }

        else {
            moreToLoad = false;
        }

//
//        //set our select subjects into our query if provided
//        if(crs != null){
//            query.setSelectSubjects( crs.getGetFieldMappings() );
//        }
//

        //set our current results and the flag
        this.currentResults = results;
    }



    /**
     * Build results from a set of candidates, and discard those that represent stale indexes.
     *
     * @param indexScope The index scope to execute the search on
     * @param crs Candidates to be considered for results
     */
    private Results buildResults( final SearchEdge indexScope, final CandidateResults crs ) {

        logger.debug( "buildResults()  from {} candidates", crs.size() );

        //get an instance of our results loader
        final ResultsLoader resultsLoader =
            this.resultsLoaderFactory.getLoader( applicationScope, indexScope, Query.Level.ALL_PROPERTIES );

        //load the results
        final Results results = resultsLoader.loadResults(crs);

        //signal for post processing
        resultsLoader.postProcess();

        //set offset into query

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
