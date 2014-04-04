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

package org.apache.usergrid.persistence.graph.impl.cache;


import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.model.entity.Id;



public interface NodeShardAllocation {

    public static final UUID MIN_UUID =  new UUID( 0, 1 );

    /**
     * Get all shards for the given info.  If none exist, a default shard should be allocated
     * @param scope
     * @param nodeId
     * @param edgeTypes
     * @return A list of all shards <= the current shard.  This will always return MIN_UUID if no shards are allocated
     */
    public List<UUID> getShards(final OrganizationScope scope, final Id nodeId, final String... edgeTypes);


    /**
     * Audit our highest shard for it's maximum capacity.  If it has reached the max capacity <=, it will allocate a new shard
     *
     * @param scope The organization scope
     * @param nodeId The node id
     * @param edgeType The edge types
     * @return True if a new shard was allocated
     */
    public boolean auditMaxShard(final OrganizationScope scope, final Id nodeId, final String... edgeType);

    /**
        * The minimum uuid we allocate
        */
//       private static final UUID MIN_UUID =  new UUID( 0, 1 );
//

    /**
     *
              * There are no shards allocated, allocate the minimum shard
              */
    /**
     *
     *
             if(shards == null || shards.size() == 0){

                 try {
                     edgeSeriesSerialization.writeEdgeMeta( scope, nodeId, MIN_UUID, edgeType ).execute();
                 }
                 catch ( ConnectionException e ) {
                     throw new RuntimeException("Unable to write edge meta data", e);
                 }

                 return MIN_UUID;
              }

     (**/
}
