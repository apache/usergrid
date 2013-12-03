package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/**
 * The possible stages in our write flow.
 */
public interface WriteStage{

    /**
     * Run this stage.  This will return the MvccEntity that should be returned or passed to the next stage
     *
     *
     * @param context The context of the current write operation
     *
     * @return The asynchronous listener to signal success
     *
     */
    public void performStage( WriteContext context);


}
