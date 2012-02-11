package com.usergrid.count;

import com.usergrid.count.common.Count;

/**
 * @author zznate
 */
public interface CounterStore {
    // TODO consider inforcing Async via Future<T> as return type
    void save(Count count);
}
