package com.usergrid.count;

/**
 * @author zznate
 */
public interface BatchSubmitter {
    void submit(SimpleBatcher.Batch batch);
}
