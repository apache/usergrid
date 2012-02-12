package com.usergrid.count;

import com.usergrid.count.common.Count;

/**
 * Defines minimal set of batch submission operations
 * @author zznate
 */
public interface Batcher {
    void setBatchSubmitter(BatchSubmitter batchSubmitter);
    void add(Count count);
    long getOpCount();
    long getBatchSubmissionCount();
}
