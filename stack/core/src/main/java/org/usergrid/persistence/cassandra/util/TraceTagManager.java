package org.usergrid.persistence.cassandra.util;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.utils.UUIDUtils;

import javax.annotation.Resource;

/**
 * Keeps the TraceTag as a ThreadLocal
 * @author zznate
 */
public class TraceTagManager {
    private Logger logger = LoggerFactory.getLogger(TraceTagManager.class);

    private static ThreadLocal<TraceTag> localTraceTag = new ThreadLocal<TraceTag>();

    private boolean traceEnabled;

    private boolean reportUnattached;

    private boolean explicitOnly;

    private int flushAtOpCount = 100;

    @Resource
    private TraceTagReporter traceTagReporter;

    /**
     * Enable tracing. Off by default.
     * @param traceEnabled
     */
    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    public boolean getTraceEnabled() {
        return traceEnabled;
    }

    /**
     * The maximum number of o TimedOpTag objects we can attach to
     * a tracing instance. Excess of this will for a blocking flush on the
     * current thread to the configured reporter instance.
     *
     * The default is 100. If you have other ThreadLocal variables, you should
     * probably lower this value.
     *
     * @return
     */
    public int getFlushAtOpCount() {
        return flushAtOpCount;
    }

    public void setFlushAtOpCount(int flushAtOpCount) {
        this.flushAtOpCount = flushAtOpCount;
    }

    /**
     * If set to true we log all TimedOpTag objects not attached to a Trace
     * @param reportUnattached
     */
    public void setReportUnattached(boolean reportUnattached) {
        this.reportUnattached = reportUnattached;
    }

    /**
     * Allow for/check against traces in piecemeal. Use this when
     * {@link #setTraceEnabled(boolean)} is set to false and you want callers
     * to control whether or not to initiate a trace. An example would be
     * initiating traces in a ServletFilter by looking for a header or parameter
     * as tracing all requests would be expensive.
     * @return
     */
    public boolean getExplicitOnly() {
        return explicitOnly;
    }

    public void setExplicitOnly(boolean explicitOnly) {
        this.explicitOnly = explicitOnly;
    }

    /**
     * Get the tag from a ThreadLocal. Will return null if no tag is attached.
     * @return
     */
    public TraceTag acquire() {
        return localTraceTag.get();
    }

    public TimedOpTag timerInstance() {
        return TimedOpTag.instance(acquire());
    }

    /**
     * Add this TimedOpTag to the underlying trace if there is one. Optionally
     * log it's contents if no trace is active.
     * If an active trace was found and {@link org.usergrid.persistence.cassandra.util.TraceTag#getOpCount()}
     * exceeded {@link #getFlushAtOpCount()}, then the trace is dumped to the reporter
     * and {@link org.usergrid.persistence.cassandra.util.TraceTag#removeOps()} is invoked. The TraceTag
     * stay attached with the same name and ID, but now with no pending ops.
     *
     * @param timedOpTag
     */
    public void addTimer(TimedOpTag timedOpTag) {
        if ( isActive() ) {
            TraceTag tag = acquire();
            if ( tag.getOpCount() >= flushAtOpCount ) {
                traceTagReporter.report(tag);
                tag.removeOps();
            }
            tag.add(timedOpTag);

            // if TraceTag#metered, send to meter by tag name
        } else {
            if (reportUnattached) {
                traceTagReporter.reportUnattached(timedOpTag);
            }
        }
    }

    /**
     * Returns true if there is a trace in progress
     * @return
     */
    public boolean isActive() {
        return acquire() != null;
    }

    /**
     * Attache the tag to the current Thread. Will throw an IllegalStateException
     * if there is already a trace in progress.
     * @param traceTag
     */
    public void attach(TraceTag traceTag) {
        Preconditions.checkState(!isActive(), "Attempt to attach on already active trace");
        localTraceTag.set(traceTag);
        logger.debug("Attached TraceTag {} to thread", traceTag);
    }

    /**
     * Detach the tag from the current thread. Throws an IllegalStateException
     * if there is no trace in progress.
     */
    public TraceTag detach() {
        TraceTag traceTag = localTraceTag.get();
        Preconditions.checkState(isActive(),"Attempt to detach on no active trace");
        localTraceTag.remove();
        logger.debug("Detached TraceTag {} from thread", traceTag);
        return traceTag;
    }

    /**
     * Create a TraceTag
     * @return
     */
    public TraceTag create(String tagName) {
        return TraceTag.getInstance(UUIDUtils.newTimeUUID(), tagName);
    }

    public TraceTag createMetered(String tagName) {
        return TraceTag.getMeteredInstance(UUIDUtils.newTimeUUID(), tagName);
    }
}
