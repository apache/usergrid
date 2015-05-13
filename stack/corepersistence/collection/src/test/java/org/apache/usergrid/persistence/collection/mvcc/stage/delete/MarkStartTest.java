package org.apache.usergrid.persistence.collection.mvcc.stage.delete;


import java.util.UUID;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.stage.AbstractIdStageTest;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.netflix.astyanax.MutationBatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/** @author tnine */
public class MarkStartTest extends AbstractIdStageTest {

    @Test
    public void testWrite() {

        final ApplicationScope context = mock( ApplicationScope.class );


        //mock returning a mock mutation when we do a log entry write
        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        final ArgumentCaptor<MvccLogEntry> logEntry = ArgumentCaptor.forClass( MvccLogEntry.class );

        final MutationBatch mutation = mock( MutationBatch.class );

        when( logStrategy.write( same( context ), logEntry.capture() ) ).thenReturn( mutation );


        //mock up the version
        final UUIDService uuidService = mock( UUIDService.class );

        final UUID version = UUIDGenerator.newTimeUUID();

        when( uuidService.newTimeUUID() ).thenReturn( version );


        //run the stage
        MarkStart newStage = new MarkStart( logStrategy, uuidService );

        final Id id = TestEntityGenerator.generateId();


        //verify the observable is correct
        CollectionIoEvent<MvccEntity> result = newStage.call( new CollectionIoEvent<Id>( context, id ) );


        //verify the log entry is correct
        MvccLogEntry entry = logEntry.getValue();

        assertEquals( "id correct", id, entry.getEntityId() );
        assertEquals( "version correct", version, entry.getVersion() );
        assertEquals( "EventStage is correct", Stage.ACTIVE, entry.getStage() );


        MvccEntity created = result.getEvent();

        //verify uuid and version in both the MvccEntity and the entity itself
        //verify uuid and version in both the MvccEntity and the entity itself
        //assertSame is used on purpose.  We want to make sure the same instance is used, not a copy.
        //this way the caller's runtime type is retained.
        assertSame( "id correct", id, created.getId() );
        assertSame( "version did not not match entityId", version, created.getVersion() );
        assertFalse( "Entity correct", created.getEntity().isPresent() );
    }


    @Override
    protected void validateStage( final CollectionIoEvent<Id> event ) {

        MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        UUIDService uuidService = mock( UUIDService.class );

        //generate the random uuid, not a time uuid, should fail tests
        when(uuidService.newTimeUUID()).thenReturn( UUID.randomUUID() );

        new MarkStart( logStrategy, uuidService ).call( event );
    }
}
