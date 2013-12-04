package org.apache.usergrid.persistence.collection.mvcc.stage;


/**
 * Pipeline that represents a collection of stages to execute
 * An implementation is mutable, so this instance should not be
 * reused across threads
 *
 * @author tnine */
public interface StagePipeline {


    /**
     * Get the first stage in this pipeline.
     */
    Stage first();


    /**
     * get the next stage after the stage specified
     * @param stage The stage to seek in our pipeline
     */
    Stage nextStage(Stage stage);




}
