package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;

import com.google.common.util.concurrent.FutureCallback;
import com.netflix.astyanax.connectionpool.OperationResult;


/** @author tnine */
public class WriteContextCallback implements FutureCallback<OperationResult<Void>> {

    private final WriteContext context;
    private final MvccEntity entity;


    public WriteContextCallback( final WriteContext context, final MvccEntity entity ) {
        this.context = context;
        this.entity = entity;
    }


    public void onSuccess( final OperationResult<Void> result ) {
        //proceed to the next stage
        context.nextStage( entity );
    }


    @Override
    public void onFailure( final Throwable t ) {
        throw new RuntimeException( "Failed to execute write", t );
    }
}
