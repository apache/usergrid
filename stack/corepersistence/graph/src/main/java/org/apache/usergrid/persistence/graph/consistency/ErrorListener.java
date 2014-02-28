package org.apache.usergrid.persistence.graph.consistency;


/**
 * Internal listener for errors, really only used for testing.  Can be used to hook into error state
 */
public interface ErrorListener <T> {

    void onError( AsynchronousEvent<T> event, Throwable t );
}
