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


import java.util.Iterator;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import com.google.common.base.Optional;


/**
 * Interface used to create and retrieve shards
 */
public interface NodeShardAllocation {



    /**
     * Get all shards for the given info.  If none exist, a default shard should be allocated.  The nodeId is the source node
     *
     * @param scope The application scope
     * @param maxShardId The max value to start seeking from.  Values <= this will be returned if specified
     * @param directedEdgeMeta The directed edge metadata to use
     * @return A list of all shards <= the current shard.  This will always return 0l if no shards are allocated
     */
    public Iterator<ShardEntryGroup> getShards( final ApplicationScope scope, Optional<Shard> maxShardId, final DirectedEdgeMeta directedEdgeMeta );


    /**
     * Audit our highest shard for it's maximum capacity.  If it has reached the max capacity <=, it will allocate a new shard
     *
     * @param scope The app scope
     * @param shardEntryGroup The shard operator to use
     * @param directedEdgeMeta The directed edge metadata to use
     * @return True if a new shard was allocated
     */
    public boolean auditShard(final ApplicationScope scope, final ShardEntryGroup shardEntryGroup, final DirectedEdgeMeta directedEdgeMeta);

    /**
     * Get the minimum time that a created shard should be considered "new", and be used for both new writes and reads
     * @return
     */
    public long getMinTime();


}
