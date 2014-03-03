package org.apache.usergrid.persistence.graph.consistency;


/**
 *
 *
 */
public class SimpleAsynchronousMessage<T> implements AsynchronousMessage<T> {

    private final T event;
    private final long timeout;


    public SimpleAsynchronousMessage( final T event, final long timeout ) {
        this.event = event;
        this.timeout = timeout;
    }


    @Override
    public T getEvent() {
       return event;
    }


    @Override
    public long getTimeout() {
        return timeout;
    }
}
