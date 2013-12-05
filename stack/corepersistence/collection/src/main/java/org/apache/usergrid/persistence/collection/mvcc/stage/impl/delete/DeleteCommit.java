package org.apache.usergrid.persistence.collection.mvcc.stage.impl.delete;


import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.Result;


/** @author tnine */
public class DeleteCommit extends CollectionEvent<MvccEntity> {
    public DeleteCommit( final EntityCollection context, final MvccEntity data, final Result result ) {

        super( context, data, result );
    }
}
