package com.usergrid.count;

import com.usergrid.count.common.Count;

/**
 * A simple Batcher implementation that keeps a sum of the
 * number of {@link Count} operations which have been applied.
 * Counters are aggregated by name.
 *
 * @author zznate
 */
public class SimpleBatcher extends AbstractBatcher {


    private int batchSize = 500;

    /**
     * Apply the provided {@link Count} to the current batch, submitting the
     * batch if we have more than batchSize insertions
     * @param c
     */
    public void add(Count c) {
        if ( batch == null ) {
            batch = new Batch();
        }
        batch.add(c);
        if ( getPayloadSize() >= batchSize ) {
            batchSubmitter.submit(batch);
            batch = new Batch();
        }

    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }


}
