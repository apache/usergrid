package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import java.util.UUID;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.stage.AbstractEntityStageTest;
import org.apache.usergrid.persistence.collection.mvcc.stage.IoEvent;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.netflix.astyanax.MutationBatch;

import rx.Observable;
import rx.util.functions.Func1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.when;


/** @author tnine */
public class WriteStartTest extends AbstractEntityStageTest {

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


    protected Entity generateEntity() throws IllegalAccessException {
            final Entity entity = new Entity( "test" );
            final UUID version = UUIDGenerator.newTimeUUID();

            EntityUtils.setVersion( entity, version );

            return entity;
        }


    @Override
    protected Func1<IoEvent<Entity>, ?> getInstance() {
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );
        return new WriteStart( logStrategy );
    }
}


