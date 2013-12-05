package org.apache.usergrid.persistence.collection.mvcc.stage.impl.write;


import java.util.UUID;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.commons.lang3.reflect.FieldUtils;

import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.mvcc.entity.CollectionEventBus;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.stage.Result;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.netflix.astyanax.MutationBatch;

import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/** @author tnine */
public class StartWriteTest {

    /** Standard flow */
    @Test
    public void testStartStage() throws Exception {


        final EntityCollection context = mock( EntityCollection.class );

        final CollectionEventBus bus = mock( CollectionEventBus.class );


        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        final ArgumentCaptor<MvccLogEntry> logEntry = ArgumentCaptor.forClass( MvccLogEntry.class );

        final MutationBatch mutation = mock( MutationBatch.class );

        when( logStrategy.write( same( context ), logEntry.capture() ) ).thenReturn( mutation );


        Result result = new Result();

        //set up the mock to return the entity from the start phase
        final Entity entity = generateEntity();


        EventStart start = new EventStart( context, entity, result );

        //run the stage
        StartWrite newStage = new StartWrite( bus, logStrategy );

        newStage.performStage( start );


        //now verify our output was correct
        ArgumentCaptor<EventVerify> eventVerify = ArgumentCaptor.forClass( EventVerify.class );


        //verify the log entry is correct
        MvccLogEntry entry = logEntry.getValue();

        assertEquals( "entity id did not match ", entity.getUuid(), entry.getEntityId() );
        assertEquals( "version did not not match entityId", entity.getVersion(), entry.getVersion() );
        assertEquals( "EventStage is correct", Stage.ACTIVE, entry.getStage() );


        //now verify we set the message into the write context
        verify( bus ).post( eventVerify.capture() );

        MvccEntity created = eventVerify.getValue().getData();

        //verify uuid and version in both the MvccEntity and the entity itself
        assertEquals( "entity id did not match generator", entity.getUuid(), created.getUuid() );
        assertEquals( "version did not not match entityId", entity.getVersion(), created.getVersion() );
        assertSame( "Entity correct", entity, created.getEntity().get() );
    }


    /** Test no entity id on the entity */
    @Test( expected = NullPointerException.class )
    public void testNoEntityId() throws Exception {


        final Entity entity = new Entity();
        final UUID version = UUIDGenerator.newTimeUUID();

        entity.setVersion( version );


        final EntityCollection context = mock( EntityCollection.class );
        final CollectionEventBus eventBus = mock( CollectionEventBus.class );

        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        //run the stage
        StartWrite newStage = new StartWrite( eventBus, logStrategy );

        newStage.performStage( new EventStart( context, entity, new Result() ) );
    }


    /** Test no entity id on the entity */
    @Test( expected = NullPointerException.class )
    public void testNoEntityVersion() throws Exception {


        final Entity entity = new Entity();
        final UUID entityId = UUIDGenerator.newTimeUUID();


        FieldUtils.writeDeclaredField( entity, "uuid", entityId, true );


        final EntityCollection context = mock( EntityCollection.class );
        final CollectionEventBus eventBus = mock( CollectionEventBus.class );


        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        //run the stage
        StartWrite newStage = new StartWrite( eventBus, logStrategy );

        newStage.performStage( new EventStart( context, entity, new Result() ) );
    }


    private Entity generateEntity() throws IllegalAccessException {
        final Entity entity = new Entity();
        final UUID entityId = UUIDGenerator.newTimeUUID();
        final UUID version = UUIDGenerator.newTimeUUID();

        FieldUtils.writeDeclaredField( entity, "uuid", entityId, true );
        entity.setVersion( version );

        return entity;
    }
}


