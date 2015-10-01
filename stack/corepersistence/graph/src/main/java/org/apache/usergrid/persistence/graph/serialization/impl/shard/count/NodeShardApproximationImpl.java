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


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardApproximation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;

import com.netflix.astyanax.MutationBatch;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;

import rx.functions.Action0;
import rx.schedulers.Schedulers;


/**
 * Implementation for doing edge approximation based on counters.  Uses a guava loading cache to load values from
 * cassandra, and beginFlush them on cache eviction.
 */
public class NodeShardApproximationImpl implements NodeShardApproximation {

    private static final Logger LOG = LoggerFactory.getLogger(NodeShardApproximationImpl.class);

    /**
     * Read write locks to ensure we atomically swap correctly
     */
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLockLock = reentrantReadWriteLock.writeLock();

    private final GraphFig graphFig;
    private final NodeShardCounterSerialization nodeShardCounterSerialization;
    private final TimeService timeService;

    /**
     * Counter currently implemented
     */
    private volatile Counter currentCounter;

    /**
     * The counter that is currently in process of flushing to Cassandra.  Can be null
     */
    private final BlockingQueue<Counter> flushQueue;

    private final FlushWorker worker;

    /**
        * Command group used for realtime user commands
        */
       public static final HystrixCommand.Setter
           COUNT_GROUP = HystrixCommand.Setter.withGroupKey(
               HystrixCommandGroupKey.Factory.asKey( "BatchCounterRollup" ) ).andThreadPoolPropertiesDefaults(
                   HystrixThreadPoolProperties.Setter().withCoreSize( 100 ) );


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
        this.flushQueue = new LinkedBlockingQueue<>( graphFig.getCounterFlushQueueSize() );

        this.worker = new FlushWorker( this.flushQueue, nodeShardCounterSerialization );

        Schedulers.newThread().createWorker().schedule( worker );

    }


    @Override
    public void increment(
            final ApplicationScope scope, final Shard shard,
            final long count, final DirectedEdgeMeta directedEdgeMeta  ) {


        final ShardKey key = new ShardKey( scope, shard, directedEdgeMeta );

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
    public long getCount( final ApplicationScope scope, final Shard shard, final DirectedEdgeMeta directedEdgeMeta ) {

        final ShardKey key = new ShardKey( scope, shard, directedEdgeMeta );


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
    public void beginFlush() {

        writeLockLock.lock();

        try {

            final boolean queued = flushQueue.offer( currentCounter );

            /**
             * We were able to q the beginFlush, swap it
             */
            if ( queued ) {
                currentCounter = new Counter();
            }
        }
        finally {
            writeLockLock.unlock();
        }
    }


    @Override
    public boolean flushPending() {
        return flushQueue.size() > 0 || worker.isFlushing();
    }


    /**
     * Check if we need to beginFlush.  If we do, perform the beginFlush
     */
    private void checkFlush() {

        //there's no beginFlush pending and we're past the timeout or count
        if ( currentCounter.getCreateTimestamp() + graphFig.getCounterFlushInterval() > timeService.getCurrentTime()
                || currentCounter.getInvokeCount() >= graphFig.getCounterFlushCount() ) {
            beginFlush();
        }
    }


    /**
     * Worker that will take from the queue
     */
    private static class FlushWorker implements Action0 {

        private final BlockingQueue<Counter> counterQueue;
        private final NodeShardCounterSerialization nodeShardCounterSerialization;

        private volatile Counter rollUp;


        private FlushWorker( final BlockingQueue<Counter> counterQueue,
                             final NodeShardCounterSerialization nodeShardCounterSerialization ) {
            this.counterQueue = counterQueue;
            this.nodeShardCounterSerialization = nodeShardCounterSerialization;
        }


        @Override
        public void call() {


            while ( true ) {
                /**
                 * Block taking the first element.  Once we take this, batch drain and roll up the rest
                 */

                try {
                    rollUp = null;
                    rollUp = counterQueue.take();
                }
                catch ( InterruptedException e ) {
                    LOG.error( "Unable to read from counter queue", e );
                    throw new RuntimeException( "Unable to read from counter queue", e );

                }




                //copy to the batch outside of the command for performance
                final MutationBatch batch = nodeShardCounterSerialization.flush( rollUp );

                /**
                 * Execute the command in hystrix to avoid slamming cassandra
                 */
                new HystrixCommand( COUNT_GROUP ) {

                    @Override
                    protected Void run() throws Exception {
                        batch.execute();

                        return null;
                    }


                    @Override
                    protected Object getFallback() {
                        //we've failed to mutate.  Merge this count back into the current one
                        counterQueue.offer( rollUp );

                        return null;
                    }
                }.execute();
            }

        }


        /**
         * Return true if we're in the process of flushing
         * @return
         */
        public boolean isFlushing(){
            return rollUp != null;
        }
    }
}
