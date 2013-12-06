package org.apache.usergrid.persistence.collection.mvcc.stage.impl.load;


import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.Result;
import org.apache.usergrid.persistence.model.entity.Id;


/** @author tnine */
public class EventLoad extends CollectionEvent<Id> {
    public EventLoad( final EntityCollection context, final Id data, final Result result ) {

        super( context, data, result );
    }
}
