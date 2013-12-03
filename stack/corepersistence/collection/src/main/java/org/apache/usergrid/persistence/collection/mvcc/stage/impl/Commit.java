package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/**
 * This phase should invoke any finalization, and mark the entity as committed in the data store before returning
 */
public class Commit implements WriteStage {

    public Commit(){

    }

    @Override
    public void performStage( final WriteContext context, final MvccEntity entity ) throws ConnectionException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
