package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;


/**
 * This is the first stage and should be invoked immediately when a write is started.  It should persist the start of a
 * new write in the data store for a checkpoint and recovery
 */
public class Start implements WriteStage {

    /**
     * Create a new stage with the current context
     */
    protected Start( ){
    }


    @Override
    public MvccEntity performStage( final MvccEntity entity ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
