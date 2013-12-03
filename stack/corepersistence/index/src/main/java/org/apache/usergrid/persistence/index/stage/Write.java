package org.apache.usergrid.persistence.index.stage;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/** This state should perform an update of the index. */
public class Write implements WriteStage
{


    @Override
    public void performStage( final WriteContext context, final MvccEntity entity ) throws ConnectionException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

