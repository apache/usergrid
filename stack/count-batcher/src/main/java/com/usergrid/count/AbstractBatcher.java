/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.usergrid.count;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import com.usergrid.count.common.Count;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

/**
 * Base batcher implementation, handles concurrency and locking throughput
 * throttling.
 *
 * @author zznate
 */
public abstract class AbstractBatcher implements Batcher {
    protected BatchSubmitter batchSubmitter;

    private Batch batch;
    private final ReentrantLock submitLock = new ReentrantLock();
    private final AtomicLong opCount = new AtomicLong();
    private final Timer addTimer =
            Metrics.newTimer(AbstractBatcher.class, "add_invocation", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);
    private final Counter invocationCounter =
            Metrics.newCounter(AbstractBatcher.class, "batch_add_invocations");
    private final Counter existingCounterHit =
            Metrics.newCounter(AbstractBatcher.class,"counter_existed");

    
    
    public AbstractBatcher(int queueSize) {
      batch = new Batch();
    }

    public void setBatchSubmitter(BatchSubmitter batchSubmitter) {
        this.batchSubmitter = batchSubmitter;
    }

    /**
     * Individual {@link Count} for the same counter get rolled up, so
     * we track the individual number of operations.
     * @return the number of operation against this SimpleBatcher
     */
    public long getOpCount() {
        return opCount.get();
    }

    protected abstract boolean shouldSubmit(Batch batch);
    protected abstract void submit(Batch batch);

  /**
     * Add a count object to this batcher
     * @param count
     * @throws CounterProcessingUnavailableException
     */
    public void add(Count count) throws CounterProcessingUnavailableException {
      invocationCounter.inc();
      final TimerContext context = addTimer.time();


     
      batch.add(count);

      // If it's submit time and the lock is free, acquire the lock.
      // Submission of the batch should reset the child impl submit state.
      // Though multiple threads can return true on shouldSubmit, only one
      //  thread will pass the tryLock check and acquire the submitLock
      if (shouldSubmit(batch) && submitLock.tryLock()) {
        try {

          submit(copyAndClear(batch));
        } finally {
          // by this time, submit(copy) above will have reset the the
          // shouldSubmit condition
          submitLock.unlock();
        }
      }
      context.stop();

    }

  private Batch copyAndClear(Batch original) {
    Batch copy;
    synchronized(original) {
      copy = new Batch(original);
      original.clear();
    }
    return copy;
  }
    

    class Batch {
        private final Map<String,Count> counts;
        private final AtomicInteger localCallCounter = new AtomicInteger();

        Batch() {
            counts = new HashMap<String, Count>();
        }

        /* copy constructor */
        Batch(Batch batch) {
            localCallCounter.set(batch.localCallCounter.get());
            counts = new HashMap<String, Count>(batch.counts);
        }

        void clear() {
          counts.clear();
          localCallCounter.set(0);
        }

        void add(Count count) {
            opCount.incrementAndGet();
            localCallCounter.incrementAndGet();
            Count found = counts.get(count.getCounterName());
            if ( found != null ) {
                existingCounterHit.inc();
                counts.put(found.getCounterName(), found.apply(count));
            } else {
                counts.put(count.getCounterName(),count);
            }
        }

        /**
         * The number of distinct counters which have been seen
         * @return
         */
        public int getPayloadSize() {
            return counts.size();
        }

        public Collection<Count> getCounts() {
            return counts.values();
        }

        /**
         * The number of times the {@link #add(com.usergrid.count.common.Count)} method has been
         * invoked on this batch instance
         * @return
         */
        public int getLocalCallCount() {
            return localCallCounter.get();
        }

    }
}
