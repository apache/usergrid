package org.apache.usergrid.persistence.collection;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


/** @author tnine */
public class EntityCollectionManagerTest {
//
//    @Test
//    public void create(){
//
//        EventStage mockEventStage = mock(EventStage.class);
//
//        StagePipeline createPipeline = mock(StagePipeline.class);
//        StagePipeline updatePipeline = mock(StagePipeline.class);
//        StagePipeline deletePipeline = mock(StagePipeline.class);
//        StagePipeline loadPipeline = mock(StagePipeline.class);
//
//
//
//        //mock up returning the first stage
//        when(createPipeline.first()).thenReturn( mockEventStage );
//
//
//        EntityCollection context = new EntityCollectionImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );
//
//        EntityCollectionManager collectionManager = new EntityCollectionManagerImpl(createPipeline, updatePipeline, deletePipeline, loadPipeline, context);
//
//        Entity create = new Entity();
//
//        MvccEntity mvccEntity = mock(MvccEntity.class);
//
//
//        Entity returned = collectionManager.create( create );
//
//        //verify the first stage was asked for
//        verify(createPipeline).first();
//
//        ArgumentCaptor<ExecutionContext> contextArg = ArgumentCaptor.forClass(ExecutionContext.class);
//
//        //verify the first perform stage was invoked
//        verify( mockEventStage ).performStage( contextArg.capture() );
//
//        //verify we set the passed entity into the ExecutionContext
//        assertEquals("Entity should be present in the write context", create, contextArg.getValue().getMessage( Entity.class ));
//
//    }
}
