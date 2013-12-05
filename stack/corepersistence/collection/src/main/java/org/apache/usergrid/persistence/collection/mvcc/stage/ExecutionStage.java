package org.apache.usergrid.persistence.collection.mvcc.stage;


/** The possible stages in our write flow. */
public interface ExecutionStage {

    /**
     * Run this stage.  This will return the MvccEntity that should be returned or passed to the next stage
     *
     * @param context The context of the current write operation
     *
     * @return The asynchronous listener to signal success
     */
    public void performStage( ExecutionContext context );
}
