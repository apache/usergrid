package org.apache.usergrid.persistence.graph.consistency;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import com.google.inject.Inject;


/**
 * Simple implementation of our timeout queue using an in memory PriorityBlockingQueue.
 *
 * This SHOULD NOT be used in a production environment.  This is for development/testing runtimes only.
 */
public class LocalTimeoutQueue<T> implements TimeoutQueue<T> {

    /**
     * For in memory queueing
     */
    private final PriorityBlockingQueue<TimeoutEvent<T>> queue = new PriorityBlockingQueue<TimeoutEvent<T>>( 1000, new TimeoutEventCompatator<T>() );

    private final TimeService timeService;


    @Inject
    public LocalTimeoutQueue( final TimeService timeService ) {
        this.timeService = timeService;
    }


    @Override
    public TimeoutEvent<T> queue( final T event, final long timeout ) {
        final long scheduledTimeout = timeService.getCurrentTime() + timeout;
        final TimeoutEvent<T> queuedEvent = new SimpleTimeoutEvent<T>( event, scheduledTimeout );

        queue.add( queuedEvent );

        return queuedEvent;
    }


    @Override
    public Collection<TimeoutEvent<T>> take( final int maxSize, final long timeout ) {

        final long now = timeService.getCurrentTime();
        final long newTimeout = now+timeout;

        List<TimeoutEvent<T>> results = new ArrayList<TimeoutEvent<T>>(maxSize);

        for(int i = 0; i < maxSize; i ++){

            TimeoutEvent<T> queuedEvent = queue.peek();

            //nothing to do
            if(queuedEvent == null){
                break;
            }


            //not yet reached timeout
            if(queuedEvent.getTimeout() > now){
                break;
            }

            final TimeoutEvent<T> newEvent =  new SimpleTimeoutEvent<T>( queuedEvent.getEvent(), newTimeout );

            //re schedule a new event to replace this one
            queue.add(newEvent);

            //we're re-added, remove the element
            queue.poll();

            results.add( newEvent );

        }

        return results;
    }


    @Override
    public boolean remove( final TimeoutEvent<T> event ) {
        return queue.remove( event );
    }


    private static class TimeoutEventCompatator<T> implements Comparator<TimeoutEvent<T>> {


        @Override
        public int compare( final TimeoutEvent<T> o1, final TimeoutEvent<T> o2 ) {
            return Long.compare( o1.getTimeout(), o2.getTimeout() );
        }
    }
}
