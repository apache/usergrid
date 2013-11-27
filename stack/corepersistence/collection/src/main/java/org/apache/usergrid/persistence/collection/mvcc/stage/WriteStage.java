package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;


/**
 * The possible stages in our write flow.
 */
public interface WriteStage {

    /**
     * Run this stage.  This will return the MvccEntity that should be returned or passed to the next stage
     * @param entity The entity to use in this stage
     *
     * @return The MvccEntity to use for the next sgage
     *
     */
    public MvccEntity performStage( MvccEntity entity);
}
