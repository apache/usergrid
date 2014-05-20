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
import java.util.UUID;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardStrategy;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardCache;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Simple sized based shard strategy. For now always returns the same shard.
 */
@Singleton
public class SizebasedEdgeShardStrategy implements EdgeShardStrategy {


    private final NodeShardCache shardCache;


    @Inject
    public SizebasedEdgeShardStrategy( final NodeShardCache shardCache ) {this.shardCache = shardCache;}



    @Override
    public long getWriteShard( final ApplicationScope scope, final Id rowKeyId, final UUID version,
                               final String... types ) {
        return shardCache.getSlice( scope, rowKeyId, version, types );
    }


    @Override
    public Iterator<Long> getReadShards( final ApplicationScope scope, final Id rowKeyId, final UUID maxVersion,
                                         final String... types ) {
        return shardCache.getVersions( scope, rowKeyId, maxVersion, types );
    }


    @Override
    public void increment( final ApplicationScope scope, final Id rowKeyId,
                           final long shardId, final long count, final String... types ) {
        shardCache.increment(  scope, rowKeyId, shardId, count, types );
    }


    @Override
    public String getSourceNodeCfName() {
        return "Graph_Source_Node_Edges";
    }


    @Override
    public String getTargetNodeCfName() {
        return "Graph_Target_Node_Edges";
    }


    @Override
    public String getSourceNodeTargetTypeCfName() {
        return "Graph_Source_Node_Target_Type";
    }


    @Override
    public String getTargetNodeSourceTypeCfName() {
        return "Graph_Target_Node_Source_Type";
    }


    @Override
    public String getGraphEdgeVersions() {
        return "Graph_Edge_Versions";
    }
}
