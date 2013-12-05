package org.apache.usergrid.persistence.collection.mvcc.entity.impl;


import org.apache.usergrid.persistence.collection.mvcc.entity.CollectionEventBus;

import com.google.common.eventbus.EventBus;


/** @author tnine */
public class CollectionEventBusImpl extends EventBus implements CollectionEventBus{

    public CollectionEventBusImpl() {
        super();
    }


    public CollectionEventBusImpl( final String identifier ) {
        super( identifier );
    }
}
