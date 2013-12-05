package org.apache.usergrid.persistence.collection.mvcc.stage.impl.write;


import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.Result;
import org.apache.usergrid.persistence.model.entity.Entity;


/** @author tnine */
public class EventStart extends CollectionEvent<Entity> {

    public EventStart( final EntityCollection context, final Entity data, final Result result ) {

        super( context, data, result );
    }
}
