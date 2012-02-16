package com.usergrid.count;

import com.usergrid.count.common.Count;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fires up a single-threaded ScheduledExecutor which will invoke
 * {@link BatchSubmitter#submit(com.usergrid.count.AbstractBatcher.Batch)} every batchInterval
 * seconds.
 *
 * @author zznate
 */
public class ScheduledBatcher extends AbstractBatcher {

    private int batchInterval;
    private volatile long currentMillis;
    private AtomicLong batchSubmissionCount = new AtomicLong();

    /**
     * Initializes the scheduledExecutor with the interval (in seconds) at which this executor will fire
     * @param batchInterval
     */
    public ScheduledBatcher(int queueSize, int batchInterval) {
        super(queueSize);
        this.batchInterval = batchInterval;
        this.currentMillis = System.currentTimeMillis();
    }

    /**
     * This implementation is synchronized
     * @param batch
     * @return
     */
    protected boolean maybeSubmit(Batch batch) {
        long now = System.currentTimeMillis();
        if ( now > ((1000 * batchInterval) + currentMillis) ) {
            currentMillis = now;
            batchSubmitter.submit(batch);
            batchSubmissionCount.incrementAndGet();
            return true;
        }
        return false;
    }

    public long getBatchSubmissionCount() {
        return batchSubmissionCount.get();
    }
}
