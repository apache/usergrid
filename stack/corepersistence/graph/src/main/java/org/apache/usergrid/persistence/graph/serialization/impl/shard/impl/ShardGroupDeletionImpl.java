/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.AsyncTaskExecutor;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardGroupDeletion;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/**
 * Implementation of the shard group deletion task
 */
@Singleton
public class ShardGroupDeletionImpl implements ShardGroupDeletion {

    private static final Logger logger = LoggerFactory.getLogger( ShardGroupDeletionImpl.class );

    private final ListeningExecutorService asyncTaskExecutor;
    private final EdgeShardSerialization edgeShardSerialization;
    private final TimeService timeService;


    @Inject
    public ShardGroupDeletionImpl( final AsyncTaskExecutor asyncTaskExecutor,
                                   final EdgeShardSerialization edgeShardSerialization,
                                   final TimeService timeService ) {
        this.edgeShardSerialization = edgeShardSerialization;
        this.timeService = timeService;
        this.asyncTaskExecutor = asyncTaskExecutor.getExecutorService();
    }


    @Override
    public ListenableFuture<DeleteResult> maybeDeleteShard( final ApplicationScope applicationScope,
                                                            final DirectedEdgeMeta directedEdgeMeta,
                                                            final ShardEntryGroup shardEntryGroup,
                                                            final Iterator<MarkedEdge> edgeIterator ) {


        /**
         * Try and submit.  During back pressure, we may not be able to submit, that's ok.  Better to drop than to
         * hose the system
         */
        final ListenableFuture<DeleteResult> future;

        try {
            future = asyncTaskExecutor
                .submit( new ShardDeleteTask( applicationScope, directedEdgeMeta, shardEntryGroup, edgeIterator ) );
        }
        catch ( RejectedExecutionException ree ) {

            //ignore, if this happens we don't care, we're saturated, we can check later
            logger.error( "Rejected shard delete check for group {}", edgeIterator );

            return Futures.immediateFuture( DeleteResult.NOT_CHECKED );
        }


        /**
         * Log our success or failures for debugging purposes
         */
        Futures.addCallback( future, new FutureCallback<DeleteResult>() {
            @Override
            public void onSuccess( @Nullable final ShardGroupDeletion.DeleteResult result ) {
                if (logger.isTraceEnabled()) logger.trace( "Successfully completed delete of task {}", result );
            }


            @Override
            public void onFailure( final Throwable t ) {
                logger.error( "Unable to perform shard delete audit.  Exception is ", t );
            }
        } );

        return future;
    }


    /**
     * Execute the logic for the delete
     */
    private DeleteResult maybeDeleteShardInternal( final ApplicationScope applicationScope,
                                                   final DirectedEdgeMeta directedEdgeMeta,
                                                   final ShardEntryGroup shardEntryGroup,
                                                   final Iterator<MarkedEdge> edgeIterator ) {

        //Use ths to TEMPORARILY remove deletes from occurring
        //return DeleteResult.NO_OP;

        if (logger.isTraceEnabled()) logger.trace( "Beginning audit of shard group {}", shardEntryGroup );

        /**
         * Compaction is pending, we cannot check it
         */
        if ( shardEntryGroup.isCompactionPending() ) {
            if (logger.isTraceEnabled()) logger.trace( "Shard group {} is compacting, not auditing group", shardEntryGroup );
            return DeleteResult.COMPACTION_PENDING;
        }

        if (logger.isTraceEnabled()) logger.trace( "Shard group {} has no compaction pending", shardEntryGroup );

        final long currentTime = timeService.getCurrentTime();

        if ( shardEntryGroup.isNew( currentTime ) ) {
            if (logger.isTraceEnabled()) logger.trace( "Shard group {} contains a shard that is is too new, not auditing group", shardEntryGroup );
            return DeleteResult.TOO_NEW;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Shard group {} has passed the delta timeout at {}", shardEntryGroup, currentTime);
        }

        /**
         * We have edges, and therefore cannot delete them
         */
        if ( edgeIterator.hasNext() ) {
            if (logger.isTraceEnabled()) {
                logger.trace("Shard group {} has edges, not deleting", shardEntryGroup);
            }

            return DeleteResult.CONTAINS_EDGES;
        }


        if (logger.isTraceEnabled()) {
            logger.trace("Shard group {} has no edges continuing to delete", shardEntryGroup, currentTime);
        }


        //now we can proceed based on the shard meta state and we don't have any edge

        DeleteResult result = DeleteResult.NO_OP;

        MutationBatch rollup = null;

        for ( final Shard shard : shardEntryGroup.getReadShards() ) {

            //skip the min shard
            if(shard.isMinShard()){
                if (logger.isTraceEnabled()) {
                    logger.trace("Shard {} in group {} is the minimum, not deleting", shard, shardEntryGroup);
                }
                continue;
            }

            //The shard is not compacted, we cannot remove it.  This should never happen, a bit of an "oh shit" scenario.
            //the isCompactionPending should return false in this case
            if(!shard.isCompacted()){
                logger.warn( "Shard {} in group {} is not compacted yet was checked.  Short circuiting", shard, shardEntryGroup );
                return DeleteResult.NO_OP;
            }


            final MutationBatch shardRemovalMutation =
                edgeShardSerialization.removeShardMeta( applicationScope, shard, directedEdgeMeta );

            if ( rollup == null ) {
                rollup = shardRemovalMutation;
            }

            else {
                rollup.mergeShallow( shardRemovalMutation );
            }

            result = DeleteResult.DELETED;

            logger.info( "Removing shard {} in group {}", shard, shardEntryGroup );
        }


       if( rollup != null) {

           try {
               rollup.execute();
           }
           catch ( ConnectionException e ) {
               logger.error( "Unable to execute shard deletion", e );
               throw new RuntimeException( "Unable to execute shard deletion", e );
           }
       }

        if (logger.isTraceEnabled()) {
            logger.trace("Completed auditing shard group {}", shardEntryGroup);
        }

        return result;
    }


    /**
     * Glue for executing the task
     */
    private final class ShardDeleteTask implements Callable<DeleteResult> {

        private final ApplicationScope applicationScope;
        private final DirectedEdgeMeta directedEdgeMeta;
        private final ShardEntryGroup shardEntryGroup;
        private final Iterator<MarkedEdge> edgeIterator;


        private ShardDeleteTask( final ApplicationScope applicationScope, final DirectedEdgeMeta directedEdgeMeta,
                                 final ShardEntryGroup shardEntryGroup, final Iterator<MarkedEdge> edgeIterator ) {
            this.applicationScope = applicationScope;
            this.directedEdgeMeta = directedEdgeMeta;
            this.shardEntryGroup = shardEntryGroup;
            this.edgeIterator = edgeIterator;
        }


        @Override
        public DeleteResult call() throws Exception {
            return maybeDeleteShardInternal( applicationScope, directedEdgeMeta, shardEntryGroup, edgeIterator );
        }
    }
}
