package com.usergrid.count;

import java.util.concurrent.Future;

/**
 * @author zznate
 */
public interface BatchSubmitter {
    Future<?> submit(SimpleBatcher.Batch batch);
    void shutdown();
}
