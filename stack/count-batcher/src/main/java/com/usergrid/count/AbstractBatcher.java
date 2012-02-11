package com.usergrid.count;

import com.usergrid.count.common.Count;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zznate
 */
public abstract class AbstractBatcher implements Batcher {
    protected BatchSubmitter batchSubmitter;
    protected Batch batch;

    public void setBatchSubmitter(BatchSubmitter batchSubmitter) {
        this.batchSubmitter = batchSubmitter;
    }

    /**
     * Individual {@link Count} for the same counter get rolled up, so
     * we track the individual number of operations.
     * @return the number of operation against this SimpleBatcher
     */
    public int getOpCount() {
        return batch.opCount.get();
    }


    /**
     * The number of distinct counters which have been seen
     * @return
     */
    public int getPayloadSize() {
        return batch.counts.size();
    }

    // need a concept of mutex to lock on re-create
    static class Batch {
        private final Map<String,Count> counts;
        protected AtomicInteger opCount = new AtomicInteger(0);

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

        public Collection<Count> getCounts() {
            return counts.values();
        }


    }
}
