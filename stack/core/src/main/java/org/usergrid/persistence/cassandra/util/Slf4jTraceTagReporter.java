package org.usergrid.persistence.cassandra.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Simple reporter which dumps to class logger at info level
 * @author zznate
 */
public class Slf4jTraceTagReporter implements TraceTagReporter {
    private Logger logger;

    public Slf4jTraceTagReporter() {
        logger = LoggerFactory.getLogger("TraceTagReporter");
        if ( logger == null ) {
            logger = LoggerFactory.getLogger(Slf4jTraceTagReporter.class);
        }
    }

    @Override
    public void report(TraceTag traceTag) {
        logger.info("TraceTag: {}", traceTag.getTraceName());
        for ( TimedOpTag timedOpTag : traceTag ) {
            logger.info("----opId: {} opName: {} startTime: {} elapsed: {}",
                            new Object[]{
                                    timedOpTag.getOpTag(),
                                    timedOpTag.getTagName(),
                                    new Date(timedOpTag.getStart()),
                                    timedOpTag.getElapsed()
                            });
        }
        logger.info("------");

    }
}
