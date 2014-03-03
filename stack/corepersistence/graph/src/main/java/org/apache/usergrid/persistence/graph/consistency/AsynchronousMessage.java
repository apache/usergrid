package org.apache.usergrid.persistence.graph.consistency;


/**
 * An interface for a timeout event
 */
public interface AsynchronousMessage<T> {

    /**
     * @return The event to fire when our timeout is reached
     */
    T getEvent();

    /**
     * @return The time in epoch millis the event will time out
     */
    long getTimeout();
}
