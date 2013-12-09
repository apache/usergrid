package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.apache.usergrid.persistence.collection.EntityCollection;


/** @author tnine */
public class IoEvent<T> {

    private EntityCollection context;

    private T event;


    public IoEvent( final EntityCollection context, final T event ) {
        this.context = context;
        this.event = event;
    }


    public EntityCollection getEntityCollection() {
        return context;
    }


    public T getEvent() {
        return event;
    }
}
