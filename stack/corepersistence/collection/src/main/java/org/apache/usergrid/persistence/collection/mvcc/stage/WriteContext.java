package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;


/** @author tnine */
public interface WriteContext {


    /**
     * Get the stage pipeline for this write context
     * @return
     */
    StagePipeline getStagePipeline();

    /**
     * Perform the write in the context with the specified entity
     * @param entity
     */
    void nextStage(MvccEntity entity);



}
