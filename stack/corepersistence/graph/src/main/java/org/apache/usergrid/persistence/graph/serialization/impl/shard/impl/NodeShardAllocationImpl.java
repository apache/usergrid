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


import java.util.Collections;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.hystrix.HystrixCassandra;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.exception.GraphRuntimeException;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeColumnFamilies;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardAllocation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardApproximation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardedEdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.util.GraphValidation;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.uuid.impl.UUIDUtil;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/**
 * Implementation of the node shard monitor and allocation
 */
public class NodeShardAllocationImpl implements NodeShardAllocation {


    private static final Logger LOG = LoggerFactory.getLogger( NodeShardAllocationImpl.class );

    private static final Shard MIN_SHARD = new Shard( 0, 0, true );

    private final EdgeShardSerialization edgeShardSerialization;
    private final EdgeColumnFamilies edgeColumnFamilies;
    private final ShardedEdgeSerialization shardedEdgeSerialization;
    private final NodeShardApproximation nodeShardApproximation;
    private final TimeService timeService;
    private final GraphFig graphFig;


    @Inject
    public NodeShardAllocationImpl( final EdgeShardSerialization edgeShardSerialization,
                                    final EdgeColumnFamilies edgeColumnFamilies,
                                    final ShardedEdgeSerialization shardedEdgeSerialization,
                                    final NodeShardApproximation nodeShardApproximation, final TimeService timeService,
                                    final GraphFig graphFig ) {
        this.edgeShardSerialization = edgeShardSerialization;
        this.edgeColumnFamilies = edgeColumnFamilies;
        this.shardedEdgeSerialization = shardedEdgeSerialization;
        this.nodeShardApproximation = nodeShardApproximation;
        this.timeService = timeService;
        this.graphFig = graphFig;
    }


    @Override
    public Iterator<ShardEntryGroup> getShards( final ApplicationScope scope, final Optional<Shard> maxShardId,
                                                final DirectedEdgeMeta directedEdgeMeta ) {

        ValidationUtils.validateApplicationScope( scope );
        Preconditions.checkNotNull( maxShardId, "maxShardId cannot be null" );
        GraphValidation.validateDirectedEdgeMeta( directedEdgeMeta );

        Iterator<Shard> existingShards;

        //its a new node, it doesn't need to check cassandra, it won't exist
        if ( isNewNode( directedEdgeMeta ) ) {
            existingShards = Collections.singleton( MIN_SHARD ).iterator();
        }

        else {
            existingShards = edgeShardSerialization.getShardMetaData( scope, maxShardId, directedEdgeMeta );
        }

        if ( existingShards == null || !existingShards.hasNext() ) {


            final MutationBatch batch = edgeShardSerialization.writeShardMeta( scope, MIN_SHARD, directedEdgeMeta );
            HystrixCassandra.user( batch  );

            existingShards = Collections.singleton( MIN_SHARD ).iterator();
        }

        return new ShardEntryGroupIterator( existingShards, graphFig.getShardMinDelta() );
    }


    @Override
    public boolean auditShard( final ApplicationScope scope, final ShardEntryGroup shardEntryGroup,
                               final DirectedEdgeMeta directedEdgeMeta ) {

        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateShardEntryGroup( shardEntryGroup );
        GraphValidation.validateDirectedEdgeMeta( directedEdgeMeta );

        Preconditions.checkNotNull( shardEntryGroup, "shardEntryGroup cannot be null" );


        /**
         * Nothing to do, it's been created very recently, we don't create a new one
         */
        if ( shardEntryGroup.isCompactionPending() ) {
            return false;
        }

        //we can't allocate, we have more than 1 write shard currently.  We need to compact first
        if ( shardEntryGroup.entrySize() != 1 ) {
            return false;
        }


        /**
         * Check the min shard in our system
         */
        final Shard shard = shardEntryGroup.getMinShard();


        if ( shard.getCreatedTime() >= getMinTime() ) {
            return false;
        }


        /**
         * Check out if we have a count for our shard allocation
         */

        final long count = nodeShardApproximation.getCount( scope, shard, directedEdgeMeta );


        if ( count < graphFig.getShardSize() ) {
            return false;
        }


        /**
         * Allocate the shard
         */

        final Iterator<MarkedEdge> edges = directedEdgeMeta
                .loadEdges( shardedEdgeSerialization, edgeColumnFamilies, scope, shardEntryGroup.getReadShards(), Long.MAX_VALUE );


        if ( !edges.hasNext() ) {
            LOG.warn( "Tried to allocate a new shard for edge meta data {}, "
                    + "but no max value could be found in that row", directedEdgeMeta );
            return false;
        }

        //we have a next, allocate it based on the max

        MarkedEdge marked = edges.next();

        final long createTimestamp = timeService.getCurrentTime();

        final Shard newShard = new Shard( marked.getTimestamp(), createTimestamp, false );


        final MutationBatch batch = this.edgeShardSerialization.writeShardMeta( scope, newShard, directedEdgeMeta );

        HystrixCassandra.user( batch );


        return true;
    }


    @Override
    public long getMinTime() {

        final long minimumAllowed = 2 * graphFig.getShardCacheTimeout();

        final long minDelta = graphFig.getShardMinDelta();


        if ( minDelta < minimumAllowed ) {
            throw new GraphRuntimeException( String.format(
                    "You must configure the property %s to be >= 2 x %s.  Otherwise you risk losing data",
                    GraphFig.SHARD_MIN_DELTA, GraphFig.SHARD_CACHE_TIMEOUT ) );
        }

        return timeService.getCurrentTime() - minDelta;
    }


    /**
     * Return true if the node has been created within our timeout.  If this is the case, we dont' need to check
     * cassandra, we know it won't exist
     */
    private boolean isNewNode( DirectedEdgeMeta directedEdgeMeta ) {

        //The timeout is in milliseconds.  Time for a time uuid is 1/10000 of a milli, so we need to get the units correct
        final long uuidDelta =  graphFig.getShardCacheTimeout()  * 10000;

        final long timeNow = UUIDGenerator.newTimeUUID().timestamp();

        for ( DirectedEdgeMeta.NodeMeta node : directedEdgeMeta.getNodes() ) {

            final long uuidTime = node.getId().getUuid().timestamp();

            final long uuidTimeDelta = uuidTime + uuidDelta;

            if ( uuidTimeDelta < timeNow ) {
                return true;
            }
        }

        return false;
    }
}
