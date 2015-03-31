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
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.exception.GraphRuntimeException;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeColumnFamilies;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardAllocation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardApproximation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardGroupCompaction;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardedEdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.util.GraphValidation;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.util.TimeUUIDUtils;


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
    private final ShardGroupCompaction shardGroupCompaction;


    @Inject
    public NodeShardAllocationImpl( final EdgeShardSerialization edgeShardSerialization,
                                    final EdgeColumnFamilies edgeColumnFamilies,
                                    final ShardedEdgeSerialization shardedEdgeSerialization,
                                    final NodeShardApproximation nodeShardApproximation, final TimeService timeService,
                                    final GraphFig graphFig, final ShardGroupCompaction shardGroupCompaction ) {
        this.edgeShardSerialization = edgeShardSerialization;
        this.edgeColumnFamilies = edgeColumnFamilies;
        this.shardedEdgeSerialization = shardedEdgeSerialization;
        this.nodeShardApproximation = nodeShardApproximation;
        this.timeService = timeService;
        this.graphFig = graphFig;
        this.shardGroupCompaction = shardGroupCompaction;
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
            try {
                batch.execute();
            }
            catch ( ConnectionException e ) {
                throw new RuntimeException( "Unable to connect to casandra", e );
            }

            existingShards = Collections.singleton( MIN_SHARD ).iterator();
        }

        return new ShardEntryGroupIterator( existingShards, graphFig.getShardMinDelta(), shardGroupCompaction, scope,
                directedEdgeMeta );
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

        final long shardSize = graphFig.getShardSize();


        if ( count < shardSize ) {
            return false;
        }

        /**
         * We want to allocate a new shard as close to the max value as possible.  This way if we're filling up a shard rapidly, we split it near the head of the values.
         * Further checks to this group will result in more splits, similar to creating a tree type structure and splitting each node.
         *
         * This means that the lower shard can be re-split later if it is still too large.  We do the division to truncate
         * to a split point < what our current max is that would be approximately be our pivot ultimately if we split from the
         * lower bound and moved forward.  Doing this will stop the current shard from expanding and avoid a point where we cannot
         * ultimately compact to the correct shard size.
         */


        /**
         * Allocate the shard
         */

        final Iterator<MarkedEdge> edges = directedEdgeMeta
                .loadEdges( shardedEdgeSerialization, edgeColumnFamilies, scope, shardEntryGroup.getReadShards(), 0,
                        SearchByEdgeType.Order.ASCENDING );


        if ( !edges.hasNext() ) {
            LOG.warn( "Tried to allocate a new shard for edge meta data {}, "
                    + "but no max value could be found in that row", directedEdgeMeta );
            return false;
        }


        MarkedEdge marked = null;

        /**
         * Advance to the pivot point we should use.  Once it's compacted, we can split again.
         * We either want to take the first one (unlikely) or we take our total count - the shard size.
         * If this is a negative number, we're approaching our max count for this shard, so the first
         * element will suffice.
         */


        for(long i = 1;  edges.hasNext(); i++){
            //we hit a pivot shard, set it since it could be the last one we encounter
            if(i% shardSize == 0){
                marked = edges.next();
            }
            else{
                edges.next();
            }
        }


        /**
         * Sanity check in case our counters become severely out of sync with our edge state in cassandra.
         */
        if(marked == null){
            LOG.warn( "Incorrect shard count for shard group {}", shardEntryGroup );
            return false;
        }

        final long createTimestamp = timeService.getCurrentTime();

        final Shard newShard = new Shard( marked.getTimestamp(), createTimestamp, false );

        LOG.info( "Allocating new shard {} for edge meta {}", newShard, directedEdgeMeta );

        final MutationBatch batch = this.edgeShardSerialization.writeShardMeta( scope, newShard, directedEdgeMeta );

        try {
            batch.execute();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to connect to casandra", e );
        }


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


        //TODO: TN this is broken....
        //The timeout is in milliseconds.  Time for a time uuid is 1/10000 of a milli, so we need to get the units correct
        final long timeoutDelta = graphFig.getShardCacheTimeout() ;

        final long timeNow = timeService.getCurrentTime();

        boolean isNew = true;

        for ( DirectedEdgeMeta.NodeMeta node : directedEdgeMeta.getNodes() ) {

            //short circuit
            if(!isNew || node.getId().getUuid().version() > 2){
                return false;
            }

            final long uuidTime =   TimeUUIDUtils.getTimeFromUUID( node.getId().getUuid());

            final long newExpirationTimeout = uuidTime + timeoutDelta;

            //our expiration is after our current time, treat it as new
            isNew = isNew && newExpirationTimeout >  timeNow;
        }

        return isNew;
    }
}
