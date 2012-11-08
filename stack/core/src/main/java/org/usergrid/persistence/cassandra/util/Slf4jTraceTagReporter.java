package org.usergrid.persistence.cassandra.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Simple reporter which dumps to class logger at info level
 * @author zznate
 */
public class Slf4jTraceTagReporter implements TraceTagReporter {
    private Logger logger = LoggerFactory.getLogger(Slf4jTraceTagReporter.class);

    @Override
    public void report(TimedOpTag timedOpTag) {
        logger.info("TraceTag: {} opId: {} opName: {} startTime: {} elapsed: {}",
                new Object[]{
                        timedOpTag.getTraceTag().get(),
                        timedOpTag.getOpTag(),
                        timedOpTag.getTagName(),
                        new Date(timedOpTag.getStart()),
                        timedOpTag.getElapsed()
                });
    }
}
