/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.collection.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.event.EntityVersionCreated;
import org.apache.usergrid.persistence.core.task.NamedTaskExecutorImpl;
import org.apache.usergrid.persistence.core.task.TaskExecutor;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.junit.AfterClass;
import org.junit.Test;
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
    public void oneListener()
            throws ExecutionException, InterruptedException, ConnectionException {

        // create a latch for the event listener, and add it to the list of events

        final int sizeToReturn = 1;

        final CountDownLatch latch = new CountDownLatch( sizeToReturn );

        final EntityVersionCreatedTest eventListener = new EntityVersionCreatedTest(latch);

        final Set<EntityVersionCreated> listeners = mock( Set.class );
        final Iterator<EntityVersionCreated> helper = mock(Iterator.class);

        when ( listeners.size()).thenReturn( 1 );
        when ( listeners.iterator()).thenReturn( helper );
        when ( helper.next() ).thenReturn( eventListener );

        final Id applicationId = new SimpleId( "application" );

        final CollectionScope appScope = new CollectionScopeImpl(
                applicationId, applicationId, "users" );

        final Id entityId = new SimpleId( "user" );
        final Entity entity = new Entity( entityId );

        // start the task

        EntityVersionCreatedTask entityVersionCreatedTask =
            new EntityVersionCreatedTask( appScope, listeners, entity);

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
