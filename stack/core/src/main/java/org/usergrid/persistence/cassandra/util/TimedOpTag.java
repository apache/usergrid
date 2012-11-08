package org.usergrid.persistence.cassandra.util;

import org.usergrid.utils.UUIDUtils;

import java.util.UUID;

/**
 * Simple struct holding timer information for an operation and an
 * arbitrary tag for spanning 0 or more operations
 *
 * @author zznate
 */
public class TimedOpTag {

    private final UUID opTag;
    private final TraceTag traceTag;
    private String tagName;
    private long elapsed = 0;
    private boolean status;

    private TimedOpTag(TraceTag traceTag) {
        this.opTag = UUIDUtils.newTimeUUID();
        this.traceTag = traceTag;
    }

    /**
     * Get an instance with the current start timer set to 'now'
     * @param traceTag can be null for single op timing
     * @return
     */
    public static TimedOpTag instance(TraceTag traceTag) {
        return new TimedOpTag(traceTag);
    }


    /**
     * Apply tagName only if not already applied
     * @param tagName
     */
    public void stopAndApply(String tagName, boolean opStatus) {
        if (elapsed == 0) {
            // extract from uuid and calculate
            elapsed = System.currentTimeMillis() - UUIDUtils.getTimestampInMillis(opTag);
        }
        if ( tagName == null ) {
            this.tagName = tagName;
            this.status = opStatus;
        }

    }

    /**
     * Elapsed time of this op in milliseconds.
     * @return
     */
    public long getElapsed() {
        return elapsed;
    }

    /**
     * The start time of this operation as represented by the timestamp embedded in
     * the type-1 UUID of the opTag property
     * @return
     */
    public long getStart() {
        return UUIDUtils.getTimestampInMillis(opTag);
    }

    /**
     * The tag for this specific operation
     * @return
     */
    public UUID getOpTag() {
        return opTag;
    }

    /**
     * A tag which may span 0 or more operations
     * @return
     */
    public TraceTag getTraceTag() {
        return traceTag;
    }

    /**
     *
     * @return the tagName - null if never applied
     */
    public String getTagName() {
        return tagName;
    }


    /**
     *
     * @return whether or not the operation was 'successful'
     * Could still be false if {@link #stopAndApply(String, boolean)} was never called
     */
    public boolean getOpSuccessful() {
        return status;
    }
}
