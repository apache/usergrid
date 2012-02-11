package com.usergrid.count;

import com.usergrid.count.common.Count;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Fires up a single-threaded ScheduledExecutor which will invoke
 * {@link BatchSubmitter#submit(com.usergrid.count.AbstractBatcher.Batch)} every batchInterval
 * seconds.
 *
 * @author zznate
 */
public class ScheduledBatcher extends AbstractBatcher {

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private int batchInterval;

    /**
     * Initializes the scheduledExecutor with the interval (in seconds) at which this executor will fire
     * @param batchInterval
     */
    public ScheduledBatcher(int batchInterval) {
        this.batchInterval = batchInterval;
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if ( batch != null ) {
                    batchSubmitter.submit(batch);
                    batch = new Batch();
                }
            }
        }, this.batchInterval, this.batchInterval, TimeUnit.SECONDS);
    }

    @Override
    public void add(Count count) {
        if ( batch == null ) {
            batch = new Batch();
        }
        batch.add(count);
    }

    public void shutdown() {
        this.executor.shutdown();
        this.executor.shutdownNow();
    }

}
