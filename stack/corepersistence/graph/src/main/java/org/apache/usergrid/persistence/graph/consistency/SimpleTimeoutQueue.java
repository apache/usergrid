package org.apache.usergrid.persistence.graph.consistency;


import java.util.Collection;

import com.amazonaws.services.sqs.AmazonSQSClient;


/**
 *
 *
 */
public class SimpleTimeoutQueue implements  TimeoutQueue {
    @Override
    public <T> TimeoutEvent<T> queue( final T event, final long timeout ) {
        /**
         * List of steps it seems like I need to take:
         * 1.) Create a queue
         * 2.) Queue the event with the timeout
         * return a element that is the queue?
         */
        AmazonSQSClient sqsClient;
        return null;
    }


    @Override
    public <T> Collection<TimeoutEvent<T>> take( final int maxSize, final long currentTime, final long timeout ) {
        return null;
    }


    @Override
    public <T> boolean remove( final TimeoutEvent<T> event ) {
        return false;
    }
}
