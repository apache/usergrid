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

package org.apache.usergrid.corepersistence.pipeline;


import java.util.ArrayList;
import java.util.List;

import org.apache.usergrid.corepersistence.pipeline.cursor.RequestCursor;
import org.apache.usergrid.corepersistence.pipeline.cursor.ResponseCursor;
import org.apache.usergrid.corepersistence.pipeline.read.CollectorFilter;
import org.apache.usergrid.corepersistence.pipeline.read.Filter;
import org.apache.usergrid.corepersistence.pipeline.read.TraverseFilter;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;

import rx.Observable;


/**
 * A pipeline that will allow us to build a traversal command for execution
 *
 * See http://martinfowler.com/articles/collection-pipeline/ for some examples
 *
 * TODO: Re work the cursor and limit phases.  They need to be lazily evaluated, not added on build time
 */
public class DataPipeline {


    private final ApplicationScope applicationScope;
    private final List<TraverseFilter> traverseFilterList;

    private Optional<String> cursor;
    private int limit;


    private int count = 0;


    /**
     * Our first pass, where we implement our start point as an Id until we can use this to perform our entire
     * traversal.  Eventually as we untangle the existing Query service nightmare, the sourceId will be remove and
     * should only be traversed from the root application
     */
    public DataPipeline( final ApplicationScope applicationScope ) {

        this.applicationScope = applicationScope;


        traverseFilterList = new ArrayList<>();
    }


    /**
     * Add a read command that will read Ids and produce Ids.  This is an intermediate traversal operations
     */
    public DataPipeline withTraverseCommand( final TraverseFilter traverseCommand ) {

        this.traverseFilterList.add( traverseCommand );

        return this;
    }


    /**
     * Build the final collection step, and process our filters
     */
    public <T> Observable<T> build( final CollectorFilter<T> pipeCollector ) {

        RequestCursor requestCursor = new RequestCursor( this.cursor );
        ResponseCursor responseCursor = new ResponseCursor();

        Observable<Id> traverseObservable = Observable.just( applicationScope.getApplication() );

        //build our traversal commands
        for ( TraverseFilter filter : traverseFilterList ) {
            setState( filter, requestCursor, responseCursor );

            traverseObservable = traverseObservable.compose( filter );
        }


        setState( pipeCollector, requestCursor, responseCursor );

        pipeCollector.setLimit( limit );

        return traverseObservable.compose( pipeCollector );
    }


    public void setCursor( Optional<String> cursor ) {
        this.cursor = cursor;
    }


    public void setLimit( final int limit ) {
        this.limit = limit;
    }


    /**
     * Set the id of the state
     */
    private void setState( final Filter<?> filter, final RequestCursor requestCursor,
                           final ResponseCursor responseCursor ) {

        //TODO, see if we can wrap this observable in our ObservableTimer so we can see how long each filter takes


        filter.setId( count );
        //done for clarity
        count++;

        filter.setCursorCaches( requestCursor, responseCursor );
        filter.setApplicationScope( applicationScope );
    }
}
