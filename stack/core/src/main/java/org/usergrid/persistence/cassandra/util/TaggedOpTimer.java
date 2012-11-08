package org.usergrid.persistence.cassandra.util;

import me.prettyprint.cassandra.connection.HOpTimer;

/**
 * Trace the timed execution of a 'tag' over the course of a number of operations.
 * Facilitates integration with Dapper-style trace logging infrastructure.
 *
 * @author zznate
 */
public class TaggedOpTimer implements HOpTimer {

    private TraceTagManager traceTagManager;
    private TraceTagReporter traceTagReporter;

    public TaggedOpTimer(TraceTagManager traceTagManager,
                         TraceTagReporter traceTagReporter) {
        this.traceTagManager = traceTagManager;
        this.traceTagReporter = traceTagReporter;
    }

    @Override
    public Object start() {
        // look for our threadLocal. if not present, return this.
        TraceTag tag = traceTagManager.acquire();
        // if present, start timer and attach
        return TimedOpTag.instance(tag);
    }

    @Override
    public void stop(Object timedOpTag, String opTagName, boolean success) {
        if ( timedOpTag instanceof TimedOpTag ) {
            TimedOpTag t = ((TimedOpTag)timedOpTag);
            t.stopAndApply(opTagName, success);
            traceTagReporter.report(t);
        }

    }


}
