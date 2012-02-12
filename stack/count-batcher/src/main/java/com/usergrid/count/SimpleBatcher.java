package com.usergrid.count;

import com.usergrid.count.common.Count;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple Batcher implementation that keeps a sum of the
 * number of {@link Count} operations which have been applied.
 * Counters are aggregated by name.
 *
 * @author zznate
 */
public class SimpleBatcher extends AbstractBatcher {

    private int batchSize = 500;
    private AtomicLong batchSubmissionCount = new AtomicLong();

    public SimpleBatcher(int queueSize) {
        super(queueSize);
    }

    /**
     * Submit the batch if we have more than batchSize insertions
     *
     */
    protected boolean maybeSubmit(Batch batch) {
        if ( batch.getPayloadSize() >= batchSize ) {
            batchSubmitter.submit(batch);
            batchSubmissionCount.incrementAndGet();
            return true;
        }
        return false;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getBatchSubmissionCount() {
        return batchSubmissionCount.get();
    }

}
