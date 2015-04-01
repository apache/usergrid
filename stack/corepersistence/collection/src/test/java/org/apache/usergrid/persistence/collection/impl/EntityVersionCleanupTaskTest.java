/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.collection.impl;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import org.junit.AfterClass;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.event.EntityVersionDeleted;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.util.LogEntryMock;
import org.apache.usergrid.persistence.collection.util.UniqueValueEntryMock;
import org.apache.usergrid.persistence.collection.util.VersionGenerator;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.task.NamedTaskExecutorImpl;
import org.apache.usergrid.persistence.core.task.TaskExecutor;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Cleanup task tests
 */
public class EntityVersionCleanupTaskTest {

    private static final TaskExecutor taskExecutor = new NamedTaskExecutorImpl( "test", 4, 0 );


    @AfterClass
    public static void shutdown() {
        taskExecutor.shutdown();
    }


    @Test( timeout = 10000 )
    public void noListenerOneVersion() throws Exception {


        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getBufferSize() ).thenReturn( 10 );

        final MvccLogEntrySerializationStrategy less = mock( MvccLogEntrySerializationStrategy.class );


        final UniqueValueSerializationStrategy uvss = mock( UniqueValueSerializationStrategy.class );

        final Keyspace keyspace = mock( Keyspace.class );

        final MutationBatch entityBatch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() ).thenReturn(
                mock( MutationBatch.class ) ) // don't care what happens to this one
                .thenReturn( entityBatch );

        // intentionally no events
        final Set<EntityVersionDeleted> listeners = new HashSet<EntityVersionDeleted>();

        final Id applicationId = new SimpleId( "application" );

        final ApplicationScope appScope = new ApplicationScopeImpl( applicationId );

        final Id entityId = new SimpleId( "user" );

        final List<UUID> versions = VersionGenerator.generateVersions( 2 );

        // mock up a single log entry for our first test
        final LogEntryMock logEntryMock = LogEntryMock.createLogEntryMock( less, appScope, entityId, versions );


        //get the version we're keeping, it's first in our list
        final UUID version = logEntryMock.getEntryAtIndex( 0 ).getVersion();

        //mock up unique version output
        final UniqueValueEntryMock uniqueValueEntryMock =
                UniqueValueEntryMock.createUniqueMock( uvss, appScope, entityId, versions );


        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig, less, uvss, keyspace, listeners, appScope, entityId,
                        version, false );

        final MutationBatch newBatch = mock( MutationBatch.class );


        // set up returning a mutator
        when( uvss.delete( same( appScope ), any( UniqueValue.class ) ) ).thenReturn( newBatch );

        //return a new batch when it's called
        when( less.delete( same( appScope ), same( entityId ), any( UUID.class ) ) ).thenReturn( newBatch );


        cleanupTask.call();


        //get the second field, this should be deleted
        final UniqueValue oldUniqueField = uniqueValueEntryMock.getEntryAtIndex( 1 );

        final MvccLogEntry expectedDeletedEntry = logEntryMock.getEntryAtIndex( 1 );


        //verify delete was invoked
        verify( uvss ).delete( same( appScope ), same( oldUniqueField ) );

        //verify the delete was invoked
        verify( less ).delete( same( appScope ), same( entityId ), same( expectedDeletedEntry.getVersion() ) );

        // verify it was run
        verify( entityBatch ).execute();
    }


    /**
     * Tests the cleanup task on the first version created
     */
    @Test( timeout = 10000 )
    public void noListenerNoVersions() throws Exception {


        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getBufferSize() ).thenReturn( 10 );

        final MvccLogEntrySerializationStrategy less = mock( MvccLogEntrySerializationStrategy.class );


        final UniqueValueSerializationStrategy uvss = mock( UniqueValueSerializationStrategy.class );

        final Keyspace keyspace = mock( Keyspace.class );

        final MutationBatch entityBatch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() ).thenReturn(
                mock( MutationBatch.class ) ) // don't care what happens to this one
                .thenReturn( entityBatch );

        // intentionally no events
        final Set<EntityVersionDeleted> listeners = new HashSet<>();

        final Id applicationId = new SimpleId( "application" );

        final ApplicationScope appScope = new ApplicationScopeImpl( applicationId );

        final Id entityId = new SimpleId( "user" );


        final List<UUID> versions = VersionGenerator.generateVersions( 1 );

        // mock up a single log entry, with no other entries
        final LogEntryMock logEntryMock = LogEntryMock.createLogEntryMock( less, appScope, entityId, versions );


        //get the version we're keeping, it's first in our list
        final UUID version = logEntryMock.getEntryAtIndex( 0 ).getVersion();

        //mock up unique version output
        final UniqueValueEntryMock uniqueValueEntryMock =
                UniqueValueEntryMock.createUniqueMock( uvss, appScope, entityId, versions );


        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig, less, uvss, keyspace, listeners, appScope, entityId, version, false );

        final MutationBatch newBatch = mock( MutationBatch.class );


        // set up returning a mutator
        when( uvss.delete( same( appScope ), any( UniqueValue.class ) ) ).thenReturn( newBatch );

        //return a new batch when it's called
        when( less.delete( same( appScope ), same( entityId ), any( UUID.class ) ) ).thenReturn( newBatch );


        cleanupTask.call();


        //verify delete was never invoked
        verify( uvss, never() ).delete( any( ApplicationScope.class ), any( UniqueValue.class ) );

        //verify the delete was never invoked
        verify( less, never() ).delete( any( ApplicationScope.class ), any( Id.class ), any( UUID.class ) );
    }


    @Test( timeout = 10000 )
    public void singleListenerSingleVersion() throws Exception {


        //create a latch for the event listener, and add it to the list of events
        final int sizeToReturn = 1;

        final CountDownLatch latch = new CountDownLatch( sizeToReturn );

        final EntityVersionDeletedTest eventListener = new EntityVersionDeletedTest( latch );

        final Set<EntityVersionDeleted> listeners = new HashSet<>();

        listeners.add( eventListener );


        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getBufferSize() ).thenReturn( 10 );

        final MvccLogEntrySerializationStrategy less = mock( MvccLogEntrySerializationStrategy.class );

        final UniqueValueSerializationStrategy uvss = mock( UniqueValueSerializationStrategy.class );

        final Keyspace keyspace = mock( Keyspace.class );

        final MutationBatch entityBatch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() ).thenReturn(
                mock( MutationBatch.class ) ) // don't care what happens to this one
                .thenReturn( entityBatch );


        final Id applicationId = new SimpleId( "application" );

        final ApplicationScope appScope = new ApplicationScopeImpl( applicationId );

        final Id entityId = new SimpleId( "user" );


        final List<UUID> versions = VersionGenerator.generateVersions( 2 );


        // mock up a single log entry for our first test
        final LogEntryMock logEntryMock = LogEntryMock.createLogEntryMock( less, appScope, entityId, versions );


        //get the version we're keeping, it's first in our list
        final UUID version = logEntryMock.getEntryAtIndex( 0 ).getVersion();

        //mock up unique version output
        final UniqueValueEntryMock uniqueValueEntryMock =
                UniqueValueEntryMock.createUniqueMock( uvss, appScope, entityId, versions );


        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig, less, uvss, keyspace, listeners, appScope, entityId,
                        version, false );

        final MutationBatch newBatch = mock( MutationBatch.class );


        // set up returning a mutator
        when( uvss.delete( same( appScope ), any( UniqueValue.class ) ) ).thenReturn( newBatch );

        //return a new batch when it's called
        when( less.delete( same( appScope ), same( entityId ), any( UUID.class ) ) ).thenReturn( newBatch );


        cleanupTask.call();


        //get the second field, this should be deleted
        final UniqueValue oldUniqueField = uniqueValueEntryMock.getEntryAtIndex( 1 );

        final MvccLogEntry expectedDeletedEntry = logEntryMock.getEntryAtIndex( 1 );


        //verify delete was invoked
        verify( uvss ).delete( same( appScope ), same( oldUniqueField ) );

        //verify the delete was invoked
        verify( less ).delete( same( appScope ), same( entityId ), same( expectedDeletedEntry.getVersion() ) );

        // verify it was run
        verify( entityBatch ).execute();


        //the latch was executed
        latch.await();
    }


    @Test//(timeout=10000)
    public void multipleListenerMultipleVersions() throws Exception {

        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getBufferSize() ).thenReturn( 10 );


        //create a latch for the event listener, and add it to the list of events
        final int sizeToReturn = 10;

        final CountDownLatch latch = new CountDownLatch( sizeToReturn / serializationFig.getBufferSize() * 3 );

        final EntityVersionDeletedTest listener1 = new EntityVersionDeletedTest( latch );
        final EntityVersionDeletedTest listener2 = new EntityVersionDeletedTest( latch );
        final EntityVersionDeletedTest listener3 = new EntityVersionDeletedTest( latch );

        final Set<EntityVersionDeleted> listeners = new HashSet<>();

        listeners.add( listener1 );
        listeners.add( listener2 );
        listeners.add( listener3 );




        final MvccLogEntrySerializationStrategy less = mock( MvccLogEntrySerializationStrategy.class );

        final UniqueValueSerializationStrategy uvss = mock( UniqueValueSerializationStrategy.class );

        final Keyspace keyspace = mock( Keyspace.class );

        final MutationBatch entityBatch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() ).thenReturn(
                mock( MutationBatch.class ) ) // don't care what happens to this one
                .thenReturn( entityBatch );




        final Id applicationId = new SimpleId( "application" );

        final ApplicationScope appScope = new ApplicationScopeImpl( applicationId );

        final Id entityId = new SimpleId( "user" );

        final List<UUID> versions = VersionGenerator.generateVersions( 2 );


        // mock up a single log entry for our first test
        final LogEntryMock logEntryMock = LogEntryMock.createLogEntryMock( less, appScope, entityId, versions );


        //get the version we're keeping, it's first in our list
        final UUID version = logEntryMock.getEntryAtIndex( 0 ).getVersion();

        //mock up unique version output
        final UniqueValueEntryMock uniqueValueEntryMock =
                UniqueValueEntryMock.createUniqueMock( uvss, appScope, entityId, versions );


        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig, less, uvss, keyspace, listeners, appScope, entityId,
                        version, false );

        final MutationBatch newBatch = mock( MutationBatch.class );


        // set up returning a mutator
        when( uvss.delete( same( appScope ), any( UniqueValue.class ) ) ).thenReturn( newBatch );

        //return a new batch when it's called
        when( less.delete( same( appScope ), same( entityId ), any( UUID.class ) ) ).thenReturn( newBatch );


        cleanupTask.call();


        //get the second field, this should be deleted
        final UniqueValue oldUniqueField = uniqueValueEntryMock.getEntryAtIndex( 1 );

        final MvccLogEntry expectedDeletedEntry = logEntryMock.getEntryAtIndex( 1 );


        //verify delete was invoked
        verify( uvss ).delete( same( appScope ), same( oldUniqueField ) );

        //verify the delete was invoked
        verify( less ).delete( same( appScope ), same( entityId ), same( expectedDeletedEntry.getVersion() ) );

        // verify it was run
        verify( entityBatch ).execute();


        //the latch was executed
        latch.await();

        //we deleted the version
        //verify we deleted everything
          //verify delete was invoked
        verify( uvss ).delete( same( appScope ), same( oldUniqueField ) );

        //verify the delete was invoked
        verify( less ).delete( same( appScope ), same( entityId ), same( expectedDeletedEntry.getVersion() ) );

        // verify it was run
        verify( entityBatch ).execute();

        //the latch was executed
        latch.await();
    }


    /**
     * Tests what happens when our listeners are VERY slow
     */
//    @Ignore( "Test is a work in progress" )
    @Test( timeout = 10000 )
    public void multipleListenerMultipleVersionsNoThreadsToRun()
            throws ExecutionException, InterruptedException, ConnectionException {


        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getBufferSize() ).thenReturn( 10 );


        //create a latch for the event listener, and add it to the list of events
        final int sizeToReturn = 10;


        final int listenerCount = 5;

        final CountDownLatch latch =
                new CountDownLatch( sizeToReturn / serializationFig.getBufferSize() * listenerCount );
        final Semaphore waitSemaphore = new Semaphore( 0 );


        final SlowListener listener1 = new SlowListener( latch, waitSemaphore );
        final SlowListener listener2 = new SlowListener( latch, waitSemaphore );
        final SlowListener listener3 = new SlowListener( latch, waitSemaphore );
        final SlowListener listener4 = new SlowListener( latch, waitSemaphore );
        final SlowListener listener5 = new SlowListener( latch, waitSemaphore );

        final Set<EntityVersionDeleted> listeners = new HashSet<EntityVersionDeleted>();

        listeners.add( listener1 );
        listeners.add( listener2 );
        listeners.add( listener3 );
        listeners.add( listener4 );
        listeners.add( listener5 );


        final MvccLogEntrySerializationStrategy less = mock( MvccLogEntrySerializationStrategy.class );

        final UniqueValueSerializationStrategy uvss = mock( UniqueValueSerializationStrategy.class );

        final Keyspace keyspace = mock( Keyspace.class );

        final MutationBatch entityBatch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() ).thenReturn(
                mock( MutationBatch.class ) ) // don't care what happens to this one
                .thenReturn( entityBatch );


        final Id applicationId = new SimpleId( "application" );

        final ApplicationScope appScope = new ApplicationScopeImpl( applicationId );

        final Id entityId = new SimpleId( "user" );


        final List<UUID> versions = VersionGenerator.generateVersions( 2 );

        // mock up a single log entry for our first test
        final LogEntryMock logEntryMock = LogEntryMock.createLogEntryMock( less, appScope, entityId, versions );


        //get the version we're keeping, it's first in our list
        final UUID version = logEntryMock.getEntryAtIndex( 0 ).getVersion();


        //mock up unique version output
        final UniqueValueEntryMock uniqueValueEntryMock =
                UniqueValueEntryMock.createUniqueMock( uvss, appScope, entityId, versions );


        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig, less, uvss, keyspace, listeners, appScope, entityId,
                        version, false);

        final MutationBatch newBatch = mock( MutationBatch.class );


        // set up returning a mutator
        when( uvss.delete( same( appScope ), any( UniqueValue.class ) ) ).thenReturn( newBatch );

        //return a new batch when it's called
        when( less.delete( same( appScope ), same( entityId ), any( UUID.class ) ) ).thenReturn( newBatch );


        //start the task
        ListenableFuture<Void> future = taskExecutor.submit( cleanupTask );

        /**
         * While we're not done, release latches every 200 ms
         */
        while ( !future.isDone() ) {
            Thread.sleep( 200 );
            waitSemaphore.release( listenerCount );
        }

        //wait for the task
        future.get();


        //get the second field, this should be deleted
        final UniqueValue oldUniqueField = uniqueValueEntryMock.getEntryAtIndex( 1 );

        final MvccLogEntry expectedDeletedEntry = logEntryMock.getEntryAtIndex( 1 );


        //verify delete was invoked
        verify( uvss ).delete( same( appScope ), same( oldUniqueField ) );

        //verify the delete was invoked
        verify( less ).delete( same( appScope ), same( entityId ), same( expectedDeletedEntry.getVersion() ) );

        // verify it was run
        verify( entityBatch ).execute();


        //the latch was executed
        latch.await();

        //we deleted the version
        //verify we deleted everything
        //verify delete was invoked
        verify( uvss ).delete( same( appScope ), same( oldUniqueField ) );

        //verify the delete was invoked
        verify( less ).delete( same( appScope ), same( entityId ), same( expectedDeletedEntry.getVersion() ) );

        // verify it was run
        verify( entityBatch ).execute();

        //the latch was executed
        latch.await();


        //the latch was executed
        latch.await();
    }


    /**
     * Tests that our task will run in the caller if there's no threads, ensures that the task runs
     */
    @Test( timeout = 10000 )
    public void singleListenerSingleVersionRejected()
            throws ExecutionException, InterruptedException, ConnectionException {



        //create a latch for the event listener, and add it to the list of events
        final int sizeToReturn = 1;

        final CountDownLatch latch = new CountDownLatch( sizeToReturn );

        final EntityVersionDeletedTest eventListener = new EntityVersionDeletedTest( latch );

        final Set<EntityVersionDeleted> listeners = new HashSet<>();

        listeners.add( eventListener );


        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getBufferSize() ).thenReturn( 10 );

        final MvccLogEntrySerializationStrategy less = mock( MvccLogEntrySerializationStrategy.class );

        final UniqueValueSerializationStrategy uvss = mock( UniqueValueSerializationStrategy.class );

        final Keyspace keyspace = mock( Keyspace.class );

        final MutationBatch entityBatch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() ).thenReturn(
                mock( MutationBatch.class ) ) // don't care what happens to this one
                .thenReturn( entityBatch );


        final Id applicationId = new SimpleId( "application" );

        final ApplicationScope appScope = new ApplicationScopeImpl( applicationId );

        final Id entityId = new SimpleId( "user" );


        final List<UUID> versions = VersionGenerator.generateVersions( 2 );


        // mock up a single log entry for our first test
        final LogEntryMock logEntryMock = LogEntryMock.createLogEntryMock( less, appScope, entityId, versions );


        //get the version we're keeping, it's first in our list
        final UUID version = logEntryMock.getEntryAtIndex( 0 ).getVersion();

        //mock up unique version output
        final UniqueValueEntryMock uniqueValueEntryMock =
                UniqueValueEntryMock.createUniqueMock( uvss, appScope, entityId, versions );


        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig, less, uvss, keyspace, listeners, appScope, entityId,
                        version, false );

        final MutationBatch newBatch = mock( MutationBatch.class );


        // set up returning a mutator
        when( uvss.delete( same( appScope ), any( UniqueValue.class ) ) ).thenReturn( newBatch );

        //return a new batch when it's called
        when( less.delete( same( appScope ), same( entityId ), any( UUID.class ) ) ).thenReturn( newBatch );


        cleanupTask.rejected();


        //get the second field, this should be deleted
        final UniqueValue oldUniqueField = uniqueValueEntryMock.getEntryAtIndex( 1 );

        final MvccLogEntry expectedDeletedEntry = logEntryMock.getEntryAtIndex( 1 );


        //verify delete was invoked
        verify( uvss ).delete( same( appScope ), same( oldUniqueField ) );

        //verify the delete was invoked
        verify( less ).delete( same( appScope ), same( entityId ), same( expectedDeletedEntry.getVersion() ) );

        // verify it was run
        verify( entityBatch ).execute();


        //the latch was executed
        latch.await();
    }


    private static class EntityVersionDeletedTest implements EntityVersionDeleted {
        final CountDownLatch invocationLatch;


        private EntityVersionDeletedTest( final CountDownLatch invocationLatch ) {
            this.invocationLatch = invocationLatch;
        }


        @Override
        public void versionDeleted( final ApplicationScope scope, final Id entityId,
                                    final List<MvccLogEntry> entityVersion ) {
            invocationLatch.countDown();
        }
    }


    private static class SlowListener extends EntityVersionDeletedTest {
        final Semaphore blockLatch;


        private SlowListener( final CountDownLatch invocationLatch, final Semaphore blockLatch ) {
            super( invocationLatch );
            this.blockLatch = blockLatch;
        }


        @Override
        public void versionDeleted( final ApplicationScope scope, final Id entityId,
                                    final List<MvccLogEntry> entityVersion ) {

            //wait for unblock to happen before counting down invocation latches
            try {
                blockLatch.acquire();
            }
            catch ( InterruptedException e ) {
                throw new RuntimeException( e );
            }
            super.versionDeleted( scope, entityId, entityVersion );
        }
    }
}
