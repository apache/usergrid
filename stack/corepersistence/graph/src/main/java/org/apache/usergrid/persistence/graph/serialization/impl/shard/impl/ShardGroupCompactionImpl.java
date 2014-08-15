/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeColumnFamilies;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardAllocation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardGroupCompaction;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardedEdgeSerialization;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Implementation of the shard group compaction
 */
@Singleton
public class ShardGroupCompactionImpl implements ShardGroupCompaction {


    private static final Charset CHARSET = Charset.forName( "UTF-8" );

    private static final HashFunction MURMUR_128 = Hashing.murmur3_128();

    private final TimeService timeService;
    private final GraphFig graphFig;
    private final NodeShardAllocation nodeShardAllocation;
    private final ShardedEdgeSerialization shardedEdgeSerialization;
    private final EdgeColumnFamilies edgeColumnFamilies;
    private final EdgeShardSerialization edgeShardSerialization;


    private final Random random;
    private final ShardCompactionTaskTracker shardCompactionTaskTracker;


    @Inject
    public ShardGroupCompactionImpl( final TimeService timeService, final GraphFig graphFig,
                                     final NodeShardAllocation nodeShardAllocation,
                                     final ShardedEdgeSerialization shardedEdgeSerialization,
                                     final EdgeColumnFamilies edgeColumnFamilies,
                                     final EdgeShardSerialization edgeShardSerialization ) {

        this.timeService = timeService;
        this.graphFig = graphFig;
        this.nodeShardAllocation = nodeShardAllocation;
        this.shardedEdgeSerialization = shardedEdgeSerialization;
        this.edgeColumnFamilies = edgeColumnFamilies;
        this.edgeShardSerialization = edgeShardSerialization;

        this.random = new Random();
        this.shardCompactionTaskTracker = new ShardCompactionTaskTracker();
    }


    @Override
    public Set<Shard> compact( final ApplicationScope scope, final DirectedEdgeMeta edgeMeta,
                               final ShardEntryGroup group ) {
        final long startTime = timeService.getCurrentTime();

        Preconditions.checkNotNull( group, "group cannot be null" );
        Preconditions.checkArgument( group.isCompactionPending(), "Compaction is pending" );
        Preconditions.checkArgument( group.shouldCompact( startTime ), "Compaction can now be run" );

        /**
         * It's already compacting, don't do anything
         */
        if (!shardCompactionTaskTracker.shouldStartCompaction( scope, edgeMeta, group )){
            return Collections.emptySet();
        }


        final Shard targetShard = group.getCompactionTarget();

        final Collection<Shard> sourceShards = group.getReadShards();



        Observable.create( new ObservableIterator<MarkedEdge>( "Shard_Repair" ) {
            @Override
            protected Iterator<MarkedEdge> getIterator() {
                return edgeMeta.loadEdges( shardedEdgeSerialization, edgeColumnFamilies, scope, group.getReadShards(), Long.MAX_VALUE );
            }
        } ).buffer( graphFig.getScanPageSize() ).doOnNext( new Action1<List<MarkedEdge>>() {
            @Override
            public void call( final List<MarkedEdge> markedEdges ) {

            }
        }).doOnNext( new Action1<List<MarkedEdge>>() {
            @Override
            public void call( final List<MarkedEdge> markedEdges ) {

            }
        } );





        return null;
    }


    @Override
    public AuditResult evaluateShardGroup( final ApplicationScope scope, final DirectedEdgeMeta edgeMeta,
                                           final ShardEntryGroup group ) {


        final double repairChance = random.nextDouble();

        //don't repair
        if ( repairChance > graphFig.getShardRepairChance() ) {
            return AuditResult.NOT_CHECKED;
        }


        /**
         * We don't have a compaction pending.  Run an audit on the shards
         */
        if ( !group.isCompactionPending() ) {

            /**
             * Check if we should allocate, we may want to
             */


            final boolean created = nodeShardAllocation.auditShard( scope, group, edgeMeta );


            if ( !created ) {
                return AuditResult.CHECKED_NO_OP;
            }


            return AuditResult.CHECKED_CREATED;
        }

        //check our taskmanager


        /**
         * Do the compaction
         */
        if ( group.shouldCompact( timeService.getCurrentTime() ) ) {
            compact( scope, edgeMeta, group );
            return AuditResult.COMPACTING;
        }

        //no op, there's nothing we need to do to this shard
        return AuditResult.NOT_CHECKED;
    }


    private static final class ShardCompactionTaskTracker {
        private BitSet runningTasks = new BitSet();


        /**
         * Sets this data into our scope to signal it's running to stop other threads from attempting to run
         * @param scope
         * @param edgeMeta
         * @param group
         * @return
         */
        public boolean shouldStartCompaction( final ApplicationScope scope, final DirectedEdgeMeta edgeMeta,
                                     ShardEntryGroup group ) {
            final int hash = doHash( scope, edgeMeta, group ).asInt();

            if(runningTasks.get( hash )){
                return false;
            }

            runningTasks.set( hash );

            return true;
        }


        /**
         * Mark this entry group as complete
         * @param scope
         * @param edgeMeta
         * @param group
         */
        public void complete( final ApplicationScope scope, final DirectedEdgeMeta edgeMeta,
                                             ShardEntryGroup group ) {
            final int hash = doHash( scope, edgeMeta, group ).asInt();
            runningTasks.clear( hash );
        }


        /**
         * Hash our data into a consistent long
         * @param scope
         * @param directedEdgeMeta
         * @param shardEntryGroup
         * @return
         */
        private HashCode doHash( final ApplicationScope scope, final DirectedEdgeMeta directedEdgeMeta,
                                 final ShardEntryGroup shardEntryGroup ) {

            final Hasher hasher = MURMUR_128.newHasher();


            addToHash( hasher, scope.getApplication() );

            /**
             * add our edge meta data
             */
            for ( DirectedEdgeMeta.NodeMeta nodeMeta : directedEdgeMeta.getNodes() ) {
                addToHash( hasher, nodeMeta.getId() );
                hasher.putInt( nodeMeta.getNodeType().getStorageValue() );
            }


            /**
             * Add our edge type
             */
            for ( String type : directedEdgeMeta.getTypes() ) {
                hasher.putString( type, CHARSET );
            }

            //add our compaction target to the hash
            final Shard compactionTarget = shardEntryGroup.getCompactionTarget();

            hasher.putLong( compactionTarget.getShardIndex() );


            return hasher.hash();
        }


        private void addToHash( final Hasher hasher, final Id id ) {

            final UUID nodeUuid = id.getUuid();
            final String nodeType = id.getType();

            hasher.putLong( nodeUuid.getMostSignificantBits() ).putLong( nodeUuid.getLeastSignificantBits() )
                  .putString( nodeType, CHARSET );
        }
    }

    private enum StartResult{
        /**
         * Returned if the compaction was started
         */

        STARTED,

        /**
         * Returned if we are running the compaction
         */
        RUNNING;
    }
}
