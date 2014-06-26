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

package org.apache.usergrid.persistence.graph.serialization.impl.shard.count;


import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;

import org.apache.cassandra.thrift.Mutation;

import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.hystrix.HystrixCassandra;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardApproximation;
import org.apache.usergrid.persistence.model.entity.Id;

import com.netflix.astyanax.MutationBatch;
import com.netflix.hystrix.HystrixCommand;

import rx.functions.Action0;
import rx.schedulers.Schedulers;


/**
 * Implementation for doing edge approximation based on counters.  Uses a guava loading cache to load values from
 * cassandra, and flush them on cache eviction.
 */
public class NodeShardApproximationImpl implements NodeShardApproximation {

    /**
     * Read write locks to ensure we atomically swap correctly
     */
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLockLock = reentrantReadWriteLock.writeLock();

    private final GraphFig graphFig;
    private final NodeShardCounterSerialization nodeShardCounterSerialization;
    private final TimeService timeService;

    private volatile Counter currentCounter;


    /**
     * Create a time shard approximation with the correct configuration.
     */
    @Inject
    public NodeShardApproximationImpl( final GraphFig graphFig,
                                       final NodeShardCounterSerialization nodeShardCounterSerialization,
                                       final TimeService timeService ) {
        this.graphFig = graphFig;
        this.nodeShardCounterSerialization = nodeShardCounterSerialization;
        this.timeService = timeService;
        this.currentCounter = new Counter();
    }


    @Override
    public void increment( final ApplicationScope scope, final Id nodeId, final long shardId, final long count,
                           final String... edgeType ) {


        final ShardKey key = new ShardKey( scope, nodeId, shardId, edgeType );

        readLock.lock();


        try {
            currentCounter.add( key, count );
        }
        finally {
            readLock.unlock();
        }

        checkFlush();
    }


    @Override
    public long getCount( final ApplicationScope scope, final Id nodeId, final long shardId,
                          final String... edgeType ) {

        final ShardKey key = new ShardKey( scope, nodeId, shardId, edgeType );


        readLock.lock();

        long count;

        try {
            count = currentCounter.get( key );
        }
        finally {
            readLock.unlock();
        }


        //read from Cassandra and add to get a "close enough" number
        return count + nodeShardCounterSerialization.getCount( key );
    }


    @Override
    public void flush() {
        final Counter toFlush;

        writeLockLock.lock();

        try {
            toFlush = currentCounter;
            currentCounter = new Counter();
        }
        finally {
            writeLockLock.unlock();
        }


        //copy to the batch outside of the command for performance
        final MutationBatch batch =  nodeShardCounterSerialization.flush( toFlush );

        /**
         * Execute the command in hystrix to avoid slamming cassandra
         */
        new HystrixCommand( HystrixCassandra.ASYNC_GROUP ) {

            @Override
            protected Void run() throws Exception {
                /**
                 * Execute the batch asynchronously
                 */
                batch.execute();

                return null;
            }


            @Override
            protected Object getFallback() {
                //we've failed to mutate.  Merge this count back into the current one
                currentCounter.merge( toFlush );

                return null;
            }
        }.execute();
    }


    /**
     * Check if we need to flush.  If we do, perform the flush
     */
    private void checkFlush() {

        //we shouldn't flush we've not pass our timeout
        if ( currentCounter.getCreateTimestamp() + graphFig.getCounterFlushInterval() > timeService.getCurrentTime()
                //or we're not past the invocation count
                && currentCounter.getInvokeCount() < graphFig.getCounterFlushCount() ) {
            return;
        }


        /**
         * Fire the flush action asynchronously
         */
        Schedulers.immediate().createWorker().schedule( new Action0() {
            @Override
            public void call() {
                flush();
            }
        } );
    }
}
