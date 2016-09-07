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

import com.google.common.base.Optional;
import com.google.inject.assistedinject.Assisted;
import org.apache.usergrid.corepersistence.pipeline.read.search.SearchCollectionFilter;
import org.apache.usergrid.corepersistence.pipeline.read.search.SearchConnectionFilter;

/**
 * Created by russo on 9/2/16.
 */
public interface SearchFilterFactory {

    /**
     * Generate a new instance of the command with the specified parameters
     *
     * @param query The query to use when querying the entities in the collection
     * @param collectionName The collection name to use when querying
     */
    SearchCollectionFilter searchCollectionFilter(@Assisted( "query" ) final String query,
                                                  @Assisted( "collectionName" ) final String collectionName,
                                                  @Assisted( "entityType" ) final String entityType );


    /**
     * Generate a new instance of the command with the specified parameters
     *
     * @param query The query to use when querying the entities in the connection
     * @param connectionName The type of connection to query
     * @param connectedEntityType The type of entity in the connection.  Leave absent to query all entity types
     */
    SearchConnectionFilter searchConnectionFilter(@Assisted( "query" ) final String query,
                                                  @Assisted( "connectionName" ) final String connectionName,
                                                  @Assisted( "connectedEntityType" )
                                                  final Optional<String> connectedEntityType );
}
