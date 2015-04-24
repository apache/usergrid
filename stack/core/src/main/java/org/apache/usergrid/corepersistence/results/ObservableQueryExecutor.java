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

import org.apache.usergrid.persistence.Results;

import rx.Observable;


/**
 * Our proxy to allow us to subscribe to observable results, then return them as an interator.  A bridge for 2.0 -> 1.0
 * code
 */
public class ObservableQueryExecutor implements QueryExecutor {

    private final Observable<Results> resultsObservable;

    public Iterator<Results> iterator;


    public ObservableQueryExecutor( final Observable<Results> resultsObservable ) {
        this.resultsObservable = resultsObservable;
    }


    @Override
    public Iterator<Results> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {

        if ( iterator == null ) {
            iterator = resultsObservable.toBlocking().getIterator();
        }


        return iterator.hasNext();
    }


    @Override
    public Results next() {
        if ( !hasNext() ) {
            throw new NoSuchElementException( "No more results present" );
        }
        final Results next = iterator.next();

        next.setQueryExecutor( this );

        return next;
    }
}
