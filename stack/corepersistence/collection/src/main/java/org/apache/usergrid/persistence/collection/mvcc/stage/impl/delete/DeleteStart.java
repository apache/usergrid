package org.apache.usergrid.persistence.collection.mvcc.stage.impl.delete;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.Result;
import org.apache.usergrid.persistence.model.entity.Id;


/** @author tnine */
public class DeleteStart extends CollectionEvent<Id> {

    public  DeleteStart( final EntityCollection context, final Id id, final Result result ) {
        super( context, id, result );
    }
}
