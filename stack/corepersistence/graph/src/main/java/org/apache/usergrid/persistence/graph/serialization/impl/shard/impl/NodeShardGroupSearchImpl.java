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
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.exception.GraphRuntimeException;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardAllocation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardGroupSearch;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.graph.serialization.util.GraphValidation;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;


/**
 * Simple implementation of the shard.  Uses a local Guava shard with a timeout.  If a value is not present in the
 * shard, it will need to be searched via cassandra.
 */
public class NodeShardGroupSearchImpl implements NodeShardGroupSearch {

    private final NodeShardAllocation nodeShardAllocation;


    /**
     *  @param nodeShardAllocation
     */
    @Inject
    public NodeShardGroupSearchImpl( final NodeShardAllocation nodeShardAllocation ) {
        Preconditions.checkNotNull( nodeShardAllocation, "nodeShardAllocation is required" );

        this.nodeShardAllocation = nodeShardAllocation;


    }


    @Override
    public ShardEntryGroup getWriteShardGroup( final ApplicationScope scope, final long timestamp,
                                               final DirectedEdgeMeta directedEdgeMeta ) {

        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateDirectedEdgeMeta( directedEdgeMeta );

        final Iterator<ShardEntryGroup> shardEntryGroupIterator = getShards( scope, directedEdgeMeta, timestamp );

        if(shardEntryGroupIterator.hasNext()){
            return shardEntryGroupIterator.next();
        }


        //if we get here, something went wrong, our shard should always have a time UUID to return to us
        throw new GraphRuntimeException( "No time UUID shard was found and could not allocate one" );
    }


    @Override
    public Iterator<ShardEntryGroup> getReadShardGroup( final ApplicationScope scope, final long maxTimestamp,
                                                        final DirectedEdgeMeta directedEdgeMeta ) {
        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateDirectedEdgeMeta( directedEdgeMeta );

        final Iterator<ShardEntryGroup> shardEntryGroupIterator = getShards( scope, directedEdgeMeta, maxTimestamp );

        return shardEntryGroupIterator;
    }



    private Iterator<ShardEntryGroup> getShards( final ApplicationScope scope, final DirectedEdgeMeta directedEdgeMeta,
                                                 final long maxTimestamp ) {

        final Shard seekShard = new Shard( maxTimestamp, System.currentTimeMillis(), false );

        final Iterator<ShardEntryGroup> edges =
            nodeShardAllocation.getShardsLocal(scope, Optional.of( seekShard ), directedEdgeMeta );


        return edges;
    }



}
