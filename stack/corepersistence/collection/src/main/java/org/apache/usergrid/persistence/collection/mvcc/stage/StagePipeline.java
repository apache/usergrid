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
    EventStage first();


    /**
     * get the next eventStage after the eventStage specified
     * @param eventStage The eventStage to seek in our pipeline
     */
    EventStage nextStage(EventStage eventStage );




}
