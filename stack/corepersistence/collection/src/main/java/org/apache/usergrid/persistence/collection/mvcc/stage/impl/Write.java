package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;


/**
 * This phase should execute the serialization to the data store.
 */
public class Write implements WriteStage {

    /**
     * Create a new stage with the current context
     */
    public Write( ){
    }


    @Override
    public void performStage(WriteContext context, final MvccEntity entity ) {



    }
}
