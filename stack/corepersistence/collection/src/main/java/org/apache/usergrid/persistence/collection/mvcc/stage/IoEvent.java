package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.apache.usergrid.persistence.collection.Scope;


/** @author tnine */
public class IoEvent<T> {

    private Scope context;

    private T event;


    public IoEvent( final Scope context, final T event ) {
        this.context = context;
        this.event = event;
    }


    public Scope getEntityCollection() {
        return context;
    }


    public T getEvent() {
        return event;
    }
}
