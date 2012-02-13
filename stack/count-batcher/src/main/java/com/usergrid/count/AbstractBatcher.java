package com.usergrid.count;

import com.usergrid.count.common.Count;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zznate
 */
public abstract class AbstractBatcher implements Batcher {
    protected BatchSubmitter batchSubmitter;

    private int queueSize;

    private ArrayBlockingQueue<Batch> queue;
    protected AtomicLong opCount = new AtomicLong(0);

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

    public void add(Count count) throws CounterProcessingUnavailableException {
        Batch batch = null;
        try {
            batch = queue.poll(100L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } finally {
            if ( batch == null ) {
                throw new CounterProcessingUnavailableException("Timed out polling for available batch");
            }
        }

        batch.add(count);

        boolean wasSubmitted = maybeSubmit(batch);
        if ( wasSubmitted ) {
            queue.offer(new Batch());
        } else {
            queue.offer(batch);
        }

    }

    // need a concept of mutex to lock on re-create
    class Batch {
        private final Map<String,Count> counts;


        Batch() {
            counts = new HashMap<String, Count>();
        }

        void add(Count count) {
            opCount.incrementAndGet();
            Count found = counts.get(count.getCounterName());
            if ( found != null ) {
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


    }
}
