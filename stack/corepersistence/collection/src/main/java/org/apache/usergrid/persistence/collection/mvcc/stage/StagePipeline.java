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
     * get the next stage after this one
     * @param stage
     */
    WriteStage nextStage(WriteStage stage);


    /**
     * Get the current stage in the pipeline
     * @return
     */
    WriteStage current();


}
