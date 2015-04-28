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


import java.util.List;

import org.apache.usergrid.corepersistence.pipeline.cursor.RequestCursor;
import org.apache.usergrid.corepersistence.pipeline.cursor.ResponseCursor;
import org.apache.usergrid.corepersistence.pipeline.read.Collector;
import org.apache.usergrid.corepersistence.pipeline.read.PipelineOperation;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import com.google.common.base.Optional;

import rx.Observable;


/**
 * A pipeline that will allow us to build a traversal command for execution
 *
 * See http://martinfowler.com/articles/collection-pipeline/ for some examples
 *
 * TODO: Re work the cursor and limit phases.  They need to be lazily evaluated, not added on build time
 */
public class Pipeline<R> {


    private final ApplicationScope applicationScope;
    private final List<PipelineOperation> idPipelineOperationList;
    private final Collector<?, R> collector;
    private final RequestCursor requestCursor;
    private final ResponseCursor responseCursor;

    private final int limit;


    private int idCount = 0;


    /**
     * Our first pass, where we implement our start point as an Id until we can use this to perform our entire
     * traversal.  Eventually as we untangle the existing Query service nightmare, the sourceId will be remove and
     * should only be traversed from the root application
     */
    public Pipeline( final ApplicationScope applicationScope, final List<PipelineOperation> pipelineOperations,
                     final Collector<?, R> collector, final Optional<String> cursor, final int limit ) {

        this.applicationScope = applicationScope;
        this.idPipelineOperationList = pipelineOperations;
        this.collector = collector;
        this.limit = limit;

        this.requestCursor = new RequestCursor( cursor );
        this.responseCursor = new ResponseCursor();
    }


    /**
     * Execute the pipline construction, returning an observable of results
     * @return
     */
    public Observable<PipelineResult<R>> execute(){


        Observable traverseObservable = Observable.just( applicationScope.getApplication() );

        //build our traversal commands
        for ( PipelineOperation pipelineOperation : idPipelineOperationList ) {
            setState( pipelineOperation );

            //TODO, see if we can wrap this observable in our ObservableTimer so we can see how long each filter takes


            traverseObservable = traverseObservable.compose( pipelineOperation );
        }


        setState( collector );

        final Observable<R> response =  traverseObservable.compose( collector );


        //append the optional cursor into the response for the caller to use
        return response.map( result -> new PipelineResult<>( result, responseCursor ) );
    }




    /**
     * Set the id of the state
     */
    private void setState( final PipelineOperation pipelineOperation ) {


        final PipelineContext context = new PipelineContext( applicationScope, requestCursor, responseCursor,
            limit, idCount );

        pipelineOperation.setContext( context );

        //done for clarity
        idCount++;

    }
}
