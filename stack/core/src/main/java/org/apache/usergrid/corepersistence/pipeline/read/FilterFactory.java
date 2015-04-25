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


import org.apache.usergrid.corepersistence.pipeline.read.elasticsearch.QueryCollectionElasticSearchCollectorFilter;
import org.apache.usergrid.corepersistence.pipeline.read.elasticsearch.QueryConnectionElasticSearchCollectorFilter;
import org.apache.usergrid.corepersistence.pipeline.read.entity.EntityIdFilter;
import org.apache.usergrid.corepersistence.pipeline.read.entity.EntityLoadCollectorFilter;
import org.apache.usergrid.corepersistence.pipeline.read.graph.ReadGraphCollectionByIdFilter;
import org.apache.usergrid.corepersistence.pipeline.read.graph.ReadGraphCollectionFilter;
import org.apache.usergrid.corepersistence.pipeline.read.graph.ReadGraphConnectionByIdFilter;
import org.apache.usergrid.corepersistence.pipeline.read.graph.ReadGraphConnectionByTypeFilter;
import org.apache.usergrid.corepersistence.pipeline.read.graph.ReadGraphConnectionFilter;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;


/**
 * A factory for generating read commands
 */
public interface FilterFactory {


    /**
     * Generate a new instance of the command with the specified parameters
     */
    ReadGraphCollectionFilter readGraphCollectionCommand( final String collectionName );

    /**
     * Read a connection between two entities, the incoming and the target entity
     * @param collectionName
     * @param targetId
     * @return
     */
    ReadGraphCollectionByIdFilter readGraphCollectionByIdFilter(final String collectionName, final Id targetId);

    /**
     * Generate a new instance of the command with the specified parameters
     */
    ReadGraphConnectionFilter readGraphConnectionCommand( final String connectionName );

    /**
     * Generate a new instance of the command with the specified parameters
     */
    ReadGraphConnectionByTypeFilter readGraphConnectionCommand( final String connectionName, final String entityType );


    /**
     * Read a connection directly between two identifiers
     * @param connectionName
     * @param targetId
     * @return
     */
    ReadGraphConnectionByIdFilter readGraphConnectionByIdFilter(final String connectionName, final Id targetId);

    /**
     * Generate a new instance of the command with the specified parameters
     */
    EntityLoadCollectorFilter entityLoadCollector();

    /**
     * Generate a new instance of the command with the specified parameters
     */
    QueryCollectionElasticSearchCollectorFilter queryCollectionElasticSearchCollector( final String collectionName, final String query);


    /**
     * Generate a new instance of the command with the specified parameters
     */
    QueryConnectionElasticSearchCollectorFilter queryConnectionElasticSearchCollector( final String connectionName,final String query);


    /**
     * Generate a new instance of the command with the specified parameters
     */
    QueryConnectionElasticSearchCollectorFilter queryConnectionElasticSearchCollector( final String connectionName, final String connectionEntityType, final String query );


    /**
     * Get an entity id filter.  Used as a 1.0->2.0 bridge since we're not doing full traversals
     */
    EntityIdFilter getEntityIdFilter( final Id entityId );
}
