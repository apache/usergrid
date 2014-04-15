package org.apache.usergrid.persistence.graph.consistency;


/**
 * Internal listener for errors, really only used for testing.  Can be used to hook into error state
 */
public interface CompleteListener<T> {

    /**
     * Invoked when an event is complete
     * @param event
     */
    void onComplete( AsynchronousMessage<T> event );
}
