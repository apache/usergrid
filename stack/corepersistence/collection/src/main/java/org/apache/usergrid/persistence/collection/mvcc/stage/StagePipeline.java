package org.apache.usergrid.persistence.collection.mvcc.stage;


/**
 * Pipeline that represents a collection of stages to execute
 * An implementation is mutable, so this instance should not be
 * reused across threads
 *
 * @author tnine */
public interface StagePipeline {


    /**
     * Get the first stage in this pipeline.  Will return null if there are no more stages to execute
     */
    WriteStage first();


    /**
     * Insert a new stage directly after the current stage.  This can be used
     * to add additional validation during write phases depending on the mvcc entity
     *
     * @param stage
     */
    void insert(WriteStage stage);


    /**
     * Add a new stage to the end of the pipline
     * @param stage
     */
    void addLast(WriteStage stage);


    /**
     * get the next stage after this one
     * @param stage
     */
    WriteStage nextStage(WriteStage stage);


    /**
     * Get the last stage in this pipeline
     * @return
     */
    WriteStage last();

    /**
     * Get the current stage in the pipeline
     * @return
     */
    WriteStage current();


}
