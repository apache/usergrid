package org.apache.usergrid.persistence.collection.mvcc.stage.impl.write;


import java.util.UUID;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.IoEvent;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.netflix.astyanax.MutationBatch;

import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.when;


/** @author tnine */
public class WriteStartTest {

    /** Standard flow */
    @Test
    public void testStartStage() throws Exception {


        final EntityCollection context = mock( EntityCollection.class );


        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        final ArgumentCaptor<MvccLogEntry> logEntry = ArgumentCaptor.forClass( MvccLogEntry.class );

        final MutationBatch mutation = mock( MutationBatch.class );

        when( logStrategy.write( same( context ), logEntry.capture() ) ).thenReturn( mutation );


        //set up the mock to return the entity from the start phase
        final Entity entity = generateEntity();


        //run the stage
        WriteStart newStage = new WriteStart( logStrategy );


        Observable<IoEvent<MvccEntity>> observable = newStage.call( new IoEvent<Entity>( context, entity ) );

        //verify the observable is correct
        IoEvent<MvccEntity> result = observable.toBlockingObservable().single();


        //verify the log entry is correct
        MvccLogEntry entry = logEntry.getValue();

        assertEquals( "version did not not match entityId", entity.getVersion(), entry.getVersion() );
        assertEquals( "EventStage is correct", Stage.ACTIVE, entry.getStage() );


        MvccEntity created = result.getEvent();

        //verify uuid and version in both the MvccEntity and the entity itself
        assertEquals( "version did not not match entityId", entity.getVersion(), created.getVersion() );
        assertSame( "Entity correct", entity, created.getEntity().get() );
    }


    /** Test no entity id on the entity */
    @Test( expected = NullPointerException.class )
    public void testNoEntityId() throws Exception {


        final Id entityId = mock( Id.class );

        when( entityId.getUuid() ).thenReturn( null );
        when( entityId.getType() ).thenReturn( "test" );

        final Entity entity = new Entity( entityId );


        final EntityCollection context = mock( EntityCollection.class );


        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        //run the stage
        WriteStart newStage = new WriteStart( logStrategy );

        newStage.call( new IoEvent<Entity>( context, entity ) );
    }


    /** Test no entity id on the entity */
    @Test( expected = IllegalArgumentException.class )
    public void testWrongEntityType() throws Exception {


        final Id entityId = mock( Id.class );

        //set this to a non time uuid
        when( entityId.getUuid() ).thenReturn( UUID.randomUUID() );
        when( entityId.getType() ).thenReturn( "test" );

        final Entity entity = new Entity( entityId );


        final EntityCollection context = mock( EntityCollection.class );


        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        //run the stage
        WriteStart newStage = new WriteStart( logStrategy );

        newStage.call( new IoEvent<Entity>( context, entity ) );
    }


    /** Test no entity id on the entity */
    @Test( expected = NullPointerException.class )
    public void testNoEntityType() throws Exception {

        final Id entityId = mock( Id.class );

        when( entityId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );
        when( entityId.getType() ).thenReturn( null );

        final Entity entity = new Entity( entityId );


        final EntityCollection context = mock( EntityCollection.class );


        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        //run the stage
        WriteStart newStage = new WriteStart( logStrategy );

        newStage.call( new IoEvent<Entity>( context, entity ) );
    }


    /** Test no entity id on the entity */
    @Test( expected = NullPointerException.class )
    public void testNoVersionEntityType() throws Exception {

        final Id entityId = mock( Id.class );

        when( entityId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );
        when( entityId.getType() ).thenReturn( "test" );

        final Entity entity = new Entity( entityId );


        final EntityCollection context = mock( EntityCollection.class );


        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        //run the stage
        WriteStart newStage = new WriteStart( logStrategy );

        newStage.call( new IoEvent<Entity>( context, entity ) );
    }


    private Entity generateEntity() throws IllegalAccessException {
        final Entity entity = new Entity( "test" );
        final UUID version = UUIDGenerator.newTimeUUID();

        EntityUtils.setVersion( entity, version );

        return entity;
    }
}


