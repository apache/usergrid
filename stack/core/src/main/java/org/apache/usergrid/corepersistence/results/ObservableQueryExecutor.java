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

import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;
import org.apache.usergrid.persistence.Results;

import com.google.common.base.Optional;

import rx.Observable;


/**
 * Our proxy to allow us to subscribe to observable results, then return them as an iterator.  A bridge for 2.0 -> 1.0
 * code.  This should not be used on any new code, and will eventually be deleted
 */
@Deprecated//Required for 1.0 compatibility
public abstract class ObservableQueryExecutor<T> implements QueryExecutor {



    private Results results;
    private Optional<String> cursor;
    private boolean complete;

    protected ObservableQueryExecutor(final Optional<String> startCursor){
        this.cursor = startCursor;
    }




    /**
     * Transform the results
     * @param resultsPage
     * @return
     */
    protected abstract Results createResults( final ResultsPage<T> resultsPage );


    /**
     * Build new results page from the cursor
     * @param cursor
     * @return
     */
    protected abstract Observable<ResultsPage<T>> buildNewResultsPage(final Optional<String>  cursor);



    /**
     * Legacy to transform our results page to a new results
     * @param resultsPage
     * @return
     */
    private Results createResultsInternal( final ResultsPage<T> resultsPage ) {


        final Results results = createResults( resultsPage );

        //add the cursor if our limit is the same
        if ( resultsPage.hasMoreResults() ) {
            final Optional<String> cursor = resultsPage.getResponseCursor().encodeAsString();

            if ( cursor.isPresent() ) {
                results.setCursor( cursor.get() );
            }
        }
        return results;
    }





    @Override
    public Iterator<Results> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {

        if ( !complete && results == null) {
            advance();
        }

        return results != null;
    }


    @Override
    public Results next() {
        if ( !hasNext() ) {
            throw new NoSuchElementException( "No more results present" );
        }

        final Results next = results;

        results = null;

        next.setQueryExecutor( this );

        return next;
    }

    private void advance(){
         //map to our old results objects, return a default empty if required
        final Observable<Results>
            observable = buildNewResultsPage( cursor ).map( resultsPage -> createResultsInternal( resultsPage ) ).defaultIfEmpty(
            new Results() );

        //take the first from our observable
        final Results resultsPage = observable.take(1).toBlocking().first();

        //set the results for the iterator
        this.results = resultsPage;

        //set the complete flag
        this.complete =  !resultsPage.hasCursor();

        //if not comlete, set our cursor for the next iteration
        if(!complete){
            this.cursor = Optional.of( results.getCursor());
        }else{
            this.cursor = Optional.absent();
        }
    }


}
