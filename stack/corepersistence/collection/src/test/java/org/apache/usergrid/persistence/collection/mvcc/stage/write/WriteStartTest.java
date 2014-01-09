package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.stage.AbstractEntityStageTest;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.netflix.astyanax.MutationBatch;

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


        final CollectionScope context = mock( CollectionScope.class );


        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        final ArgumentCaptor<MvccLogEntry> logEntry = ArgumentCaptor.forClass( MvccLogEntry.class );

        final MutationBatch mutation = mock( MutationBatch.class );

        when( logStrategy.write( same( context ), logEntry.capture() ) ).thenReturn( mutation );


        //set up the mock to return the entity from the start phase
        final Entity entity = TestEntityGenerator.generateEntity();

        //run the stage
        WriteStart newStage = new WriteStart( logStrategy );


        //verify the observable is correct
        CollectionIoEvent<MvccEntity> result = newStage.call( new CollectionIoEvent<Entity>( context, entity ) );


        //verify the log entry is correct
        MvccLogEntry entry = logEntry.getValue();

        assertEquals( "id correct", entity.getId(), entry.getEntityId() );
        assertEquals( "version did not not match entityId", entity.getVersion(), entry.getVersion() );
        assertEquals( "EventStage is correct", Stage.ACTIVE, entry.getStage() );


        MvccEntity created = result.getEvent();

        //verify uuid and version in both the MvccEntity and the entity itself
        //assertSame is used on purpose.  We want to make sure the same instance is used, not a copy.
        //this way the caller's runtime type is retained.
        assertSame( "id correct", entity.getId(), created.getId() );
        assertSame( "version did not not match entityId", entity.getVersion(), created.getVersion() );
        assertSame( "Entity correct", entity, created.getEntity().get() );
    }


    @Override
    protected void validateStage( final CollectionIoEvent<Entity> event ) {
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );
        new WriteStart( logStrategy ).call( event );
    }
}


