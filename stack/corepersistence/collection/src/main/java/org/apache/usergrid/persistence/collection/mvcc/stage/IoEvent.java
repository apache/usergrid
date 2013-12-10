package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.apache.usergrid.persistence.collection.CollectionScope;


/** @author tnine */
public class IoEvent<T> {

    private CollectionScope context;

    private T event;


    public IoEvent( final CollectionScope context, final T event ) {
        this.context = context;
        this.event = event;
    }


    public CollectionScope getEntityCollection() {
        return context;
    }


    public T getEvent() {
        return event;
    }
}
