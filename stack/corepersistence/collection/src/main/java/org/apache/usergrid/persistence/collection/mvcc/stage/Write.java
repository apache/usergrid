package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;


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
    public MvccEntity performStage( final MvccEntity entity) {


        return entity;
    }
}
