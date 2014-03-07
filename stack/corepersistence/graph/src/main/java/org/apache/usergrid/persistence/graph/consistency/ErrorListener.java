package org.apache.usergrid.persistence.graph.consistency;


/**
 * Internal listener for errors, really only used for testing.  Can be used to hook into error state
 */
public interface ErrorListener <T> {

    /**
     * Invoked when an error occurs during asynchronous processing
     * @param event
     * @param t
     */
    void onError( AsynchronousMessage<T> event, Throwable t );
}
