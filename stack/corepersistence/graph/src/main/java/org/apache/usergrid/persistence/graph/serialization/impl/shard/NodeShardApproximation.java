/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.graph.serialization.impl.shard;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Interface for creating approximate estimates of shards
 */
public interface NodeShardApproximation {


    /**
     * Increment the shard Id the specified amount
     *
     * @param scope The scope
     * @param nodeId The node id
     * @param shardId The shard id
     * @param count
     * @param edgeType The edge type
     */
    public void increment( final OrganizationScope scope, final Id nodeId, final UUID shardId,  final long count,
                           final String... edgeType );


    /**
     * Get the approximation of the number of unique items
     */
    public long getCount( final OrganizationScope scope, final Id nodeId, final UUID shardId,
                          final String... edgeType );
}
