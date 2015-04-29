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

package org.apache.usergrid.corepersistence.pipeline.read;


import org.apache.usergrid.corepersistence.pipeline.PipelineResult;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;

import rx.Observable;


/**
 * An instance of a pipeline builder for building commands on our read pipline
 *
 * Each invocation of the method will assemble the underlying pipe and updating it's state
 *
 * Results are added by invoking execute.
 */
public interface ReadPipelineBuilder {


    /**
     * Set the cursor
     * @param cursor
     */
    ReadPipelineBuilder withCursor(final Optional<String> cursor);

    /**
     * Set the limit of our page sizes
     * @param limit
     * @return
     */
    ReadPipelineBuilder withLimit(final Optional<Integer> limit);

    /**
     * An operation to bridge 2.0-> 1.0.  Should be removed when everyone uses the pipeline
     * @param id
     * @return
     */
    ReadPipelineBuilder setStartId(final Id id);


    /**
     * Add a get entity to the pipeline
     */
    ReadPipelineBuilder getEntityViaCollection( final String collectionName, final Id entityId );


    /**
     * Add get Collection from our previous source
     */
    ReadPipelineBuilder getCollection( final String collectionName );

    /**
     * Get all entities with a query
     */
    ReadPipelineBuilder getCollectionWithQuery( final String collectionName,final String entityType,  final String query);

    /**
     * Get an entity via the connection name and entity Id
     */
    ReadPipelineBuilder getEntityViaConnection( final String connectionName, final Id entityId );

    /**
     * Get all entities in a connection by the connection name
     */
    ReadPipelineBuilder getConnection( final String connectionName );

    /**
     * Get all entities in a connection of the specified connection type
     */
    ReadPipelineBuilder getConnection( final String connectionName, final String entityType );

    /**
     * Get all entities in a connection with a query and a target entity type
     */
    ReadPipelineBuilder getConnectionWithQuery( final String connectionName, final Optional<String> entityType,
                                                final String query );


    /**
     * Load our entity results when our previous filter calls graph
     * @return
     */
    Observable<PipelineResult<ResultsPage>> execute();
}
