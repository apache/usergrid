package org.apache.usergrid.persistence.collection.mvcc.stage.impl.write;


import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.Result;


/** @author tnine */
public class EventVerify extends CollectionEvent<MvccEntity> {


    public EventVerify( final EntityCollection context, final MvccEntity data, final Result result ) {
        super( context, data, result );
    }
}
