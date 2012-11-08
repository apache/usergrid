package org.usergrid.persistence.cassandra.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.utils.UUIDUtils;

/**
 * Keeps the TraceTag as a ThreadLocal
 * @author zznate
 */
public class TraceTagManager {
    private Logger logger = LoggerFactory.getLogger(TraceTagManager.class);

    private static ThreadLocal<TraceTag> localTraceTag = new ThreadLocal<TraceTag>();

    /**
     * Get the tag from a ThreadLocal. Will return null if no tag is attached.
     * @return
     */
    public TraceTag acquire() {
        return localTraceTag.get();
    }

    /**
     * Attache the tag to the current Thread
     * @param traceTag
     */
    public void attach(TraceTag traceTag) {
        // TODO throw illegal state exception if we have one already
        localTraceTag.set(traceTag);
        logger.debug("Attached TraceTag {} to thread", traceTag);
    }

    /**
     * Detach the tag from the current thread
     * @param traceTag
     */
    public void detach(TraceTag traceTag) {
        // TODO throw illegalstate if was not attached
        localTraceTag.remove();
        logger.debug("Detached TraceTag {} from thread", traceTag);
    }

    /**
     * Create a TraceTag
     * @return
     */
    public TraceTag create(String tagName) {
        return TraceTag.getInstance(UUIDUtils.newTimeUUID(), tagName);
    }
}
