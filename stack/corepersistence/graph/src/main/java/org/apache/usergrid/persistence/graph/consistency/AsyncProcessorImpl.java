package org.apache.usergrid.persistence.graph.consistency;


import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * The implementation of asynchronous processing
 */
@Singleton
public class AsyncProcessorImpl implements AsyncProcessor {

    private final EventBus bus;
    private final TimeoutQueue queue;


    @Inject
    public AsyncProcessorImpl( final EventBus bus, final TimeoutQueue queue ) {
        this.bus = bus;
        this.queue = queue;
    }


    @Override
    public <T> TimeoutEvent<T> verify( final T event, final long timeout ) {
        return queue.queue( event, timeout  );
    }


    @Override
    public <T> void start( final T event ) {
        //TODO, wrap this in hystrix for timeouts and capacity
        bus.post( event );
    }
}
