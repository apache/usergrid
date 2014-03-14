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
package org.apache.usergrid.count;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.usergrid.count.common.Count;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;


/**
 * Base batcher implementation, handles concurrency and locking throughput throttling.
 *
 * @author zznate
 */
public abstract class AbstractBatcher implements Batcher {
    protected BatchSubmitter batchSubmitter;

    private volatile Batch batch;
    private final AtomicLong opCount = new AtomicLong();
    private final Timer addTimer =
            Metrics.newTimer( AbstractBatcher.class, "add_invocation", TimeUnit.MICROSECONDS, TimeUnit.SECONDS );
    protected final Counter invocationCounter = Metrics.newCounter( AbstractBatcher.class, "batch_add_invocations" );
    private final Counter existingCounterHit = Metrics.newCounter( AbstractBatcher.class, "counter_existed" );
    // TODO add batchCount, remove shouldSubmit, impl submit, change simpleBatcher to just be an extension
    protected int batchSize = 500;
    private final AtomicLong batchSubmissionCount = new AtomicLong();
    private final AtomicBoolean lock = new AtomicBoolean( false );


    public void setBatchSize( int batchSize ) {
        this.batchSize = batchSize;
    }


    public void setBatchSubmitter( BatchSubmitter batchSubmitter ) {
        this.batchSubmitter = batchSubmitter;
    }


    /**
     * Individual {@link Count} for the same counter get rolled up, so we track the individual number of operations.
     *
     * @return the number of operation against this SimpleBatcher
     */
    public long getOpCount() {
        return opCount.get();
    }


    /** Add a count object to this batcher */
    public void add( Count count ) throws CounterProcessingUnavailableException {
        invocationCounter.inc();
        final TimerContext context = addTimer.time();
        if ( batchSize == 1 ) {
            getBatch().addSerial( count );
        }
        else {
            getBatch().add( count );
        }
        context.stop();
    }


    Batch getBatch() {
        Batch active = batch;
        if ( active == null ) {
            synchronized ( this ) {
                active = batch;
                if ( active == null ) {
                    batch = active = new Batch();
                }
            }
        }
        if ( batchSize > 1 && active.getCapacity() == 0 ) {
            synchronized ( this ) {
                if ( active.getCapacity() == 0 ) {
                    active.flush();
                }
            }
        }
        return active;
    }


    public long getBatchSubmissionCount() {
        return batchSubmissionCount.get();
    }


    class Batch {
        private BlockingQueue<Count> counts;
        private final AtomicInteger localCallCounter = new AtomicInteger();

        private final AtomicBoolean lock = new AtomicBoolean( false );


        Batch() {
            counts = new ArrayBlockingQueue<Count>( batchSize );
        }


        int getCapacity() {
            return counts.remainingCapacity();
        }


        void flush() {
            ArrayList<Count> flushed = new ArrayList<Count>( batchSize );
            counts.drainTo( flushed );
            batchSubmitter.submit( flushed );
            batchSubmissionCount.incrementAndGet();
            opCount.incrementAndGet();
            localCallCounter.incrementAndGet();
        }


        void add( Count count ) {
            try {
                counts.offer( count, 500, TimeUnit.MILLISECONDS );
            }
            catch ( Exception ex ) {
                ex.printStackTrace();
            }
        }


        void addSerial( Count count ) {
            Future f = batchSubmitter.submit( Arrays.asList( count ) );
            try {
                f.get();
            }
            catch ( Exception ex ) {
                ex.printStackTrace();
            }
            batchSubmissionCount.incrementAndGet();
            opCount.incrementAndGet();
            localCallCounter.incrementAndGet();
        }


        /**
         * The number of times the {@link #add(org.apache.usergrid.count.common.Count)} method has been invoked on this batch
         * instance
         */
        public int getLocalCallCount() {
            return localCallCounter.get();
        }
    }
}
