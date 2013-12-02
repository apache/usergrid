package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;


/**
 * This phase should invoke any finalization, and mark the entity as committed in the data store before returning
 */
public class Commit implements WriteStage {



    @Override
    public MutationBatch performStage( final MvccEntity entity ) {
        return null;
    }
}
