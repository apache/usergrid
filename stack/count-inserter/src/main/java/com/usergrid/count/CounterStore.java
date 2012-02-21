package com.usergrid.count;

import com.usergrid.count.common.Count;

import java.util.Collection;
import java.util.List;

/**
 * @author zznate
 */
public interface CounterStore {
    // TODO consider inforcing Async via Future<T> as return type
    void save(Count count);

    void save(Collection<Count> counts);
}
