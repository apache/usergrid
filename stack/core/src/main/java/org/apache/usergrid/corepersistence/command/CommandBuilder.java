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

package org.apache.usergrid.corepersistence.command;


import org.apache.usergrid.corepersistence.command.cursor.RequestCursor;
import org.apache.usergrid.corepersistence.command.cursor.ResponseCursor;
import org.apache.usergrid.corepersistence.command.read.Collector;
import org.apache.usergrid.corepersistence.command.read.Command;
import org.apache.usergrid.corepersistence.command.read.TraverseCommand;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;

import rx.Observable;


/**
 * A builder that will allow us to build a traversal command for execution
 */
public class CommandBuilder {


    private final ApplicationScope applicationScope;
    private final RequestCursor requestCursor;
    private final ResponseCursor responseCursor;
    private final int limit;

    private int count = 0;

    private Observable<Id> currentObservable;


       /**
     * Our first pass, where we implement our start point as an Id until we can use this to perform our entire
     * traversal.  Eventually as we untangle the existing Query service nightmare, the sourceId will be remove and should
     * only be traversed from the root application
     */
    public CommandBuilder( final ApplicationScope applicationScope, final Id sourceId,
                           final Optional<String> requestCursor, final int limit ) {

        this.applicationScope = applicationScope;
        this.limit = limit;

        //set the request cursor
        this.requestCursor = new RequestCursor( requestCursor );

        //set the response cursor
        this.responseCursor = new ResponseCursor();


        this.currentObservable = Observable.just( sourceId );
    }


    /**
     * Add a read command that will read Ids and produce Ids.  This is an intermediate traversal operations
     */
    public CommandBuilder withTraverseCommand( final TraverseCommand traverseCommand ) {

        setState( traverseCommand );

        this.currentObservable = currentObservable.compose( traverseCommand );

        return this;
    }


    /**
     * Build the final collection step, and
     */
    public <T> Observable<T> build( final Collector<T> collector ) {
        setState( collector );

        collector.setLimit( limit );

        return currentObservable.compose( collector );
    }


    /**
     * Set the id of the state
     * @param command
     */
    private void setState( Command<?> command ) {
        command.setId( count );
        //done for clarity
        count++;

        command.setCursorCaches( requestCursor, responseCursor );
        command.setApplicationScope( applicationScope );
    }
}
