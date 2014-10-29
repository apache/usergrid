package org.apache.usergrid.persistence.collection.impl;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import org.junit.AfterClass;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityVersionCreatedFactory;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.event.EntityVersionCreated;
import org.apache.usergrid.persistence.collection.event.EntityVersionDeleted;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.util.LogEntryMock;
import org.apache.usergrid.persistence.core.task.NamedTaskExecutorImpl;
import org.apache.usergrid.persistence.core.task.TaskExecutor;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Created task tests.
 */
public class EntityVersionCreatedTaskTest {

    private static final TaskExecutor taskExecutor = new NamedTaskExecutorImpl( "test", 4, 0 );

    @AfterClass
    public static void shutdown() {
        taskExecutor.shutdown();
    }

    @Test(timeout=10000)
    public void noListener()//why does it matter if it has a version or not without a listener
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

        final EntityVersionCreatedFactory entityVersionCreatedFactory = mock(EntityVersionCreatedFactory.class);

        when( keyspace.prepareMutationBatch() )
                .thenReturn( mock( MutationBatch.class ) ) // don't care what happens to this one
                .thenReturn( entityBatch )
                .thenReturn( logBatch );

        // intentionally no events
        final Set<EntityVersionCreated> listeners = mock( Set.class );//new HashSet<EntityVersionCreated>();

        when ( listeners.size()).thenReturn( 0 );

        final Id applicationId = new SimpleId( "application" );

        final CollectionScope appScope = new CollectionScopeImpl(
                applicationId, applicationId, "users" );

        final Id entityId = new SimpleId( "user" );

        final Entity entity = new Entity( entityId );

        final MvccEntity mvccEntity = new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(),
                MvccEntity.Status.COMPLETE,entity );


        // mock up a single log entry for our first test
        final LogEntryMock logEntryMock =
                LogEntryMock.createLogEntryMock(less, appScope, entityId, 2 );

        final UUID version = logEntryMock.getEntries().iterator().next().getVersion();

        final UniqueValueSerializationStrategy uvss =
                mock( UniqueValueSerializationStrategy.class );

        EntityVersionCreatedTask entityVersionCreatedTask =
                new EntityVersionCreatedTask(entityVersionCreatedFactory,
                serializationFig,
                        less,
                        ess,
                        uvss,
                        keyspace,
                        appScope,
                        listeners,
                        entity);

        // start the task
        ListenableFuture<Void> future = taskExecutor.submit( entityVersionCreatedTask );

        // wait for the task
        future.get();

        //mocked listener makes sure that the task is called
        verify( listeners ).size();

    }

    @Test//(timeout=10000)
    public void oneListener()//why does it matter if it has a version or not without a listener
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

        final EntityVersionCreatedFactory entityVersionCreatedFactory = mock(EntityVersionCreatedFactory.class);

        when( keyspace.prepareMutationBatch() )
                .thenReturn( mock( MutationBatch.class ) ) // don't care what happens to this one
                .thenReturn( entityBatch )
                .thenReturn( logBatch );

        // intentionally no events

        //create a latch for the event listener, and add it to the list of events
        final int sizeToReturn = 1;

        final CountDownLatch latch = new CountDownLatch( sizeToReturn );

        final EntityVersionCreatedTest eventListener = new EntityVersionCreatedTest(latch);

        final Set<EntityVersionCreated> listeners = mock( Set.class );//new HashSet<EntityVersionCreated>();

        final Iterator<EntityVersionCreated> helper = mock(Iterator.class);


        when ( listeners.size()).thenReturn( 1 );
        when ( listeners.iterator()).thenReturn( helper );
        when ( helper.next() ).thenReturn( eventListener );

        final Id applicationId = new SimpleId( "application" );

        final CollectionScope appScope = new CollectionScopeImpl(
                applicationId, applicationId, "users" );

        final Id entityId = new SimpleId( "user" );

        final Entity entity = new Entity( entityId );

        final MvccEntity mvccEntity = new MvccEntityImpl( entityId, UUIDGenerator.newTimeUUID(),
                MvccEntity.Status.COMPLETE,entity );


        // mock up a single log entry for our first test
        final LogEntryMock logEntryMock =
                LogEntryMock.createLogEntryMock(less, appScope, entityId, 2 );

        final UUID version = logEntryMock.getEntries().iterator().next().getVersion();

        final UniqueValueSerializationStrategy uvss =
                mock( UniqueValueSerializationStrategy.class );

        //when ( listeners.iterator().next().versionCreated(appScope,entity));


                EntityVersionCreatedTask entityVersionCreatedTask =
                new EntityVersionCreatedTask(entityVersionCreatedFactory,
                        serializationFig,
                        less,
                        ess,
                        uvss,
                        keyspace,
                        appScope,
                        listeners,
                        entity);

        // start the task
        ListenableFuture<Void> future = taskExecutor.submit( entityVersionCreatedTask );

        // wait for the task
        future.get();

        //mocked listener makes sure that the task is called
        verify( listeners ).size();
        verify( listeners ).iterator();
        verify( helper ).next();

    }

    private static class EntityVersionCreatedTest implements EntityVersionCreated {
        final CountDownLatch invocationLatch;

        private EntityVersionCreatedTest( final CountDownLatch invocationLatch) {
            this.invocationLatch = invocationLatch;
        }

        @Override
        public void versionCreated( final CollectionScope scope, final Entity entity ) {
            invocationLatch.countDown();
        }
    }


//    private static class SlowListener extends EntityVersionCreatedTest {
//        final Semaphore blockLatch;
//
//        private SlowListener( final CountDownLatch invocationLatch, final Semaphore blockLatch ) {
//            super( invocationLatch );
//            this.blockLatch = blockLatch;
//        }
//
//
//        @Override
//        public void versionDeleted( final CollectionScope scope, final Id entityId,
//                                    final List<MvccEntity> entityVersion ) {
//
//            //wait for unblock to happen before counting down invocation latches
//            try {
//                blockLatch.acquire();
//            }
//            catch ( InterruptedException e ) {
//                throw new RuntimeException( e );
//            }
//            super.versionDeleted( scope, entityId, entityVersion );
//        }
//    }

}
