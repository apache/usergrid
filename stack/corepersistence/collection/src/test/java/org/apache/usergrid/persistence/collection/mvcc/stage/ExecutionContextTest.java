package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.ExecutionContextImpl;

import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/** @author tnine */
public class ExecutionContextTest {

    @Test
    public void performWrite() {

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );

        ExecutionStage executionStage = mock( ExecutionStage.class );

        when( pipeline.first() ).thenReturn( executionStage );

        ExecutionContext executionContext = new ExecutionContextImpl( pipeline, collectionContext );

        Object test = new Object();

        executionContext.execute( test );

        //verify we called first in the pipeline to get the first value
        verify( pipeline ).first();

        //verify the first executionStage was invoked
        verify( executionStage ).performStage( same( executionContext ) );

        //verify the bean value was set
        assertSame( test, executionContext.getMessage( Object.class ) );
    }


    @Test
    public void setAndGet() {
        Object test = new Object();

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );


        ExecutionContext executionContext = new ExecutionContextImpl( pipeline, collectionContext );

        executionContext.setMessage( test );

        assertSame( "Same value returned", test, executionContext.getMessage( Object.class ) );
    }


    @Test
    public void setAndGetTypeSafe() {
        TestBean test = new TestBean();

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );


        ExecutionContext executionContext = new ExecutionContextImpl( pipeline, collectionContext );

        executionContext.setMessage( test );

        //works because Test is an instance of object
        assertSame( "Test instance of object", test, executionContext.getMessage( Object.class ) );

        assertSame( "Test instance of object", test, executionContext.getMessage( TestBean.class ) );
    }


    @Test( expected = ClassCastException.class )
    public void setAndGetBadType() {
        Object test = new Object();

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );


        ExecutionContext executionContext = new ExecutionContextImpl( pipeline, collectionContext );

        executionContext.setMessage( test );

        //works because Test is an instance of object
        assertSame( "Test instance of object", test, executionContext.getMessage( Object.class ) );

        //should blow up, not type save.  The object test is not an instance of TestBean
        executionContext.getMessage( TestBean.class );
    }


    @Test
    public void nullMessage() {

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );


        ExecutionContext executionContext = new ExecutionContextImpl( pipeline, collectionContext );

        executionContext.setMessage( null );

        //works because Test is an instance of object
        assertNull( "Null message returned", executionContext.getMessage( Object.class ) );
    }


    @Test
    public void proceedHasNextStep() {

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );

        ExecutionStage firstExecutionStage = mock( ExecutionStage.class );

        ExecutionStage secondExecutionStage = mock( ExecutionStage.class );


        when( pipeline.first() ).thenReturn( firstExecutionStage );

        when( pipeline.nextStage( same( firstExecutionStage ) ) ).thenReturn( secondExecutionStage );


        ExecutionContext executionContext = new ExecutionContextImpl( pipeline, collectionContext );

        Object test = new Object();

        executionContext.execute( test );

        //now proceed and validate we were called
        executionContext.proceed();

        verify( secondExecutionStage ).performStage( same( executionContext ) );
    }


    @Test
    public void proceedNoNextStep() {

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );

        ExecutionStage firstExecutionStage = mock( ExecutionStage.class );

        when( pipeline.first() ).thenReturn( firstExecutionStage );

        when( pipeline.nextStage( same( firstExecutionStage ) ) ).thenReturn( null );


        ExecutionContext executionContext = new ExecutionContextImpl( pipeline, collectionContext );

        Object test = new Object();

        executionContext.execute( test );

        //now proceed and validate we were called
        executionContext.proceed();
    }


    @Test
    public void getContextCorrect() {

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );


        ExecutionContext executionContext = new ExecutionContextImpl( pipeline, collectionContext );

        assertSame( "Collection context pointer correct", collectionContext, executionContext.getCollectionContext() );
    }




    @Test( expected = NullPointerException.class )
    public void nullContextFails() {

        CollectionContext collectionContext = mock( CollectionContext.class );


        new ExecutionContextImpl( null, collectionContext );
    }


    @Test( expected = NullPointerException.class )
    public void nullPipelineFails() {

        CollectionContext collectionContext = mock( CollectionContext.class );


        new ExecutionContextImpl( null, collectionContext );
    }


    private static class TestBean {

    }
}
