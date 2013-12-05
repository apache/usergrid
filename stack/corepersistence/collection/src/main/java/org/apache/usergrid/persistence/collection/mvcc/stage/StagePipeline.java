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
    ExecutionStage first();


    /**
     * get the next executionStage after the executionStage specified
     * @param executionStage The executionStage to seek in our pipeline
     */
    ExecutionStage nextStage(ExecutionStage executionStage );




}
