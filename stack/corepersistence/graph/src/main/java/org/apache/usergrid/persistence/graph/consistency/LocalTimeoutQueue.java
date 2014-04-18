package org.apache.usergrid.persistence.graph.consistency;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Simple implementation of our timeout queue using an in memory PriorityBlockingQueue.
 *
 * This SHOULD NOT be used in a production environment.  This is for development/testing runtimes only.
 * This should not be a singleton, we can have multiple instances of
 */
@Singleton
public class LocalTimeoutQueue<T extends Serializable> implements TimeoutQueue<T> {

    /**
     * For in memory queueing
     */
    private final PriorityBlockingQueue<AsynchronousMessage<T>> queue = new PriorityBlockingQueue<AsynchronousMessage<T>>( 1000, new TimeoutEventCompatator<T>() );

    private final TimeService timeService;


    @Inject
    public LocalTimeoutQueue( final TimeService timeService ) {
        this.timeService = timeService;
    }


    @Override
    public AsynchronousMessage<T> queue( final T event, final long timeout ) {
        final long scheduledTimeout = timeService.getCurrentTime() + timeout;
        final AsynchronousMessage<T> queuedEvent = new SimpleAsynchronousMessage<T>( event, scheduledTimeout );

        queue.add( queuedEvent );

        return queuedEvent;
    }


    @Override
    public Collection<AsynchronousMessage<T>> take( final int maxSize, final long timeout ) {

        final long now = timeService.getCurrentTime();
        final long newTimeout = now+timeout;

        List<AsynchronousMessage<T>> results = new ArrayList<AsynchronousMessage<T>>(maxSize);

        for(int i = 0; i < maxSize; i ++){

            AsynchronousMessage<T> queuedEvent = queue.peek();

            //nothing to do
            if(queuedEvent == null){
                break;
            }


            //not yet reached timeout
            if(queuedEvent.getTimeout() > now){
                break;
            }

            final AsynchronousMessage<T>
                    newEvent =  new SimpleAsynchronousMessage<T>( queuedEvent.getEvent(), newTimeout );

            //re schedule a new event to replace this one
            queue.add(newEvent);

            //we're re-added, remove the element
            queue.poll();

            results.add( newEvent );

        }

        return results;
    }


    @Override
    public boolean remove( final AsynchronousMessage<T> event ) {
        return queue.remove( event );
    }


    private static class TimeoutEventCompatator<T> implements Comparator<AsynchronousMessage<T>> {


        @Override
        public int compare( final AsynchronousMessage<T> o1, final AsynchronousMessage<T> o2 ) {
            return Long.compare( o1.getTimeout(), o2.getTimeout() );
        }
    }
}
