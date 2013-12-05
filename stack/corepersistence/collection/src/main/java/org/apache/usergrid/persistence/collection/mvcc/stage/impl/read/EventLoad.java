package org.apache.usergrid.persistence.collection.mvcc.stage.impl.read;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.Result;


/** @author tnine */
public class EventLoad extends CollectionEvent<UUID> {
    public EventLoad( final EntityCollection context, final UUID data, final Result result ) {

        super( context, data, result );
    }
}
