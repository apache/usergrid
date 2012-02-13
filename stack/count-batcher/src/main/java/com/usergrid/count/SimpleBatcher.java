package com.usergrid.count;

import com.usergrid.count.common.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple Batcher implementation that keeps a sum of the
 * number of {@link Count} operations which have been applied.
 * Counters are aggregated by name.
 *
 * @author zznate
 */
public class SimpleBatcher extends AbstractBatcher {
    private Logger log = LoggerFactory.getLogger(SimpleBatcher.class);
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
        int localCallCount = batch.getLocalCallCount();
        if ( localCallCount > 0 && localCallCount % batchSize == 0 ) {
            log.info("submit triggered...");
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
