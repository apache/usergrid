package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.junit.Test;

import org.apache.usergrid.persistence.collection.mvcc.stage.impl.StagePipelineImpl;

import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;


/** @author tnine */
public class StagePipelineTest {

    @Test
    public void oneStage() {
        ExecutionStage first = mock( ExecutionStage.class );

        StagePipeline pipeline = StagePipelineImpl.fromStages( first );

        assertSame( "Correct stage returned", first, pipeline.first() );

        ExecutionStage next = pipeline.nextStage( first );

        assertNull( "No next stage", next );
    }


    @Test
    public void threeStages() {
        ExecutionStage first = mock( ExecutionStage.class );
        ExecutionStage second = mock( ExecutionStage.class );
        ExecutionStage third = mock( ExecutionStage.class );

        StagePipeline pipeline = StagePipelineImpl.fromStages( first, second, third );

        assertSame( "Correct stage returned", first, pipeline.first() );

        ExecutionStage next = pipeline.nextStage( first );

        assertSame( "Correct stage returned", second, next );

        next = pipeline.nextStage( next );

        assertSame( "Correct stage returned", third, next );

        next = pipeline.nextStage( next );

        assertNull( "No next stage", next );
    }


    /**
     * Test seeking without calling .first() just to make sure there's no side effects
     */
    @Test
    public void stageSeek() {
        ExecutionStage first = mock( ExecutionStage.class );
        ExecutionStage second = mock( ExecutionStage.class );
        ExecutionStage third = mock( ExecutionStage.class );

        StagePipeline pipeline = StagePipelineImpl.fromStages( first, second, third );


        ExecutionStage next = pipeline.nextStage( second );

        assertSame( "Correct stage returned", third, next );

        next = pipeline.nextStage( next );

        assertNull( "No next stage", next );
    }


    @Test( expected = NullPointerException.class )
    public void invalidStageInput() {
        ExecutionStage first = mock( ExecutionStage.class );

        StagePipeline pipeline = StagePipelineImpl.fromStages( first );
        pipeline.nextStage( null );
    }


    @Test( expected = IllegalArgumentException.class )
    public void noStagesErrors() {
        StagePipelineImpl.fromStages();
    }
}
