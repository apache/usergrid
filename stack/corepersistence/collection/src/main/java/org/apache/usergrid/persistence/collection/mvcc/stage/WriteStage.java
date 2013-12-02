package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/**
 * The possible stages in our write flow.
 */
public interface WriteStage {

    /**
     * Run this stage.  This will return the MvccEntity that should be returned or passed to the next stage
     *
     * @param entity The entity to use in this stage
     *
     * @return The asynchronous listener to signal success
     *
     */
    public MutationBatch performStage( MvccEntity entity ) throws ConnectionException;
}
