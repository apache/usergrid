package org.apache.usergrid.persistence.index.stage;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;


/** This state should signal an index update has started */
public class Start implements WriteStage
{

    @Override
    public MutationBatch performStage( final MvccEntity entity ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
