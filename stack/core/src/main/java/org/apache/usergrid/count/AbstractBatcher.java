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
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    protected static final Logger logger = LoggerFactory.getLogger( AbstractBatcher.class );

    private volatile Batch batch;
    private final AtomicLong opCount = new AtomicLong();
    private final Timer addTimer =
            Metrics.newTimer( AbstractBatcher.class, "add_invocation", TimeUnit.MICROSECONDS, TimeUnit.SECONDS );
    protected final Counter invocationCounter = Metrics.newCounter( AbstractBatcher.class, "batch_add_invocations" );
    // TODO add batchCount, remove shouldSubmit, impl submit, change simpleBatcher to just be an extension
    protected int batchSize = 500;
    protected int batchIntervalSeconds = 10;
    private final AtomicLong batchSubmissionCount = new AtomicLong();

    /**
     * Create our scheduler to fire our execution
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 1 );


    /**
     * Set the batch interval in seconds
     * @param batchIntervalSeconds
     */
    public void setBatchInterval(int batchIntervalSeconds){
       this.batchIntervalSeconds  = batchIntervalSeconds;
    }

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


    private Batch getBatch() {
        Batch active = batch;
        if ( active == null ) {
            synchronized ( this ) {
                active = batch;
                if ( active == null ) {
                    batch = active = new Batch();

                    //now schedule our task for execution since we're creating a batch
                    scheduler.scheduleWithFixedDelay( new BatchFlusher(), this.batchIntervalSeconds,
                        this.batchIntervalSeconds, TimeUnit.SECONDS );

                }
            }
        }

        //we want to flush, and we have no capacity left, perform a flush
        if ( batchSize > 1 && active.getCapacity() == 0 ) {
            synchronized ( this ) {
                if ( active.getCapacity() == 0 ) {
                    active.flush();
                }
            }
        }

        return active;
    }

    private void flush(){
        synchronized(this) {
            getBatch().flush();
        }
    }


    /**
     * Runnable that will flush the batch every 30 seconds
     */
    private final class BatchFlusher implements Runnable {

        @Override
        public void run() {
            //explicitly flush the batch
            AbstractBatcher.this.flush();
        }
    }


    public long getBatchSubmissionCount() {
        return batchSubmissionCount.get();
    }


    class Batch {
        private BlockingQueue<Count> counts;
        private final AtomicInteger localCallCounter = new AtomicInteger();


        Batch() {
            counts = new ArrayBlockingQueue<>( batchSize );
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
                logger.error( "Unable to add count, dropping count {}", count, ex );
            }
        }


        void addSerial( Count count ) {
            Future f = batchSubmitter.submit( Arrays.asList( count ) );
            try {
                f.get();
            }
            catch ( Exception ex ) {
                logger.error( "Unable to add count, dropping count {}", count, ex );
            }
            batchSubmissionCount.incrementAndGet();
            opCount.incrementAndGet();
            localCallCounter.incrementAndGet();
        }


    }
}
