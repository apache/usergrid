package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import java.util.UUID;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.commons.lang3.reflect.FieldUtils;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
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
public class StartTest {

    /** Standard flow */
    @Test
    public void testStartStage() throws Exception {


        final WriteContext writeContext = mock( WriteContext.class );
        final CollectionContext context = mock( CollectionContext.class );


        //mock returning the context
        when( writeContext.getCollectionContext() ).thenReturn( context );


        //set up the mock to return the entity from the start phase
        final Entity entity = generateEntity();

        //mock returning the entity from the write context
        when( writeContext.getMessage( Entity.class ) ).thenReturn( entity );


        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        final ArgumentCaptor<MvccLogEntry> logEntry = ArgumentCaptor.forClass( MvccLogEntry.class );

        final MutationBatch mutation = mock( MutationBatch.class );

        when( logStrategy.write( same( context ), logEntry.capture() ) ).thenReturn( mutation );


        //run the stage
        Start newStage = new Start( logStrategy );

        newStage.performStage( writeContext );


        //now verify our output was correct
        ArgumentCaptor<MvccEntity> mvccEntity = ArgumentCaptor.forClass( MvccEntity.class );


        //verify the log entry is correct
        MvccLogEntry entry = logEntry.getValue();

        assertEquals( "entity id did not match ", entity.getUuid(), entry.getEntityId() );
        assertEquals( "version did not not match entityId", entity.getVersion(), entry.getVersion() );
        assertEquals( "Stage is correct", Stage.ACTIVE, entry.getStage() );


        //now verify we set the message into the write context
        verify( writeContext ).setMessage( mvccEntity.capture() );

        MvccEntity created = mvccEntity.getValue();

        //verify uuid and version in both the MvccEntity and the entity itself
        assertEquals( "entity id did not match generator", entity.getUuid(), created.getUuid() );
        assertEquals( "version did not not match entityId", entity.getVersion(), created.getVersion() );
        assertSame( "Entity correct", entity, created.getEntity().get() );


        //now verify the proceed was called
        verify( writeContext ).proceed();
    }


    /** Test no entity in the pipeline */
    @Test( expected = NullPointerException.class )
    public void testNoEntity() throws Exception {


        final WriteContext writeContext = mock( WriteContext.class );


        //mock returning the entity from the write context
        when( writeContext.getMessage( Entity.class ) ).thenReturn( null );


        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        //run the stage
        Start newStage = new Start( logStrategy );

        newStage.performStage( writeContext );
    }


    /** Test no entity id on the entity */
    @Test( expected = NullPointerException.class )
    public void testNoEntityId() throws Exception {


        final WriteContext writeContext = mock( WriteContext.class );


        final Entity entity = new Entity();
        final UUID version = UUIDGenerator.newTimeUUID();

        entity.setVersion( version );

        //mock returning the entity from the write context
        when( writeContext.getMessage( Entity.class ) ).thenReturn( entity );


        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        //run the stage
        Start newStage = new Start( logStrategy );

        newStage.performStage( writeContext );
    }


    /** Test no entity id on the entity */
    @Test( expected = NullPointerException.class )
    public void testNoEntityVersion() throws Exception {


        final WriteContext writeContext = mock( WriteContext.class );


        final Entity entity = new Entity();
        final UUID entityId = UUIDGenerator.newTimeUUID();


        FieldUtils.writeDeclaredField( entity, "uuid", entityId, true );


        //mock returning the entity from the write context
        when( writeContext.getMessage( Entity.class ) ).thenReturn( entity );


        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        //run the stage
        Start newStage = new Start( logStrategy );

        newStage.performStage( writeContext );
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


