package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import java.util.Arrays;
import java.util.List;

import org.apache.usergrid.persistence.collection.mvcc.stage.Stage;
import org.apache.usergrid.persistence.collection.mvcc.stage.StagePipeline;

import com.google.common.base.Preconditions;


/** @author tnine */
public class StagePipelineImpl implements StagePipeline {

    private final List<Stage> stages;


    protected StagePipelineImpl( List<Stage> stages ) {
        Preconditions.checkNotNull(stages, "stages is required");
        Preconditions.checkArgument(  stages.size() > 0, "stages must have more than 1 element" );

        this.stages = stages;
    }


    @Override
    public Stage first() {

        if ( stages.size() == 0 ) {
            return null;
        }

        return stages.get( 0 );
    }





    @Override
    public Stage nextStage( final Stage stage ) {

        Preconditions.checkNotNull( stage, "Stage cannot be null" );

        int index = stages.indexOf( stage );

        //we're done, do nothing
        if ( index == stages.size() -1  ) {
            return null;
        }

        return  stages.get( index + 1 );
    }


    /** Factory to create a new instance. */
    public static StagePipelineImpl fromStages( Stage... stages ) {
        return new StagePipelineImpl(Arrays.asList(  stages ));
    }
}
