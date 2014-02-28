package org.apache.usergrid.persistence.graph.consistency;


import rx.Observable;


/**
 *
 *
 */
public interface AsynchronousEventListener<T, R> {


    /**
     * The handler to receive the event.  Any exception that is thrown is considered
     * a failure, and the event will be re-fired.
     * @param event  The input event
     * @return The observable that performs the operations
     */
    Observable<R> receive(T event);

}
