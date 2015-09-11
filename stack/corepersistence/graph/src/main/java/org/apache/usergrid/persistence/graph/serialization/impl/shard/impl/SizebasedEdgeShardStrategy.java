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

package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import java.util.Iterator;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardStrategy;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardGroupSearch;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Simple sized based shard strategy. For now always returns the same shard.
 */
@Singleton
public class SizebasedEdgeShardStrategy implements EdgeShardStrategy {


    private final NodeShardGroupSearch shardGroup;


    @Inject
    public SizebasedEdgeShardStrategy( final NodeShardGroupSearch shardGroup ) {
        this.shardGroup = shardGroup;
    }


    @Override
    public ShardEntryGroup getWriteShards( final ApplicationScope scope,
                                        final long timestamp, final DirectedEdgeMeta directedEdgeMeta ) {
        return shardGroup.getWriteShardGroup( scope, timestamp, directedEdgeMeta);
    }


    @Override
    public Iterator<ShardEntryGroup> getReadShards( final ApplicationScope scope, final long maxTimestamp, final DirectedEdgeMeta directedEdgeMeta ) {
        return shardGroup.getReadShardGroup( scope, maxTimestamp, directedEdgeMeta );
    }

}
