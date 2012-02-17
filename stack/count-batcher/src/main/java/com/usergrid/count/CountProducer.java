package com.usergrid.count;

import com.usergrid.count.common.Count;

/**
 * @author zznate
 */
public interface CountProducer {
    void send(Count count);
}
