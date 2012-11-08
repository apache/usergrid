package org.usergrid.persistence.cassandra.util;

/**
 * @author zznate
 */
public interface TraceTagReporter {

    void report(TraceTag traceTag);

}
