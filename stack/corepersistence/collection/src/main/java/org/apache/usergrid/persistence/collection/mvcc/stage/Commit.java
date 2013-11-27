package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;


/**
 * This phase should invoke any finalization, and mark the entity as committed in the data store before returning
 */
public class Commit implements WriteStage {



    @Override
    public MvccEntity performStage( final MvccEntity entity ) {
        return entity;
    }
}
