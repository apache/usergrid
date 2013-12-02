package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;


/**
 * This phase should execute the serialization to the data store.
 */
public class Write implements WriteStage {

    /**
     * Create a new stage with the current context
     */
    protected Write( ){
    }


    @Override
    public MutationBatch performStage( final MvccEntity entity ) {


        return null;
    }
}
