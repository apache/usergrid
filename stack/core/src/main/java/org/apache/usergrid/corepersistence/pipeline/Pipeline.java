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


import org.apache.usergrid.corepersistence.pipeline.cursor.RequestCursor;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import rx.Observable;
import rx.schedulers.Schedulers;


/**
 * Pipeline for applying our UG domain specific filters.
 *
 * Modeled after an observable, with typing to allow input of specific filters
 *
 * @param InputType the input type in the current pipeline state
 */
public class Pipeline<InputType> {


    private int idCount = 0;

    private final ApplicationScope applicationScope;


    private final RequestCursor requestCursor;
    private int limit;

    //Generics hell, intentionally without a generic, we check at the filter level
    private Observable currentObservable;


    /**
     * Create our filter pipeline
     */
    public Pipeline( final ApplicationScope applicationScope, final Optional<String> cursor, final int limit ) {


        ValidationUtils.validateApplicationScope( applicationScope );
        Preconditions.checkNotNull( cursor, "cursor optional is required" );
        Preconditions.checkArgument( limit > 0, "limit must be > 0" );


        this.applicationScope = applicationScope;

        //init our cursor to empty
        this.requestCursor = new RequestCursor( cursor );

        //set the default limit
        this.limit = limit;

        //set our observable to start at the application
        final FilterResult<Id> filter = new FilterResult<>( applicationScope.getApplication(), Optional.absent() );

        this.currentObservable = Observable.just( filter );
    }


    public <OutputType> Pipeline<OutputType> withFilter(
        final PipelineOperation<? super InputType, ? extends OutputType> filter ) {



        final PipelineContext context = new PipelineContext( applicationScope, requestCursor, limit, idCount );

        filter.setContext( context );

        //update the observable
        this.currentObservable = currentObservable.compose( filter );

        //done for clarity
        idCount++;

        return ( Pipeline<OutputType> ) this;
    }



    /**
     * Return the observable of the filter pipeline
     */
    public Observable<InputType> execute() {
        return currentObservable;
    }


}
