package org.apache.usergrid.persistence.graph.consistency;


/**
 *  Used to fork lazy repair and other types of operations.  This can be implemented
 *  across multiple environments.
 *
 */
public interface AsyncProcessor {


    /**
     * The processor implementation is responsible for guaranteeing the events fire in the runtime environment.
     * This could be local or clustered, consult the documentation on the implementation.  Note that events published
     * here could possibly be double published if the operation reaches it's timeout before completion.  As a result, every
     * receiver of the event should operate in an idempotent way.
     *
     * @param event The event to be scheduled for verification
     * @param timeout  The epoch time in milliseconds the event should fire
     */
    public <T> TimeoutEvent<T> verify(T event, long timeout);


    /**
     * Start processing the event immediately asynchronously.
     *
     * @param event
     * @param <T>
     */
    public <T> void start(T event);




}
