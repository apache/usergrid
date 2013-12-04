package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.WriteContextImpl;

import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/** @author tnine */
public class WriteContextTest {

    @Test
    public void performWrite() {

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );

        WriteStage stage = mock( WriteStage.class );

        when( pipeline.first() ).thenReturn( stage );

        WriteContext writeContext = new WriteContextImpl( pipeline, collectionContext );

        Object test = new Object();

        writeContext.performWrite( test );

        //verify we called first in the pipeline to get the first value
        verify( pipeline ).first();

        //verify the first stage was invoked
        verify( stage ).performStage( same( writeContext ) );

        //verify the bean value was set
        assertSame( test, writeContext.getMessage( Object.class ) );
    }


    @Test
    public void setAndGet() {
        Object test = new Object();

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );


        WriteContext writeContext = new WriteContextImpl( pipeline, collectionContext );

        writeContext.setMessage( test );

        assertSame( "Same value returned", test, writeContext.getMessage( Object.class ) );
    }


    @Test
    public void setAndGetTypeSafe() {
        TestBean test = new TestBean();

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );


        WriteContext writeContext = new WriteContextImpl( pipeline, collectionContext );

        writeContext.setMessage( test );

        //works because Test is an instance of object
        assertSame( "Test instance of object", test, writeContext.getMessage( Object.class ) );

        assertSame( "Test instance of object", test, writeContext.getMessage( TestBean.class ) );
    }


    @Test( expected = ClassCastException.class )
    public void setAndGetBadType() {
        Object test = new Object();

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );


        WriteContext writeContext = new WriteContextImpl( pipeline, collectionContext );

        writeContext.setMessage( test );

        //works because Test is an instance of object
        assertSame( "Test instance of object", test, writeContext.getMessage( Object.class ) );

        //should blow up, not type save.  The object test is not an instance of TestBean
        writeContext.getMessage( TestBean.class );
    }


    @Test
    public void nullMessage() {

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );


        WriteContext writeContext = new WriteContextImpl( pipeline, collectionContext );

        writeContext.setMessage( null );

        //works because Test is an instance of object
        assertNull( "Null message returned", writeContext.getMessage( Object.class ) );
    }


    @Test
    public void proceedHasNextStep() {

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );

        WriteStage firstStage = mock( WriteStage.class );

        WriteStage secondStage = mock( WriteStage.class );


        when( pipeline.first() ).thenReturn( firstStage );

        when( pipeline.nextStage( same( firstStage ) ) ).thenReturn( secondStage );


        WriteContext writeContext = new WriteContextImpl( pipeline, collectionContext );

        Object test = new Object();

        writeContext.performWrite( test );

        //now proceed and validate we were called
        writeContext.proceed();

        verify( secondStage ).performStage( same( writeContext ) );
    }


    @Test
    public void proceedNoNextStep() {

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );

        WriteStage firstStage = mock( WriteStage.class );

        when( pipeline.first() ).thenReturn( firstStage );

        when( pipeline.nextStage( same( firstStage ) ) ).thenReturn( null );


        WriteContext writeContext = new WriteContextImpl( pipeline, collectionContext );

        Object test = new Object();

        writeContext.performWrite( test );

        //now proceed and validate we were called
        writeContext.proceed();
    }


    @Test
    public void getContextCorrect() {

        CollectionContext collectionContext = mock( CollectionContext.class );

        StagePipeline pipeline = mock( StagePipeline.class );


        WriteContext writeContext = new WriteContextImpl( pipeline, collectionContext );

        assertSame( "Collection context pointer correct", collectionContext, writeContext.getCollectionContext() );
    }




    @Test( expected = NullPointerException.class )
    public void nullContextFails() {

        CollectionContext collectionContext = mock( CollectionContext.class );


        new WriteContextImpl( null, collectionContext );
    }


    @Test( expected = NullPointerException.class )
    public void nullPipelineFails() {

        CollectionContext collectionContext = mock( CollectionContext.class );


        new WriteContextImpl( null, collectionContext );
    }


    private static class TestBean {

    }
}
