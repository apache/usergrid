package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import java.util.Arrays;
import java.util.List;

import org.apache.usergrid.persistence.collection.mvcc.stage.ExecutionStage;
import org.apache.usergrid.persistence.collection.mvcc.stage.StagePipeline;

import com.google.common.base.Preconditions;


/** @author tnine */
public class StagePipelineImpl implements StagePipeline {

    private final List<ExecutionStage> executionStages;


    protected StagePipelineImpl( List<ExecutionStage> executionStages ) {
        Preconditions.checkNotNull( executionStages, "executionStages is required");
        Preconditions.checkArgument(  executionStages.size() > 0, "executionStages must have more than 1 element" );

        this.executionStages = executionStages;
    }


    @Override
    public ExecutionStage first() {

        if ( executionStages.size() == 0 ) {
            return null;
        }

        return executionStages.get( 0 );
    }





    @Override
    public ExecutionStage nextStage( final ExecutionStage executionStage ) {

        Preconditions.checkNotNull( executionStage, "ExecutionStage cannot be null" );

        int index = executionStages.indexOf( executionStage );

        //we're done, do nothing
        if ( index == executionStages.size() -1  ) {
            return null;
        }

        return  executionStages.get( index + 1 );
    }


    /** Factory to create a new instance. */
    public static StagePipelineImpl fromStages( ExecutionStage... executionStages ) {
        return new StagePipelineImpl(Arrays.asList( executionStages ));
    }
}
