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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

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
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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


    @Test
    public void noListenerOneVersion() throws ExecutionException, InterruptedException, ConnectionException {


        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getHistorySize() ).thenReturn( 10 );

        final MvccEntitySerializationStrategy mvccEntitySerializationStrategy =
                mock( MvccEntitySerializationStrategy.class );

        final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy =
                mock( MvccLogEntrySerializationStrategy.class );


        //intentionally no events
        final List<EntityVersionDeleted> listeners = new ArrayList<EntityVersionDeleted>();

        final Id applicationId = new SimpleId( "application" );


        final CollectionScope appScope = new CollectionScopeImpl( applicationId, applicationId, "users" );

        final Id entityId = new SimpleId( "user" );


        //mock up a single log entry for our first test
        final LogEntryMock logEntryMock =
                LogEntryMock.createLogEntryMock( mvccLogEntrySerializationStrategy, appScope, entityId, 2 );


        final UUID version = logEntryMock.getEntries().iterator().next().getVersion();


        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig, mvccLogEntrySerializationStrategy,
                        mvccEntitySerializationStrategy, listeners, appScope, entityId, version );

        final MutationBatch firstBatch = mock( MutationBatch.class );


        //set up returning a mutator
        when( mvccEntitySerializationStrategy.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( firstBatch );


        final MutationBatch secondBatch = mock( MutationBatch.class );

        when( mvccLogEntrySerializationStrategy.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( secondBatch );


        //start the task
        ListenableFuture<Void> future = taskExecutor.submit( cleanupTask );

        //wait for the task
        future.get();

        //verify it was run
        verify( firstBatch ).execute();

        verify( secondBatch ).execute();
    }


    /**
     * Tests the cleanup task on the first version ceated
     */
    @Test
    public void noListenerNoVersions() throws ExecutionException, InterruptedException, ConnectionException {


        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getHistorySize() ).thenReturn( 10 );

        final MvccEntitySerializationStrategy mvccEntitySerializationStrategy =
                mock( MvccEntitySerializationStrategy.class );

        final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy =
                mock( MvccLogEntrySerializationStrategy.class );


        //intentionally no events
        final List<EntityVersionDeleted> listeners = new ArrayList<EntityVersionDeleted>();

        final Id applicationId = new SimpleId( "application" );


        final CollectionScope appScope = new CollectionScopeImpl( applicationId, applicationId, "users" );

        final Id entityId = new SimpleId( "user" );


        //mock up a single log entry for our first test
        final LogEntryMock logEntryMock =
                LogEntryMock.createLogEntryMock( mvccLogEntrySerializationStrategy, appScope, entityId, 1 );


        final UUID version = logEntryMock.getEntries().iterator().next().getVersion();


        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig, mvccLogEntrySerializationStrategy,
                        mvccEntitySerializationStrategy, listeners, appScope, entityId, version );

        final MutationBatch firstBatch = mock( MutationBatch.class );


        //set up returning a mutator
        when( mvccEntitySerializationStrategy.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( firstBatch );


        final MutationBatch secondBatch = mock( MutationBatch.class );

        when( mvccLogEntrySerializationStrategy.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( secondBatch );


        //start the task
        ListenableFuture<Void> future = taskExecutor.submit( cleanupTask );

        //wait for the task
        future.get();

        //verify it was run
        verify( firstBatch, never() ).execute();

        verify( secondBatch, never() ).execute();
    }


    @Test
    public void singleListenerSingleVersion() throws ExecutionException, InterruptedException, ConnectionException {


        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getHistorySize() ).thenReturn( 10 );

        final MvccEntitySerializationStrategy mvccEntitySerializationStrategy =
                mock( MvccEntitySerializationStrategy.class );

        final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy =
                mock( MvccLogEntrySerializationStrategy.class );


        //create a latch for the event listener, and add it to the list of events
        final int sizeToReturn = 1;

        final CountDownLatch latch = new CountDownLatch( sizeToReturn );

        final EntityVersionDeletedTest eventListener = new EntityVersionDeletedTest( latch );

        final List<EntityVersionDeleted> listeners = new ArrayList<EntityVersionDeleted>();

        listeners.add( eventListener );

        final Id applicationId = new SimpleId( "application" );


        final CollectionScope appScope = new CollectionScopeImpl( applicationId, applicationId, "users" );

        final Id entityId = new SimpleId( "user" );


        //mock up a single log entry for our first test
        final LogEntryMock logEntryMock = LogEntryMock
                .createLogEntryMock( mvccLogEntrySerializationStrategy, appScope, entityId, sizeToReturn + 1 );


        final UUID version = logEntryMock.getEntries().iterator().next().getVersion();


        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig, mvccLogEntrySerializationStrategy,
                        mvccEntitySerializationStrategy, listeners, appScope, entityId, version );

        final MutationBatch firstBatch = mock( MutationBatch.class );


        //set up returning a mutator
        when( mvccEntitySerializationStrategy.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( firstBatch );


        final MutationBatch secondBatch = mock( MutationBatch.class );

        when( mvccLogEntrySerializationStrategy.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( secondBatch );


        //start the task
        ListenableFuture<Void> future = taskExecutor.submit( cleanupTask );

        //wait for the task
        future.get();

        //we deleted the version
        //verify it was run
        verify( firstBatch ).execute();

        verify( secondBatch ).execute();

        //the latch was executed
        latch.await();
    }


    @Test
    public void multipleListenerMultipleVersions()
            throws ExecutionException, InterruptedException, ConnectionException {


        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getHistorySize() ).thenReturn( 10 );

        final MvccEntitySerializationStrategy mvccEntitySerializationStrategy =
                mock( MvccEntitySerializationStrategy.class );

        final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy =
                mock( MvccLogEntrySerializationStrategy.class );


        //create a latch for the event listener, and add it to the list of events
        final int sizeToReturn = 10;


        final CountDownLatch latch = new CountDownLatch( sizeToReturn * 3 );

        final EntityVersionDeletedTest listener1 = new EntityVersionDeletedTest( latch );
        final EntityVersionDeletedTest listener2 = new EntityVersionDeletedTest( latch );
        final EntityVersionDeletedTest listener3 = new EntityVersionDeletedTest( latch );

        final List<EntityVersionDeleted> listeners = new ArrayList<EntityVersionDeleted>();

        listeners.add( listener1 );
        listeners.add( listener2 );
        listeners.add( listener3 );

        final Id applicationId = new SimpleId( "application" );


        final CollectionScope appScope = new CollectionScopeImpl( applicationId, applicationId, "users" );

        final Id entityId = new SimpleId( "user" );


        //mock up a single log entry for our first test
        final LogEntryMock logEntryMock = LogEntryMock
                .createLogEntryMock( mvccLogEntrySerializationStrategy, appScope, entityId, sizeToReturn + 1 );


        final UUID version = logEntryMock.getEntries().iterator().next().getVersion();


        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig, mvccLogEntrySerializationStrategy,
                        mvccEntitySerializationStrategy, listeners, appScope, entityId, version );

        final MutationBatch firstBatch = mock( MutationBatch.class );


        //set up returning a mutator
        when( mvccEntitySerializationStrategy.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( firstBatch );


        final MutationBatch secondBatch = mock( MutationBatch.class );

        when( mvccLogEntrySerializationStrategy.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( secondBatch );


        //start the task
        ListenableFuture<Void> future = taskExecutor.submit( cleanupTask );

        //wait for the task
        future.get();

        //we deleted the version
        //verify we deleted everything
        verify( firstBatch, times( sizeToReturn ) ).execute();

        verify( secondBatch, times( sizeToReturn ) ).execute();

        //the latch was executed
        latch.await();
    }


    /**
     * Tests what happens when our listeners are VERY slow
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws ConnectionException
     */
    @Test
    public void multipleListenerMultipleVersionsNoThreadsToRun()
            throws ExecutionException, InterruptedException, ConnectionException {


        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getHistorySize() ).thenReturn( 10 );

        final MvccEntitySerializationStrategy mvccEntitySerializationStrategy =
                mock( MvccEntitySerializationStrategy.class );

        final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy =
                mock( MvccLogEntrySerializationStrategy.class );


        //create a latch for the event listener, and add it to the list of events
        final int sizeToReturn = 10;


        final int listenerCount = 5;

        final CountDownLatch latch = new CountDownLatch( sizeToReturn * listenerCount );
        final Semaphore waitSemaphore = new Semaphore( 0 );


        final SlowListener listener1 = new SlowListener( latch, waitSemaphore );
        final SlowListener listener2 = new SlowListener( latch, waitSemaphore );
        final SlowListener listener3 = new SlowListener( latch, waitSemaphore );
        final SlowListener listener4 = new SlowListener( latch, waitSemaphore );
        final SlowListener listener5 = new SlowListener( latch, waitSemaphore );

        final List<EntityVersionDeleted> listeners = new ArrayList<>();

        listeners.add( listener1 );
        listeners.add( listener2 );
        listeners.add( listener3 );
        listeners.add( listener4 );
        listeners.add( listener5 );

        final Id applicationId = new SimpleId( "application" );


        final CollectionScope appScope = new CollectionScopeImpl( applicationId, applicationId, "users" );

        final Id entityId = new SimpleId( "user" );


        //mock up a single log entry for our first test
        final LogEntryMock logEntryMock = LogEntryMock
                .createLogEntryMock( mvccLogEntrySerializationStrategy, appScope, entityId, sizeToReturn + 1 );


        final UUID version = logEntryMock.getEntries().iterator().next().getVersion();


        EntityVersionCleanupTask cleanupTask =
                new EntityVersionCleanupTask( serializationFig, mvccLogEntrySerializationStrategy,
                        mvccEntitySerializationStrategy, listeners, appScope, entityId, version );

        final MutationBatch firstBatch = mock( MutationBatch.class );


        //set up returning a mutator
        when( mvccEntitySerializationStrategy.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( firstBatch );


        final MutationBatch secondBatch = mock( MutationBatch.class );

        when( mvccLogEntrySerializationStrategy.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( secondBatch );


        //start the task
        ListenableFuture<Void> future = taskExecutor.submit( cleanupTask );

        /**
         * While we're not done, release latches every 200 ms
         */
        while(!future.isDone()) {
            Thread.sleep( 200 );
            waitSemaphore.release( listenerCount );
        }

        //wait for the task
        future.get();

        //we deleted the version
        //verify we deleted everything
        verify( firstBatch, times( sizeToReturn ) ).execute();

        verify( secondBatch, times( sizeToReturn ) ).execute();

        //the latch was executed
        latch.await();
    }



    /**
     * Tests that our task will run in the caller if there's no threads, ensures that the task runs
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws ConnectionException
     */
    @Test
    public void runsWhenRejected()
            throws ExecutionException, InterruptedException, ConnectionException {


        /**
         * only 1 thread on purpose, we want to saturate the task
         */
        final TaskExecutor taskExecutor = new NamedTaskExecutorImpl( "test", 1, 0);

        final SerializationFig serializationFig = mock( SerializationFig.class );

        when( serializationFig.getHistorySize() ).thenReturn( 10 );

        final MvccEntitySerializationStrategy mvccEntitySerializationStrategy =
                mock( MvccEntitySerializationStrategy.class );

        final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy =
                mock( MvccLogEntrySerializationStrategy.class );


        //create a latch for the event listener, and add it to the list of events
        final int sizeToReturn = 10;


        final int listenerCount = 2;

        final CountDownLatch latch = new CountDownLatch( sizeToReturn * listenerCount );
        final Semaphore waitSemaphore = new Semaphore( 0 );


        final SlowListener slowListener = new SlowListener( latch, waitSemaphore );
        final EntityVersionDeletedTest runListener = new EntityVersionDeletedTest( latch );



        final Id applicationId = new SimpleId( "application" );


        final CollectionScope appScope = new CollectionScopeImpl( applicationId, applicationId, "users" );

        final Id entityId = new SimpleId( "user" );


        //mock up a single log entry for our first test
        final LogEntryMock logEntryMock = LogEntryMock
                .createLogEntryMock( mvccLogEntrySerializationStrategy, appScope, entityId, sizeToReturn + 1 );


        final UUID version = logEntryMock.getEntries().iterator().next().getVersion();


        EntityVersionCleanupTask firstTask =
                new EntityVersionCleanupTask( serializationFig, mvccLogEntrySerializationStrategy,
                        mvccEntitySerializationStrategy, Arrays.<EntityVersionDeleted>asList(slowListener), appScope, entityId, version );



        //change the listeners to one that is just invoked quickly


        EntityVersionCleanupTask secondTask =
                      new EntityVersionCleanupTask( serializationFig, mvccLogEntrySerializationStrategy,
                              mvccEntitySerializationStrategy, Arrays.<EntityVersionDeleted>asList(runListener), appScope, entityId, version );


        final MutationBatch firstBatch = mock( MutationBatch.class );


        //set up returning a mutator
        when( mvccEntitySerializationStrategy.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( firstBatch );


        final MutationBatch secondBatch = mock( MutationBatch.class );

        when( mvccLogEntrySerializationStrategy.delete( same( appScope ), same( entityId ), any( UUID.class ) ) )
                .thenReturn( secondBatch );


        //start the task
        ListenableFuture<Void> future1 =  taskExecutor.submit( firstTask );

        //now start another task while the slow running task is running
        ListenableFuture<Void> future2 =  taskExecutor.submit( secondTask );

        //get the second task, we shouldn't have been able to queue it, therefore it should just run in process
        future2.get();

        /**
         * While we're not done, release latches every 200 ms
         */
        while(!future1.isDone()) {
            Thread.sleep( 200 );
            waitSemaphore.release( listenerCount );
        }

        //wait for the task
        future1.get();

        //we deleted the version
        //verify we deleted everything
        verify( firstBatch, times( sizeToReturn*2 ) ).execute();

        verify( secondBatch, times( sizeToReturn*2 ) ).execute();

        //the latch was executed
        latch.await();
    }


    private static class EntityVersionDeletedTest implements EntityVersionDeleted {
        final CountDownLatch invocationLatch;


        private EntityVersionDeletedTest( final CountDownLatch invocationLatch ) {
            this.invocationLatch = invocationLatch;
        }


        @Override
        public void versionDeleted( final CollectionScope scope, final Id entityId, final UUID entityVersion ) {
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
        public void versionDeleted( final CollectionScope scope, final Id entityId, final UUID entityVersion ) {
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
