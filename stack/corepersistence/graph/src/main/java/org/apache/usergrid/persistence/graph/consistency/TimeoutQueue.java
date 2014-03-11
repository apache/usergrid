package org.apache.usergrid.persistence.graph.consistency;


import java.util.Collection;


/**
 * Interface for implementations of a timeout queue.
 */
public interface TimeoutQueue {

    /**
     * Queue the event with the timeout provided
     *
     * @param event The event to queue
     * @param timeout The timeout to set on the queue element
     * @param <T> The type to return
     */
    public <T> TimeoutEvent<T> queue( T event, long timeout );


    /**
     * Take up to maxSize elements with a timeout <= the currentTime
     *
     * This implicitly re-schedules every taken operation at currentTime+timeout
     *
     * @param <T> The type to return
     *
     * @return A collection of events.
     */
    public <T> Collection<TimeoutEvent<T>> take( int maxSize, long currentTime, long timeout );


    /**
     * Remove this timeout event from the queue.
     *
     * @param event The event to remove
     *
     * @return True if the element was removed
     */
    public <T> boolean remove( TimeoutEvent<T> event );
}
