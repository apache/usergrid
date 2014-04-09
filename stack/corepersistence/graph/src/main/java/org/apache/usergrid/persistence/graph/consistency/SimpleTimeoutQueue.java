package org.apache.usergrid.persistence.graph.consistency;


import java.util.Collection;

import com.amazonaws.services.sqs.AmazonSQSAsyncClient;


/**
 *
 *
 */
public class SimpleTimeoutQueue implements  TimeoutQueue {


    @Override
    public AsynchronousMessage queue( final Object event, final long timeout ) {
        AmazonSQSAsyncClient sqsAsyncClient;
        return null;
    }


    @Override
    public Collection<AsynchronousMessage> take( final int maxSize, final long timeout ) {
        return null;
    }


    @Override
    public boolean remove( final AsynchronousMessage event ) {
        return false;
    }
}
