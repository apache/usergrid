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

package org.apache.usergrid.corepersistence.pipeline.builder;


import org.apache.usergrid.corepersistence.pipeline.read.FilterFactory;
import org.apache.usergrid.corepersistence.pipeline.Pipeline;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;


/**
 * This is our root builder to build filter pipelines.  All operations should start with an instance of this class, and compose
 * graph operations by traversing various builders to create our filter pipeline
 */
public class PipelineBuilder {



    private final ApplicationScope applicationScope;
    private Optional<String> cursor = Optional.absent();
    private int limit = 10;
    private final FilterFactory filterFactory;


    /**
     * Create an instance of our I/O operations
     * @param filterFactory
     */
    @Inject
    public PipelineBuilder( final FilterFactory filterFactory, @Assisted final ApplicationScope applicationScope ) {
        this.filterFactory = filterFactory;
        this.applicationScope = applicationScope;
    }




    /**
     * Set the cursor to use in our filter pipline
     * @param cursor
     * @return
     */
    public PipelineBuilder withCursor(final Optional<String> cursor){
        Preconditions.checkNotNull(cursor, "cursor must not be null");
        this.cursor = cursor;
        return this;
    }


    /**
     * Set our limit
     * @param limit
     * @return
     */
    public PipelineBuilder withLimit(final int limit){
        this.limit = limit;
        return this;
    }


    /**
     * Set our start point in our graph traversal to the specified entity id. A 1.0 compatibility API.  eventually this should be replaced with
     * a call that will allow us to start traversing at the application node to any other node in the graph
     *
     * @param entityId
     * @return
     */
    @Deprecated
    public IdBuilder fromId(final Id entityId){
        Pipeline<FilterResult<Id>> pipeline =  new Pipeline( applicationScope, this.cursor,limit ).withFilter(  filterFactory.getEntityIdFilter( entityId ) );

        return new IdBuilder( pipeline, filterFactory );
    }


}
