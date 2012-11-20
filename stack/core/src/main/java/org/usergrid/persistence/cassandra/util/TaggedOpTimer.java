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

    public TaggedOpTimer(TraceTagManager traceTagManager) {
        this.traceTagManager = traceTagManager;
    }

    @Override
    public Object start() {
        // look for our threadLocal. if not present, return this.
        return traceTagManager.timerInstance();
    }

    @Override
    public void stop(Object timedOpTag, String opTagName, boolean success) {
        if ( timedOpTag instanceof TimedOpTag ) {
            TimedOpTag t = ((TimedOpTag)timedOpTag);
            t.stopAndApply(opTagName, success);
            traceTagManager.addTimer(t);
        }
    }


}
