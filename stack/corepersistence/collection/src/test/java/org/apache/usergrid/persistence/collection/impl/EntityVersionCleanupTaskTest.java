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


import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.Assert;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.event.EntityVersionDeleted;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.util.LogEntryMock;
import org.apache.usergrid.persistence.core.task.NamedTaskExecutorImpl;
import org.apache.usergrid.persistence.core.task.TaskExecutor;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import java.util.HashSet;
import java.util.Set;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.junit.Ignore;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.internal.util.collections.Sets;


/**
 * Cleanup task tests
 */
public class EntityVersionCleanupTaskTest {

    private static final TaskExecutor taskExecutor = new NamedTaskExecutorImpl( "test", 4, 0 );


    @AfterClass
    public static void shutdown() {
        taskExecutor.shutdown();
    }


    @Test(timeout=10000)
    public void noListenerOneVersion() 
            throws ExecutionException, InterruptedException, ConnectionException {


        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getBufferSize() ).thenReturn( 10 );

        final MvccEntitySerializationStrategy ess =
                mock( MvccEntitySerializationStrategy.class );

        final MvccLogEntrySerializationStrategy less =
                mock( MvccLogEntrySerializationStrategy.class );

        final Keyspace keyspace = mock( Keyspace.class );

        final MutationBatch entityBatch = mock( MutationBatch.class );

        final MutationBatch logBatch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() )
            .thenReturn( mock( MutationBatch.class ) ) // don't care what happens to this one
            .thenReturn( entityBatch )
            .thenReturn( logBatch );

        // intentionally no events
        final Set<EntityVersionDeleted> listeners = new HashSet<EntityVersionDeleted>();

        final Id applicationId = new SimpleId( "application" );

        final CollectionScope appScope = new CollectionScopeImpl( 
                applicationId, applicationId, "users" );

        final Id entityId = new SimpleId( "user" );


        // mock up a single log entry for our first test
        final LogEntryMock logEntryMock =
                LogEntryMock.createLogEntryMock(less, appScope, entityId, 2 );

        final UUID version = logEntryMock.getEntries().iterator().next().getVersion();

        final UniqueValueSerializationStrategy uvss =
                mock( UniqueValueSerializationStrategy.class );

        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig,
                        less,
                        ess,
                        uvss,
                        keyspace,
                        listeners,
                        appScope,
                        entityId,
                        version
                );

        final MutationBatch newBatch = mock( MutationBatch.class );


        // set up returning a mutator
        when(ess.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( newBatch );

        when(less.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( newBatch );

        final List<MvccEntity> mel = new ArrayList<MvccEntity>();

        mel.add( new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), 
                MvccEntity.Status.DELETED, Optional.fromNullable((Entity)null)) );

        mel.add( new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), 
                MvccEntity.Status.DELETED, Optional.fromNullable((Entity)null)) );

        when( ess.load( same( appScope ), same( entityId ), any(UUID.class), any(Integer.class) ) )
                .thenReturn(mel.iterator() );

        try {
            cleanupTask.call();
        }catch(Exception e){
            Assert.fail( e.getMessage() );
        }

        // verify it was run
        verify( entityBatch ).execute();

        verify( logBatch ).execute();
    }


    /**
     * Tests the cleanup task on the first version created
     */
    @Test(timeout=10000)
    public void noListenerNoVersions() 
            throws ExecutionException, InterruptedException, ConnectionException {


        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getBufferSize() ).thenReturn( 10 );

        final MvccEntitySerializationStrategy ess =
                mock( MvccEntitySerializationStrategy.class );

        final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy =
                mock( MvccLogEntrySerializationStrategy.class );

        final Keyspace keyspace = mock( Keyspace.class );


        final MutationBatch entityBatch = mock( MutationBatch.class );
        final MutationBatch logBatch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() )
            .thenReturn( mock( MutationBatch.class ) ) // don't care what happens to this one
            .thenReturn( entityBatch )
            .thenReturn( logBatch );



        //intentionally no events
        final Set<EntityVersionDeleted> listeners = new HashSet<EntityVersionDeleted>();

        final Id applicationId = new SimpleId( "application" );


        final CollectionScope appScope = new CollectionScopeImpl( 
                applicationId, applicationId, "users" );

        final Id entityId = new SimpleId( "user" );


        //mock up a single log entry for our first test
        final LogEntryMock logEntryMock = LogEntryMock.createLogEntryMock( 
                mvccLogEntrySerializationStrategy, appScope, entityId, 1 );


        final UUID version = logEntryMock.getEntries().iterator().next().getVersion();

        final UniqueValueSerializationStrategy uniqueValueSerializationStrategy =
                mock( UniqueValueSerializationStrategy.class );

        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig,
                        mvccLogEntrySerializationStrategy,
                        ess,
                        uniqueValueSerializationStrategy,
                        keyspace,
                        listeners,
                        appScope,
                        entityId,
                        version
                );

        final MutationBatch batch = mock( MutationBatch.class );


        //set up returning a mutator
        when(ess.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( batch );

        when( mvccLogEntrySerializationStrategy
                .delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( batch );

        final List<MvccEntity> mel = new ArrayList<MvccEntity>();

        mel.add( new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), 
                MvccEntity.Status.DELETED, Optional.fromNullable((Entity)null)) );

        mel.add( new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), 
                MvccEntity.Status.DELETED, Optional.fromNullable((Entity)null)) );

        when( ess.load( same( appScope ), same( entityId ), any(UUID.class), any(Integer.class) ) )
                .thenReturn(mel.iterator() );

        //start the task
        try {
            cleanupTask.call();
        }catch(Exception e){
            Assert.fail( e.getMessage() );
        }


        // These last two verify statements do not make sense. We cannot assert that the entity
        // and log batches are never called. Even if there are no listeners the entity delete 
        // cleanup task will still run to do the normal cleanup.
        //
        // verify( entityBatch, never() ).execute();
        // verify( logBatch, never() ).execute();
    }


    @Test(timeout=10000)
    public void singleListenerSingleVersion() 
            throws ExecutionException, InterruptedException, ConnectionException {


        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getBufferSize() ).thenReturn( 10 );

        final MvccEntitySerializationStrategy ess =
                mock( MvccEntitySerializationStrategy.class );

        final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy =
                mock( MvccLogEntrySerializationStrategy.class );

        final Keyspace keyspace = mock( Keyspace.class );


        final MutationBatch entityBatch = mock( MutationBatch.class );
        final MutationBatch logBatch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() )
            .thenReturn( mock( MutationBatch.class ) ) // don't care what happens to this one
            .thenReturn( entityBatch )
            .thenReturn( logBatch );



        //create a latch for the event listener, and add it to the list of events
        final int sizeToReturn = 1;

        final CountDownLatch latch = new CountDownLatch( sizeToReturn );

        final EntityVersionDeletedTest eventListener = new EntityVersionDeletedTest( latch );

        final Set<EntityVersionDeleted> listeners = new HashSet<EntityVersionDeleted>();

        listeners.add( eventListener );

        final Id applicationId = new SimpleId( "application" );


        final CollectionScope appScope = new CollectionScopeImpl( 
                applicationId, applicationId, "users" );

        final Id entityId = new SimpleId( "user" );


        //mock up a single log entry for our first test
        final LogEntryMock logEntryMock = LogEntryMock.createLogEntryMock( 
                mvccLogEntrySerializationStrategy, appScope, entityId, sizeToReturn + 1 );


        final UUID version = logEntryMock.getEntries().iterator().next().getVersion();


        final UniqueValueSerializationStrategy uniqueValueSerializationStrategy =
                mock( UniqueValueSerializationStrategy.class );

        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig,
                        mvccLogEntrySerializationStrategy,
                        ess,
                        uniqueValueSerializationStrategy,
                        keyspace,
                        listeners,
                        appScope,
                        entityId,
                        version
                );

        final MutationBatch batch = mock( MutationBatch.class );


        //set up returning a mutator
        when(ess.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( batch );


        when( mvccLogEntrySerializationStrategy
                .delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( batch );


        final List<MvccEntity> mel = new ArrayList<MvccEntity>();

        mel.add( new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), 
                MvccEntity.Status.DELETED, Optional.fromNullable((Entity)null)) );

        mel.add( new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), 
                MvccEntity.Status.DELETED, Optional.fromNullable((Entity)null)) );

        when( ess.load( same( appScope ), same( entityId ), any(UUID.class), any(Integer.class) ) )
                .thenReturn(mel.iterator() );


        try {
            cleanupTask.call();
        }catch(Exception e){
            Assert.fail( e.getMessage() );
        }

        //we deleted the version
        //verify it was run
        verify( entityBatch ).execute();

        verify( logBatch ).execute();

        //the latch was executed
        latch.await();
    }


    @Test//(timeout=10000)
    public void multipleListenerMultipleVersions()
            throws ExecutionException, InterruptedException, ConnectionException {


        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getBufferSize() ).thenReturn( 10 );

        final MvccEntitySerializationStrategy ess =
                mock( MvccEntitySerializationStrategy.class );

        final UniqueValueSerializationStrategy uniqueValueSerializationStrategy =
                mock( UniqueValueSerializationStrategy.class );

        final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy =
                mock( MvccLogEntrySerializationStrategy.class );

        final Keyspace keyspace = mock( Keyspace.class );

        final MutationBatch entityBatch = mock( MutationBatch.class );
        final MutationBatch logBatch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() )
            .thenReturn( mock( MutationBatch.class ) ) // don't care what happens to this one
            .thenReturn( entityBatch )
            .thenReturn( logBatch );


        //create a latch for the event listener, and add it to the list of events
        final int sizeToReturn = 10;


        final CountDownLatch latch = new CountDownLatch( 
                sizeToReturn/serializationFig.getBufferSize() * 3 );

        final EntityVersionDeletedTest listener1 = new EntityVersionDeletedTest( latch );
        final EntityVersionDeletedTest listener2 = new EntityVersionDeletedTest( latch );
        final EntityVersionDeletedTest listener3 = new EntityVersionDeletedTest( latch );

        final Set<EntityVersionDeleted> listeners = new HashSet<EntityVersionDeleted>();

        listeners.add( listener1 );
        listeners.add( listener2 );
        listeners.add( listener3 );

        final Id applicationId = new SimpleId( "application" );

        final CollectionScope appScope = new CollectionScopeImpl( 
                applicationId, applicationId, "users" );

        final Id entityId = new SimpleId( "user" );

        // mock up a single log entry for our first test
        final LogEntryMock logEntryMock = LogEntryMock.createLogEntryMock( 
                mvccLogEntrySerializationStrategy, appScope, entityId, sizeToReturn + 1 );

        final UUID version = logEntryMock.getEntries().iterator().next().getVersion();

        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig,
                        mvccLogEntrySerializationStrategy,
                        ess,
                        uniqueValueSerializationStrategy,
                        keyspace,
                        listeners,
                        appScope,
                        entityId,
                        version
                );

        final MutationBatch batch = mock( MutationBatch.class );


        //set up returning a mutator
        when( ess.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( batch );

        when( mvccLogEntrySerializationStrategy
                .delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( batch );

        final List<MvccEntity> mel = new ArrayList<MvccEntity>();

        Entity entity = new Entity( entityId );

        mel.add( new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), 
                MvccEntity.Status.DELETED, Optional.of(entity)) );

        mel.add( new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(), 
                MvccEntity.Status.DELETED, Optional.of(entity)) );

        when( ess.load( same( appScope ), same( entityId ), any(UUID.class), any(Integer.class) ) )
                .thenReturn(mel.iterator() );

        try {
            cleanupTask.call();
        }catch(Exception e){
            Assert.fail( e.getMessage() );
        }
        //we deleted the version
        //verify we deleted everything
        verify( entityBatch, times( 1 ) ).mergeShallow( any( MutationBatch.class ) );

        verify( logBatch, times( 1 ) ).mergeShallow( any( MutationBatch.class ) );

        verify( logBatch ).execute();

        verify( entityBatch ).execute();

        //the latch was executed
        latch.await();
    }


    /**
     * Tests what happens when our listeners are VERY slow
     */
    @Ignore("Test is a work in progress")
    @Test(timeout=10000)
    public void multipleListenerMultipleVersionsNoThreadsToRun()
            throws ExecutionException, InterruptedException, ConnectionException {


        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getBufferSize() ).thenReturn( 10 );

        final MvccEntitySerializationStrategy mvccEntitySerializationStrategy =
                mock( MvccEntitySerializationStrategy.class );

        final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy =
                mock( MvccLogEntrySerializationStrategy.class );

        final Keyspace keyspace = mock( Keyspace.class );



        final MutationBatch entityBatch = mock( MutationBatch.class );
        final MutationBatch logBatch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() )
            .thenReturn( mock( MutationBatch.class ) ) // don't care what happens to this one
            .thenReturn( entityBatch )
            .thenReturn( logBatch );




        //create a latch for the event listener, and add it to the list of events
        final int sizeToReturn = 10;


        final int listenerCount = 5;

        final CountDownLatch latch = new CountDownLatch( 
                sizeToReturn/serializationFig.getBufferSize() * listenerCount );
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

        final Id applicationId = new SimpleId( "application" );


        final CollectionScope appScope = new CollectionScopeImpl( 
                applicationId, applicationId, "users" );

        final Id entityId = new SimpleId( "user" );


        //mock up a single log entry for our first test
        final LogEntryMock logEntryMock = LogEntryMock.createLogEntryMock( 
                mvccLogEntrySerializationStrategy, appScope, entityId, sizeToReturn + 1 );


        final UUID version = logEntryMock.getEntries().iterator().next().getVersion();


        final UniqueValueSerializationStrategy uniqueValueSerializationStrategy =
                mock( UniqueValueSerializationStrategy.class );

        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig,
                        mvccLogEntrySerializationStrategy,
                        mvccEntitySerializationStrategy,
                        uniqueValueSerializationStrategy,
                        keyspace,
                        listeners,
                        appScope,
                        entityId,
                        version
                );

        final MutationBatch batch = mock( MutationBatch.class );


        //set up returning a mutator
        when( mvccEntitySerializationStrategy
                .delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( batch );


        when( mvccLogEntrySerializationStrategy
                .delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( batch );


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



        //we deleted the version
        //verify we deleted everything
        verify( logBatch, times( sizeToReturn ) ).mergeShallow( any( MutationBatch.class ) );

        verify( entityBatch, times( sizeToReturn ) ).mergeShallow( any( MutationBatch.class ) );


        verify( logBatch ).execute();

        verify( entityBatch ).execute();



        //the latch was executed
        latch.await();
    }

    /**
     * Tests that our task will run in the caller if there's no threads, ensures that the task runs
     */
    @Test(timeout=10000)
    public void singleListenerSingleVersionRejected()
            throws ExecutionException, InterruptedException, ConnectionException {


        final TaskExecutor taskExecutor = new NamedTaskExecutorImpl( "test", 0, 0 );

        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getBufferSize() ).thenReturn( 10 );

        final MvccEntitySerializationStrategy ess =
                mock( MvccEntitySerializationStrategy.class );

        final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy =
                mock( MvccLogEntrySerializationStrategy.class );

        final Keyspace keyspace = mock( Keyspace.class );


        final MutationBatch entityBatch = mock( MutationBatch.class );
        final MutationBatch logBatch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() )
                .thenReturn( mock( MutationBatch.class ) ) // don't care what happens to this one
                .thenReturn( entityBatch )
                .thenReturn( logBatch );



        //create a latch for the event listener, and add it to the list of events
        final int sizeToReturn = 1;

        final CountDownLatch latch = new CountDownLatch( sizeToReturn );

        final EntityVersionDeletedTest eventListener = new EntityVersionDeletedTest( latch );

        final Set<EntityVersionDeleted> listeners = new HashSet<EntityVersionDeleted>();

        listeners.add( eventListener );

        final Id applicationId = new SimpleId( "application" );


        final CollectionScope appScope = new CollectionScopeImpl(
                applicationId, applicationId, "users" );

        final Id entityId = new SimpleId( "user" );


        //mock up a single log entry for our first test
        final LogEntryMock logEntryMock = LogEntryMock.createLogEntryMock(
                mvccLogEntrySerializationStrategy, appScope, entityId, sizeToReturn + 1 );


        final UUID version = logEntryMock.getEntries().iterator().next().getVersion();


        final UniqueValueSerializationStrategy uniqueValueSerializationStrategy =
                mock( UniqueValueSerializationStrategy.class );

        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig,
                        mvccLogEntrySerializationStrategy,
                        ess,
                        uniqueValueSerializationStrategy,
                        keyspace,
                        listeners,
                        appScope,
                        entityId,
                        version
                );

        final MutationBatch batch = mock( MutationBatch.class );


        //set up returning a mutator
        when(ess.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( batch );


        when( mvccLogEntrySerializationStrategy
                .delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( batch );


        final List<MvccEntity> mel = new ArrayList<MvccEntity>();

        mel.add( new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(),
                MvccEntity.Status.DELETED, Optional.fromNullable((Entity)null)) );

        mel.add( new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(),
                MvccEntity.Status.DELETED, Optional.fromNullable((Entity)null)) );

        when( ess.load( same( appScope ), same( entityId ), any(UUID.class), any(Integer.class) ) )
                .thenReturn(mel.iterator() );


        try {
            cleanupTask.rejected();
        }catch(Exception e){
            Assert.fail(e.getMessage());
        }

        //we deleted the version
        //verify it was run
        verify( entityBatch ).execute();

        verify( logBatch ).execute();

        //the latch was executed
        latch.await();
    }

    private static class EntityVersionDeletedTest implements EntityVersionDeleted {
        final CountDownLatch invocationLatch;


        private EntityVersionDeletedTest( final CountDownLatch invocationLatch ) {
            this.invocationLatch = invocationLatch;
        }


        @Override
        public void versionDeleted( final CollectionScope scope, final Id entityId, 
                final List<MvccEntity> entityVersion ) {
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
        public void versionDeleted( final CollectionScope scope, final Id entityId, 
                final List<MvccEntity> entityVersion ) {

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
