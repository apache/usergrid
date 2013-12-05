package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.apache.usergrid.persistence.collection.CollectionContext;

import com.google.common.base.Preconditions;


/** @author tnine */
public abstract class CollectionEvent<T> {

    private final CollectionContext context;
    private final T data;
    private final Result result;


    protected CollectionEvent( final CollectionContext context, final T data, final Result result ) {
        Preconditions.checkNotNull( context, "context is required" );
        Preconditions.checkNotNull( data, "context is required" );
        Preconditions.checkNotNull( context, "context is required" );
        this.context = context;
        this.data = data;
        this.result = result;
    }




    /** Get the collection context for this event */
    public CollectionContext getCollectionContext() {
        return this.context;
    }


    public T getData() {
        return data;
    }


    public Result getResult() {
        return result;
    }
}
