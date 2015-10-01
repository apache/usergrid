/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.collection.mvcc.stage.delete;


import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class VersionCompactTest {

//    private static final TaskExecutor taskExecutor = new NamedTaskExecutorImpl( "test", 4, 0 );
//
//       @AfterClass
//       public static void shutdown() {
//           taskExecutor.shutdown();
//       }
//
//
//       @Test(timeout=10000)
//       public void noListener()
//               throws ExecutionException, InterruptedException, ConnectionException {
//
//           // create a latch for the event listener, and add it to the list of events
//
//           final Set<EntityVersionCreated> listeners = mock( Set.class );
//
//           when ( listeners.size()).thenReturn( 0 );
//
//           final Id applicationId = new SimpleId( "application" );
//
//           final ApplicationScope appScope = new ApplicationScopeImpl(applicationId);
//
//           final Id entityId = new SimpleId( "user" );
//           final Entity entity = new Entity( entityId );
//
//           // start the task
//
//           EntityVersionCreatedTask entityVersionCreatedTask =
//                   new EntityVersionCreatedTask( appScope, listeners, entity);
//
//           try {
//               entityVersionCreatedTask.call();
//           }catch(Exception e){
//               Assert.fail( e.getMessage() );
//           }
//
//
//           // wait for the task
//          // future.get();
//
//           //mocked listener makes sure that the task is called
//           verify( listeners ).size();
//
//       }
//       @Test(timeout=10000)
//       public void oneListener()
//               throws ExecutionException, InterruptedException, ConnectionException {
//
//           // create a latch for the event listener, and add it to the list of events
//
//           final int sizeToReturn = 1;
//
//           final CountDownLatch latch = new CountDownLatch( sizeToReturn );
//
//           final EntityVersionCreatedTest eventListener = new EntityVersionCreatedTest(latch);
//
//           final Set<EntityVersionCreated> listeners = mock( Set.class );
//           final Iterator<EntityVersionCreated> helper = mock(Iterator.class);
//
//           when ( listeners.size()).thenReturn( 1 );
//           when ( listeners.iterator()).thenReturn( helper );
//           when ( helper.next() ).thenReturn( eventListener );
//
//           final Id applicationId = new SimpleId( "application" );
//
//           final ApplicationScope appScope = new ApplicationScopeImpl(applicationId);
//
//           final Id entityId = new SimpleId( "user" );
//           final Entity entity = new Entity( entityId );
//
//           // start the task
//
//           EntityVersionCreatedTask entityVersionCreatedTask =
//               new EntityVersionCreatedTask( appScope, listeners, entity);
//
//           try {
//               entityVersionCreatedTask.call();
//           }catch(Exception e){
//
//               Assert.fail(e.getMessage());
//           }
//           //mocked listener makes sure that the task is called
//           verify( listeners ).size();
//           verify( listeners ).iterator();
//           verify( helper ).next();
//
//       }
//
//       @Test(timeout=10000)
//       public void multipleListener()
//               throws ExecutionException, InterruptedException, ConnectionException {
//
//           final int sizeToReturn = 3;
//
//           final Set<EntityVersionCreated> listeners = mock( Set.class );
//           final Iterator<EntityVersionCreated> helper = mock(Iterator.class);
//
//           when ( listeners.size()).thenReturn( 3 );
//           when ( listeners.iterator()).thenReturn( helper );
//
//           final Id applicationId = new SimpleId( "application" );
//
//           final ApplicationScope appScope = new ApplicationScopeImpl(applicationId);
//
//           final Id entityId = new SimpleId( "user" );
//           final Entity entity = new Entity( entityId );
//
//           // start the task
//
//           EntityVersionCreatedTask entityVersionCreatedTask =
//                   new EntityVersionCreatedTask( appScope, listeners, entity);
//
//           final CountDownLatch latch = new CountDownLatch( sizeToReturn );
//
//           final EntityVersionCreatedTest listener1 = new EntityVersionCreatedTest(latch);
//           final EntityVersionCreatedTest listener2 = new EntityVersionCreatedTest(latch);
//           final EntityVersionCreatedTest listener3 = new EntityVersionCreatedTest(latch);
//
//           when ( helper.next() ).thenReturn( listener1,listener2,listener3);
//
//           try {
//               entityVersionCreatedTask.call();
//           }catch(Exception e){
//               ;
//           }
//           //ListenableFuture<Void> future = taskExecutor.submit( entityVersionCreatedTask );
//
//           //wait for the task
//           //intentionally fails due to difficulty mocking observable
//
//           //mocked listener makes sure that the task is called
//           verify( listeners ).size();
//           //verifies that the observable made listener iterate.
//           verify( listeners ).iterator();
//       }
//
//       @Test(timeout=10000)
//       public void oneListenerRejected()
//               throws ExecutionException, InterruptedException, ConnectionException {
//
//           // create a latch for the event listener, and add it to the list of events
//
//           final TaskExecutor taskExecutor = new NamedTaskExecutorImpl( "test", 0, 0 );
//
//           final int sizeToReturn = 1;
//
//           final CountDownLatch latch = new CountDownLatch( sizeToReturn );
//
//           final EntityVersionCreatedTest eventListener = new EntityVersionCreatedTest(latch);
//
//           final Set<EntityVersionCreated> listeners = mock( Set.class );
//           final Iterator<EntityVersionCreated> helper = mock(Iterator.class);
//
//           when ( listeners.size()).thenReturn( 1 );
//           when ( listeners.iterator()).thenReturn( helper );
//           when ( helper.next() ).thenReturn( eventListener );
//
//           final Id applicationId = new SimpleId( "application" );
//
//           final ApplicationScope appScope = new ApplicationScopeImpl(applicationId);
//
//           final Id entityId = new SimpleId( "user" );
//           final Entity entity = new Entity( entityId );
//
//           // start the task
//
//           EntityVersionCreatedTask entityVersionCreatedTask =
//                   new EntityVersionCreatedTask( appScope, listeners, entity);
//
//           entityVersionCreatedTask.rejected();
//
//           //mocked listener makes sure that the task is called
//           verify( listeners ).size();
//           verify( listeners ).iterator();
//           verify( helper ).next();
//
//       }
//
//       private static class EntityVersionCreatedTest implements EntityVersionCreated {
//           final CountDownLatch invocationLatch;
//
//           private EntityVersionCreatedTest( final CountDownLatch invocationLatch) {
//               this.invocationLatch = invocationLatch;
//           }
//
//           @Override
//           public void versionCreated( final ApplicationScope scope, final Entity entity ) {
//               invocationLatch.countDown();
//           }
//       }
}
