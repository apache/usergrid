package org.apache.usergrid.perftest;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A performance test that will be run.
 */
public interface Perftest {
    @JsonProperty
    long getCallCount();

    @JsonProperty
    int getThreadCount();

    @JsonProperty
    long getDelayBetweenCalls();

    void call();
}
