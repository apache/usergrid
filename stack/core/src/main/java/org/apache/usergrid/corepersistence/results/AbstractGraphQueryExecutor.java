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


import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.usergrid.corepersistence.command.CommandBuilder;
import org.apache.usergrid.corepersistence.command.read.entity.EntityLoadCommand;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.common.base.Optional;

import rx.Observable;


/**
 * This class is a nasty hack to bridge 2.0 observables into 1.0 iterators DO NOT use this as a model for moving
 * forward, pandas will die.
 */
public abstract class AbstractGraphQueryExecutor implements QueryExecutor {


    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final ApplicationScope applicationScope;
    private final Id sourceId;
    private final int limit;

    private final Optional<String> requestCursor;

    private Iterator<Results> observableIterator;


    public AbstractGraphQueryExecutor( final EntityCollectionManagerFactory entityCollectionManagerFactory,
                                       final ApplicationScope applicationScope, final EntityRef source,
                                       final String cursor, final int limit ) {

        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.applicationScope = applicationScope;
        this.limit = limit;
        this.sourceId = new SimpleId( source.getUuid(), source.getType() );

        this.requestCursor = Optional.fromNullable( cursor );
    }


    @Override
    public Iterator<Results> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {

        //hasn't been set up yet, run through our first setup
        if ( observableIterator == null ) {
            //assign them to an iterator.  this now uses an internal buffer with backpressure, so we won't load all
            // results
            //set up our command builder
            final CommandBuilder commandBuilder = new CommandBuilder( applicationScope, sourceId, requestCursor, limit );


            addTraverseCommand( commandBuilder );

            //construct our results to be observed later. This is a cold observable
            final Observable<Results> resultsObservable =
                commandBuilder.build( new EntityLoadCommand( entityCollectionManagerFactory, applicationScope ) );

            this.observableIterator = resultsObservable.toBlocking().getIterator();

            if(!observableIterator.hasNext()){
                //no results, generate an empty one
                this.observableIterator = Collections.singleton(new Results()).iterator();
            }
        }


        //see if our current results are not null
        return observableIterator.hasNext();
    }


    @Override
    public Results next() {
        if ( !hasNext() ) {
            throw new NoSuchElementException( "No more results present" );
        }

        final Results results = observableIterator.next();

        //ugly and tight coupling, but we don't have a choice until we finish some refactoring
        results.setQueryExecutor( this );

        return results;
    }


    @Override
    public void remove() {
        throw new RuntimeException( "Remove not implemented!!" );
    }


    /**
     * Add the traverse command to the graph
     */
    protected abstract void addTraverseCommand( final CommandBuilder commandBuilder );
}
