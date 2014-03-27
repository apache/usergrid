package org.apache.usergrid.persistence.graph.consistency;


import java.util.Collection;


/**
 * Used to fork lazy repair and other types of operations.
 */
public interface AsyncProcessor<T> {


    /**
     * The processor implementation is responsible for guaranteeing the events fire in the runtime environment. This
     * could be local or clustered, consult the documentation on the implementation.  Note that events published here
     * could possibly be double published if the operation reaches it's timeout before completion.  As a result, every
     * receiver of the event should operate in an idempotent way.  Note that the event will fire at a time >= the
     * timeout time. Firing immediately should not be assumed.
     *
     * @param event The event to be scheduled for verification
     * @param timeout The time in milliseconds we should wait before the event should fire
     */
    public AsynchronousMessage<T> setVerification( T event, long timeout );


    /**
     * Start processing the event immediately asynchronously.  In the event an exception is thrown, the
     * AsynchronousMessage should be re-tried. It is up to the implementer to commit the event so that it does not fire
     * again.  This should never throw exceptions.
     *
     * @param event The event to start
     */
    public void start( AsynchronousMessage<T> event );


    /**
     * Get all events that have passed their timeout
     *
     * @param maxCount The maximum count
     * @param timeout The timeout to set when retrieving these timeouts to ensure they aren't lost
     *
     * @return A collection of asynchronous messages that have passed their timeout.  This could be due to process
     *         failure node loss etc.  No assumptions regarding the state of the message should be assumed when they are
     *         returned.
     */
    public Collection<AsynchronousMessage<T>> getTimeouts( int maxCount, long timeout );

    /**
     * Add the error listener to the list of listeners
     */
    public void addErrorListener( ErrorListener<T> listener );

    /**
     * Add the listener to this instance
     */
    public void addListener( MessageListener<T, T> listener );

    /**
     * Add a complete listener that is invoked when the listener has been invoked
     */
    public void addCompleteListener( CompleteListener<T> listener );
}
