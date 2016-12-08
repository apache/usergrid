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
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import com.google.common.base.Optional;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Preconditions;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.hash.PrimitiveSink;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/**
 * Implementation of the shard group compaction
 */
@Singleton
public class ShardGroupCompactionImpl implements ShardGroupCompaction {


    private final AtomicLong countAudits;
    private static final Logger logger = LoggerFactory.getLogger( ShardGroupCompactionImpl.class );


    private static final Charset CHARSET = Charset.forName( "UTF-8" );

    private static final HashFunction MURMUR_128 = Hashing.murmur3_128();


    private final ListeningExecutorService taskExecutor;
    private final TimeService timeService;
    private final GraphFig graphFig;
    private final NodeShardAllocation nodeShardAllocation;
    private final ShardedEdgeSerialization shardedEdgeSerialization;
    private final EdgeColumnFamilies edgeColumnFamilies;
    private final Keyspace keyspace;
    private final EdgeShardSerialization edgeShardSerialization;

    private final Random random;
    private final ShardCompactionTaskTracker shardCompactionTaskTracker;
    private final ShardAuditTaskTracker shardAuditTaskTracker;
    private final NodeShardCache nodeShardCache;


    @Inject
    public ShardGroupCompactionImpl( final TimeService timeService, final GraphFig graphFig,
                                     final NodeShardAllocation nodeShardAllocation,
                                     final ShardedEdgeSerialization shardedEdgeSerialization,
                                     final EdgeColumnFamilies edgeColumnFamilies, final Keyspace keyspace,
                                     final EdgeShardSerialization edgeShardSerialization,
                                     final AsyncTaskExecutor asyncTaskExecutor,
                                     final NodeShardCache nodeShardCache ) {

        this.timeService = timeService;
        this.countAudits = new AtomicLong();
        this.graphFig = graphFig;
        this.nodeShardAllocation = nodeShardAllocation;
        this.shardedEdgeSerialization = shardedEdgeSerialization;
        this.edgeColumnFamilies = edgeColumnFamilies;
        this.keyspace = keyspace;
        this.edgeShardSerialization = edgeShardSerialization;

        this.random = new Random();
        this.shardCompactionTaskTracker = new ShardCompactionTaskTracker();
        this.shardAuditTaskTracker = new ShardAuditTaskTracker();


        this.taskExecutor = asyncTaskExecutor.getExecutorService();
        this.nodeShardCache = nodeShardCache;
    }


    /**
     * Execute the compaction task.  Will return the status the operations performed
     *
     * @param group The shard entry group to compact
     *
     * @return The result of the compaction operation
     */
    public CompactionResult compact( final ApplicationScope scope, final DirectedEdgeMeta edgeMeta,
                                     final ShardEntryGroup group ) {


        final long startTime = timeService.getCurrentTime();


        Preconditions.checkNotNull( group, "group cannot be null" );
        Preconditions.checkArgument( group.isCompactionPending(), "Compaction is pending" );
        Preconditions
            .checkArgument( group.shouldCompact( startTime ), "Compaction cannot be run yet.  Ignoring compaction." );

        if(logger.isTraceEnabled()) {
            logger.trace("Compacting shard group. Audit count is {} ", countAudits.get());
        }
        final CompactionResult.CompactionBuilder resultBuilder = CompactionResult.builder();

        final Shard targetShard = group.getCompactionTarget();

        final Set<Shard> sourceShards = new HashSet<>( group.getReadShards() );

        //remove the target
        sourceShards.remove( targetShard );


        final UUID timestamp = UUIDGenerator.newTimeUUID();

        final long newShardPivot = targetShard.getShardIndex();

        final int maxWorkSize = graphFig.getScanPageSize();




        /**
         * As we move edges, we want to keep track of it
         */
        long totalEdgeCount = 0;


        for ( Shard sourceShard : sourceShards ) {

            final MutationBatch newRowBatch = keyspace.prepareMutationBatch();
            final MutationBatch deleteRowBatch = keyspace.prepareMutationBatch();
            final MutationBatch updateShardMetaBatch = keyspace.prepareMutationBatch();

            long edgeCount = 0;

            Iterator<MarkedEdge> edges = edgeMeta
                .loadEdges( shardedEdgeSerialization, edgeColumnFamilies, scope, Collections.singleton( sourceShard ),
                    Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING );

            MarkedEdge shardEnd = null;

            while ( edges.hasNext() ) {
                final MarkedEdge edge = edges.next();

                final long edgeTimestamp = edge.getTimestamp();

                shardEnd = edge;

                /**
                 * The edge is within a different shard, break
                 */
                if ( edgeTimestamp < newShardPivot ) {
                    break;
                }

                newRowBatch.mergeShallow( edgeMeta
                        .writeEdge( shardedEdgeSerialization, edgeColumnFamilies, scope, targetShard, edge,
                            timestamp ) );

                deleteRowBatch.mergeShallow( edgeMeta
                        .deleteEdge( shardedEdgeSerialization, edgeColumnFamilies, scope, sourceShard, edge,
                            timestamp ) );


                edgeCount++;



                // if we're at our count, execute the mutation of writing the edges to the new row, then remove them
                // from the old rows
                if ( edgeCount % maxWorkSize == 0 ) {



                    try {

                        // write the edges into the new shard atomically so we know they all succeed
                        newRowBatch.withAtomicBatch(true).execute();


                        // Update the shard end after each batch so any reads during transition stay as close to current
                        sourceShard.setShardEnd(
                            Optional.of(new DirectedEdge(shardEnd.getTargetNode(), shardEnd.getTimestamp()))
                        );

                        if(logger.isTraceEnabled()) {
                            logger.trace("Updating shard {} during batch removal with shardEnd {}", sourceShard, shardEnd);
                        }
                        updateShardMetaBatch.mergeShallow(
                            edgeShardSerialization.writeShardMeta(scope, sourceShard, edgeMeta));



                        // on purpose block this thread before deleting the old edges to be sure there are no gaps
                        // duplicates are filtered on graph seeking so this is OK
                        Thread.sleep(1000);

                        if(logger.isTraceEnabled()) {
                            logger.trace("Deleting batch of {} from old shard", maxWorkSize);
                        }
                        deleteRowBatch.withAtomicBatch(true).execute();

                        updateShardMetaBatch.execute();


                    }
                    catch ( Throwable t ) {
                        logger.error( "Unable to move edges from shard {} to shard {}", sourceShard, targetShard );
                    }

                    totalEdgeCount += edgeCount;
                    edgeCount = 0;
                }



            }

            totalEdgeCount += edgeCount;

            try {

                // write the edges into the new shard atomically so we know they all succeed
                newRowBatch.withAtomicBatch(true).execute();

                // on purpose block this thread before deleting the old edges to be sure there are no gaps
                // duplicates are filtered on graph seeking so this is OK
                Thread.sleep(1000);

                if(logger.isTraceEnabled()) {
                    logger.trace("Deleting remaining {} edges from old shard", edgeCount);
                }
                deleteRowBatch.withAtomicBatch(true).execute();

                if (shardEnd != null){

                    sourceShard.setShardEnd(
                        Optional.of(new DirectedEdge(shardEnd.getTargetNode(), shardEnd.getTimestamp()))
                    );

                    if(logger.isTraceEnabled()) {
                        logger.trace("Updating for last time shard {} with shardEnd {}", sourceShard, shardEnd);
                    }
                    updateShardMetaBatch.mergeShallow( edgeShardSerialization.writeShardMeta(scope, sourceShard, edgeMeta));
                    updateShardMetaBatch.execute();
                }


            }
            catch ( Throwable t ) {
                logger.error( "Unable to move edges to target shard {}", targetShard );
            }



        }




        if (logger.isTraceEnabled()) {
            logger.trace("Finished compacting {} shards and moved {} edges", sourceShards, totalEdgeCount);
        }

        resultBuilder.withCopiedEdges( totalEdgeCount ).withSourceShards( sourceShards ).withTargetShard( targetShard );

        /**
         * We didn't move anything this pass, mark the shard as compacted.  If we move something,
         * it means that we missed it on the first pass
         * or someone is still not writing to the target shard only.
         */
        if ( totalEdgeCount == 0 ) {


            // now that we've marked our target as compacted, we can successfully remove any shards that are not
            // compacted themselves in the sources

            final MutationBatch shardRemovalRollup = keyspace.prepareMutationBatch();

            for ( Shard source : sourceShards ) {

                //if we can't safely delete it, don't do so
                if ( !group.canBeDeleted( source ) ) {
                    continue;
                }

                logger.info( "Source shards have been fully drained.  Removing shard {}", source );

                final MutationBatch shardRemoval = edgeShardSerialization.removeShardMeta( scope, source, edgeMeta );
                shardRemovalRollup.mergeShallow( shardRemoval );

                resultBuilder.withRemovedShard( source );
            }


            try {
                shardRemovalRollup.execute();

                // invalidate the shard cache so we can be sure that all read shards are up to date
                nodeShardCache.invalidate(scope, edgeMeta);
            }
            catch ( ConnectionException e ) {
                throw new RuntimeException( "Unable to connect to cassandra", e );
            }

            //Overwrite our shard index with a newly created one that has been marked as compacted
            Shard compactedShard = new Shard( targetShard.getShardIndex(), timeService.getCurrentTime(), true );
            compactedShard.setShardEnd(Optional.absent());

            if(logger.isTraceEnabled()) {
                logger.trace("Shard has been fully compacted.  Marking shard {} as compacted in Cassandra", compactedShard);
            }

            final MutationBatch updateMark = edgeShardSerialization.writeShardMeta( scope, compactedShard, edgeMeta );
            try {
                updateMark.execute();

                // invalidate the shard cache so we can be sure that all read shards are up to date
                nodeShardCache.invalidate(scope, edgeMeta);
            }
            catch ( ConnectionException e ) {
                throw new RuntimeException( "Unable to connect to cassandra", e );
            }

            resultBuilder.withCompactedShard( compactedShard );
        }

        return resultBuilder.build();
    }


    @Override
    public ListenableFuture<AuditResult> evaluateShardGroup( final ApplicationScope scope,
                                                             final DirectedEdgeMeta edgeMeta,
                                                             final ShardEntryGroup group ) {

        final double repairChance = random.nextDouble();


        //don't audit, we didn't hit our chance
        if ( repairChance > graphFig.getShardRepairChance() ) {
            return Futures.immediateFuture( AuditResult.NOT_CHECKED );
        }

        countAudits.getAndIncrement();

        if(logger.isTraceEnabled()) {
            logger.trace("Auditing shard group {}. count is {} ", group, countAudits.get());
        }

        /**
         * Try and submit.  During back pressure, we may not be able to submit, that's ok.  Better to drop than to
         * hose the system
         */
        final ListenableFuture<AuditResult> future;

        try {
            future = taskExecutor.submit( new ShardAuditTask( scope, edgeMeta, group ) );
        }
        catch ( RejectedExecutionException ree ) {

            // ignore, if this happens we don't care, we're saturated, we can check later
            logger.info( "Rejected audit for shard of scope {} edge, meta {} and group {}", scope, edgeMeta, group );

            return Futures.immediateFuture( AuditResult.NOT_CHECKED );
        }

        /**
         * Log our success or failures for debugging purposes
         */
        Futures.addCallback( future, new FutureCallback<AuditResult>() {
            @Override
            public void onSuccess( @Nullable final AuditResult result ) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Successfully completed audit of task {}", result);
                }
            }


            @Override
            public void onFailure( final Throwable t ) {
                logger.error( "Unable to perform audit.  Exception is ", t );
            }
        } );

        return future;
    }


    private final class ShardAuditTask implements Callable<AuditResult> {

        private final ApplicationScope scope;
        private final DirectedEdgeMeta edgeMeta;
        private final ShardEntryGroup group;


        public ShardAuditTask( final ApplicationScope scope, final DirectedEdgeMeta edgeMeta,
                               final ShardEntryGroup group ) {
            this.scope = scope;
            this.edgeMeta = edgeMeta;
            this.group = group;
        }


        @Override
        public AuditResult call() throws Exception {
            /**
             * We don't have a compaction pending.  Run an audit on the shards
             */
            if ( !group.isCompactionPending() ) {

                /**
                 * Check if we should allocate, we may want to
                 */

                /**
                 * It's already compacting, don't do anything
                 */
                if ( !shardAuditTaskTracker.canStartTask( scope, edgeMeta, group ) ) {
                    return AuditResult.CHECKED_NO_OP;
                }

                try {

                    final boolean created = nodeShardAllocation.auditShard( scope, group, edgeMeta );
                    if ( !created ) {
                        return AuditResult.CHECKED_NO_OP;
                    }
                }
                finally {
                    shardAuditTaskTracker.complete( scope, edgeMeta, group );
                }


                return AuditResult.CHECKED_CREATED;
            }

            //check our taskmanager


            /**
             * Do the compaction
             */
            if ( group.shouldCompact( timeService.getCurrentTime() ) ) {
                /**
                 * It's already compacting, don't do anything
                 */
                if ( !shardCompactionTaskTracker.canStartTask( scope, edgeMeta, group ) ) {

                    if(logger.isTraceEnabled()) {
                        logger.trace("Already compacting, won't compact group: {}", group);
                    }


                    return AuditResult.COMPACTING;
                }

                /**
                 * We use a finally b/c we always want to remove the task track
                 */
                try {
                    CompactionResult result = compact( scope, edgeMeta, group );
                    if(logger.isTraceEnabled()) {
                        logger.trace("Compaction result for compaction of scope {} with edge meta data of {} and shard group {} is {}",
                            scope, edgeMeta, group, result);
                    }
                }
                finally {
                    shardCompactionTaskTracker.complete( scope, edgeMeta, group );
                }
                return AuditResult.COMPACTED;
            }

            //no op, there's nothing we need to do to this shard
            return AuditResult.NOT_CHECKED;
        }
    }


    /**
     * Inner class used to track running tasks per instance
     */
    private static abstract class TaskTracker {

        private static final Boolean TRUE = true;

        private ConcurrentHashMap<Long, Boolean> runningTasks = new ConcurrentHashMap<>();


        /**
         * Sets this data into our scope to signal it's running to stop other threads from attempting to run
         */
        public boolean canStartTask( final ApplicationScope scope, final DirectedEdgeMeta edgeMeta,
                                     ShardEntryGroup group ) {
            final Long hash = doHash( scope, edgeMeta, group ).hash().asLong();
            final Boolean returned = runningTasks.putIfAbsent( hash, TRUE );

            /**
             * Someone already put the value
             */
            return returned == null;
        }


        /**
         * Mark this entry group as complete
         */
        public void complete( final ApplicationScope scope, final DirectedEdgeMeta edgeMeta, ShardEntryGroup group ) {
            final long hash = doHash( scope, edgeMeta, group ).hash().asLong();
            runningTasks.remove( hash );
        }


        protected abstract Hasher doHash( final ApplicationScope scope, final DirectedEdgeMeta directedEdgeMeta,
                                          final ShardEntryGroup shardEntryGroup );
    }


    /**
     * Task tracker for shard compaction
     */
    private static final class ShardCompactionTaskTracker extends ShardAuditTaskTracker {

        /**
         * Hash our data into a consistent long
         */
        @Override
        protected Hasher doHash( final ApplicationScope scope, final DirectedEdgeMeta directedEdgeMeta,
                                 final ShardEntryGroup shardEntryGroup ) {

            final Hasher hasher = super.doHash( scope, directedEdgeMeta, shardEntryGroup );

            // add the compaction target to the hash
            final Shard compactionTarget = shardEntryGroup.getCompactionTarget();

            hasher.putLong( compactionTarget.getShardIndex() );


            return hasher;
        }
    }


    /**
     * Task tracker for shard audit
     */
    private static class ShardAuditTaskTracker extends TaskTracker {

        /**
         * Hash our data into a consistent long
         */
        protected Hasher doHash( final ApplicationScope scope, final DirectedEdgeMeta directedEdgeMeta,
                                 final ShardEntryGroup shardEntryGroup ) {

            final Hasher hasher = MURMUR_128.newHasher();


            addToHash( hasher, scope.getApplication() );


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


            return hasher;
        }


        protected void addToHash( final PrimitiveSink into, final Id id ) {

            final UUID nodeUuid = id.getUuid();
            final String nodeType = id.getType();

            into.putLong( nodeUuid.getMostSignificantBits() ).putLong( nodeUuid.getLeastSignificantBits() )
                .putString( nodeType, CHARSET );
        }
    }


    public static final class CompactionResult {

        public final long copiedEdges;
        public final Shard targetShard;
        public final Set<Shard> sourceShards;
        public final Set<Shard> removedShards;
        public final Shard compactedShard;


        private CompactionResult( final long copiedEdges, final Shard targetShard, final Set<Shard> sourceShards,
                                  final Set<Shard> removedShards, final Shard compactedShard ) {
            this.copiedEdges = copiedEdges;
            this.targetShard = targetShard;
            this.compactedShard = compactedShard;
            this.sourceShards = Collections.unmodifiableSet( sourceShards );
            this.removedShards = Collections.unmodifiableSet( removedShards );
        }


        /**
         * Create a builder to use to create the result
         */
        public static CompactionBuilder builder() {
            return new CompactionBuilder();
        }


        @Override
        public String toString() {
            return "CompactionResult{" +
                "copiedEdges=" + copiedEdges +
                ", targetShard=" + targetShard +
                ", sourceShards=" + sourceShards +
                ", removedShards=" + removedShards +
                ", compactedShard=" + compactedShard +
                '}';
        }


        public static final class CompactionBuilder {
            private long copiedEdges;
            private Shard targetShard;
            private Set<Shard> sourceShards;
            private Set<Shard> removedShards = new HashSet<>();
            private Shard compactedShard;


            public CompactionBuilder withCopiedEdges( final long copiedEdges ) {
                this.copiedEdges = copiedEdges;
                return this;
            }


            public CompactionBuilder withTargetShard( final Shard targetShard ) {
                this.targetShard = targetShard;
                return this;
            }


            public CompactionBuilder withSourceShards( final Set<Shard> sourceShards ) {
                this.sourceShards = sourceShards;
                return this;
            }


            public CompactionBuilder withRemovedShard( final Shard removedShard ) {
                this.removedShards.add( removedShard );
                return this;
            }


            public CompactionBuilder withCompactedShard( final Shard compactedShard ) {
                this.compactedShard = compactedShard;
                return this;
            }


            public CompactionResult build() {
                return new CompactionResult( copiedEdges, targetShard, sourceShards, removedShards, compactedShard );
            }
        }
    }
}
