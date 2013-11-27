package org.apache.usergrid.perftest;

/**
 * A performance test that will be run.
 */
public interface Perftest {
    int getCallCount();
    int getThreadCount();
    int getDelayBetweenCalls();
    void call();
}
