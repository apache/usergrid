package com.usergrid.count;

import com.usergrid.count.common.Count;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.annotation.Metered;
import com.yammer.metrics.annotation.Timed;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base batcher implementation, handles concurrency and locking throughput
 * throttling.
 *
 * @author zznate
 */
public abstract class AbstractBatcher implements Batcher {
    protected BatchSubmitter batchSubmitter;

    private int queueSize;

    private ArrayBlockingQueue<Batch> queue;
    private final AtomicLong opCount = new AtomicLong();
    private final Timer addTimer =
            Metrics.newTimer(AbstractBatcher.class, "add_invocation", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);
    private final Counter invocationCounter =
            Metrics.newCounter(AbstractBatcher.class, "batch_add_invocations");
    private final Counter existingCounterHit =
            Metrics.newCounter(AbstractBatcher.class,"counter_existed");

    public AbstractBatcher(int queueSize) {
        this.queueSize = queueSize;
        queue = new ArrayBlockingQueue<Batch>(queueSize, true);
        while ( queue.size() < queueSize ) {
            queue.add(new Batch());
        }
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

    protected abstract boolean maybeSubmit(Batch batch);

    /**
     * Add a count object to this batcher
     * @param count
     * @throws CounterProcessingUnavailableException
     */
    public void add(Count count) throws CounterProcessingUnavailableException {
        invocationCounter.inc();
        final TimerContext context = addTimer.time();
        Batch batch = null;
        try {
            batch = queue.poll(100L, TimeUnit.MILLISECONDS);
            batch.add(count);

            boolean wasSubmitted = maybeSubmit(batch);
            if ( wasSubmitted ) {
                queue.offer(new Batch());
            } else {
                queue.offer(batch);
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } finally {
            context.stop();
            if ( batch == null ) {
                throw new CounterProcessingUnavailableException("Timed out polling for available batch");
            }
        }



    }

    class Batch {
        private final Map<String,Count> counts;
        private final AtomicInteger localCallCounter = new AtomicInteger();

        Batch() {
            counts = new HashMap<String, Count>();
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
