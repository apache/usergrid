package org.apache.usergrid.persistence.collection;


import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.usergrid.persistence.collection.impl.CollectionContextImpl;
import org.apache.usergrid.persistence.collection.impl.CollectionManagerImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.StagePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/** @author tnine */
public class CollectionManagerTest {

    @Test
    public void create(){

        WriteStage mockStage = mock(WriteStage.class);

        StagePipeline createPipeline = mock(StagePipeline.class);
        StagePipeline updatePipeline = mock(StagePipeline.class);
        StagePipeline deletePipeline = mock(StagePipeline.class);

        //mock up returning the first stage
        when(createPipeline.first()).thenReturn(mockStage);


        CollectionContext context = new CollectionContextImpl( UUIDGenerator.newTimeUUID(), UUIDGenerator.newTimeUUID(), "test" );

        CollectionManager collectionManager = new CollectionManagerImpl(createPipeline, updatePipeline, deletePipeline,  context);

        Entity create = new Entity();

        Entity returned = collectionManager.create( create );

        //verify the first stage was asked for
        verify(createPipeline).first();

        ArgumentCaptor<WriteContext> contextArg = ArgumentCaptor.forClass(WriteContext.class);

        //verify the first perform stage was invoked
        verify(mockStage).performStage( contextArg.capture() );

        //verify we set the passed entity into the WriteContext
        assertEquals("Entity should be present in the write context", create, contextArg.getValue().getMessage( Entity.class ));

    }
}
