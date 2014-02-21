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
     * receiver of the event should operate in an idempotent way.  Note that the event will fire at a time >= the timeout time.
     * Firing immediately should not be assumed.
     *
     * @param event The event to be scheduled for verification
     * @param timeout  The time in milliseconds we should wait before the event should fire
     */
    public <T> TimeoutEvent<T> setVerification( T event, long timeout );


    /**
     * Start processing the event immediately asynchronously.  In the event an exception is thrown, the TimeoutEvent should be re-tried.
     * It is up to the implementer to commit the event so that it does not fire again.  This should never throw exceptions.
     *
     * @param event The event to start
     * @param <T>
     */
    public <T> void start(TimeoutEvent<T> event);




}
