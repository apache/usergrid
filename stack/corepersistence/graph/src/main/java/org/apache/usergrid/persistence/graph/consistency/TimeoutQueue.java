package org.apache.usergrid.persistence.graph.consistency;


import java.util.Collection;


/**
 * Interface for implementations of a timeout queue.
 */
public interface TimeoutQueue<T> {

    /**
     * Queue the event with the timeout provided
     *
     * @param event The event to queue
     * @param timeout The timeout to set on the queue element
     * @return The AsynchronousEvent that has been queued
     */
    public AsynchronousEvent<T> queue( T event, long timeout );


    /**
     * Take up to maxSize elements with a timeout <= the currentTime
     *
     * This implicitly re-schedules every taken operation at currentTime+timeout
     *
     * @param  maxSize The maximum number of elements to take
     * @param timeout The timeout to set when taking the elements from the Q
     *
     * @return A collection of events.
     */
    public Collection<AsynchronousEvent<T>> take( int maxSize, long timeout );


    /**
     * Remove this timeout event from the queue.
     *
     * @param event The event to remove
     *
     * @return True if the element was removed.  False otherwise
     */
    public boolean remove( AsynchronousEvent<T> event );
}
