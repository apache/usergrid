package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.MvccEntityNew;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.service.TimeService;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/** @author tnine */
public class MvccEntityNewTest {

    /**
     * Test the start stage for happy path
     * TODO throw junk at it
     * TODO refactor a lot of this mock setup.  It's common across a lot of tests
     */
    @Test
    public void testStartStage() throws ConnectionException, ExecutionException, InterruptedException {

        final MvccLogEntrySerializationStrategy logStrategy = mock( MvccLogEntrySerializationStrategy.class );

        final ArgumentCaptor<MvccLogEntry> logEntry = ArgumentCaptor.forClass( MvccLogEntry.class );


        final WriteContext writeContext = mock( WriteContext.class );
        final CollectionContext context = mock( CollectionContext.class );


        //mock returning the context
        when( writeContext.getCollectionContext() ).thenReturn( context );


        final MutationBatch mutation = mock( MutationBatch.class );


        //mock returning a mock mutation when we do a log entry write
        when( logStrategy.write( same( context ), logEntry.capture() ) ).thenReturn( mutation );


        //mock the listenable future
        final ListenableFuture<OperationResult<Void>> future = mock( ListenableFuture.class);
        final OperationResult<Void> result = mock(OperationResult.class);

        when(mutation.executeAsync()).thenReturn( future );

        //mock the "get" on the future
        when(future.get()).thenReturn( result  );


        //set up the mock to return the entity from the start phase
        final Entity entity = new Entity();

        when( writeContext.getMessage( Entity.class ) ).thenReturn( entity );


        //mock returning the time
        final TimeService timeService = mock( TimeService.class );

        final long time = System.currentTimeMillis();

        when( timeService.getTime() ).thenReturn( time );


        //mock the uuid service
        final UUIDService uuidService = mock( UUIDService.class );

        final UUID newEntityId = UUIDGenerator.newTimeUUID();
        final UUID newVersion = newEntityId;


        //mock the uuid service
        when( uuidService.newTimeUUID() ).thenReturn( newEntityId );


        //run the stage
        MvccEntityNew newStage = new MvccEntityNew( logStrategy, timeService, uuidService );

        newStage.performStage( writeContext );


        //now verify our output was correct
        ArgumentCaptor<MvccEntity> mvccEntity = ArgumentCaptor.forClass( MvccEntity.class );


        verify( writeContext).setMessage( mvccEntity.capture() );

        MvccEntity created = mvccEntity.getValue();

        //verify uuid and version in both the MvccEntity and the entity itself
        assertEquals( "entity id did not match generator", newEntityId, created.getUuid() );
        assertEquals( "entity id did not match generator", newEntityId, created.getEntity().get().getUuid() );
        assertEquals( "version did not not match entityId", newVersion, created.getVersion() );
        assertEquals( "version did not not match entityId", newVersion, created.getEntity().get().getVersion() );

        //check the time
        assertEquals( "created time matches generator", time, created.getEntity().get().getCreated() );
        assertEquals( "updated time matches generator", time, created.getEntity().get().getUpdated() );

        //now  verify we invoked the mvcc log operation correctly

        MvccLogEntry entry = logEntry.getValue();

        assertEquals("Log entry has correct uuid", newEntityId,  entry.getEntityId());
        assertEquals("Log entry has correct version", newVersion,  entry.getEntityId());
        assertEquals( "Stage was correct", Stage.ACTIVE, entry.getStage() );

        //now verify the proceed was called
        verify(writeContext).proceed();

    }
}
