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

package org.apache.usergrid.corepersistence.command.read;


import org.apache.usergrid.corepersistence.command.read.elasticsearch.QueryCollectionElasticSearchCollector;
import org.apache.usergrid.corepersistence.command.read.elasticsearch.QueryConnectionElasticSearchCollector;
import org.apache.usergrid.corepersistence.command.read.entity.EntityLoadCollector;
import org.apache.usergrid.corepersistence.command.read.graph.ReadGraphCollectionCommand;
import org.apache.usergrid.corepersistence.command.read.graph.ReadGraphConnectionCommand;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * A factory for generating read commands
 */
public interface ReadCommandFactory {


    /**
     * Generate a new instance of the command with the specified parameters
     * @param applicationScope
     * @param sourceId
     * @param collectionName
     * @return
     */
    ReadGraphCollectionCommand readGraphCollectionCommand(final ApplicationScope applicationScope, final Id sourceId, final String collectionName);

    /**
     * Generate a new instance of the command with the specified parameters
     * @param applicationScope
     * @param sourceId
     * @param connectionName
     * @return
     */
    ReadGraphConnectionCommand readGraphConnectionCommand(final ApplicationScope applicationScope, final Id sourceId, final String connectionName);

    /**
     * Generate a new instance of the command with the specified parameters
     * @param applicationScope
     * @return
     */
    EntityLoadCollector entityLoadCollector(final ApplicationScope applicationScope);

    /**
     * Generate a new instance of the command with the specified parameters
     * @param applicationScope
     * @param sourceId
     * @param collectionName
     * @return
     */
    QueryCollectionElasticSearchCollector queryCollectionElasticSearchCollector(final ApplicationScope applicationScope, final Id sourceId, final String collectionName);


    /**
     * Generate a new instance of the command with the specified parameters
     * @param applicationScope
     * @param sourceId
     * @param connectionName
     * @return
     */
    QueryConnectionElasticSearchCollector queryConnectionElasticSearchCollector(final ApplicationScope applicationScope, final Id sourceId, final String connectionName);
}
