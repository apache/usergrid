package org.apache.usergrid.persistence.graph.consistency;


/**
 *
 *
 */
public interface AsynchronousEventListener<T> {


    /**
     * The handler to receive the event.  Any exception that is thrown is considered
     * a failure, and the event will be re-fired.
     * @param event
     */
    void receive(T event);

}
