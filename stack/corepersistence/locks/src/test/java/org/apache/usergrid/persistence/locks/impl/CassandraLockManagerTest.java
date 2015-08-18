/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.persistence.locks.impl;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.locks.Lock;
import org.apache.usergrid.persistence.locks.LockId;
import org.apache.usergrid.persistence.locks.guice.TestLockModule;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;

import rx.Observable;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Tests the cassandra lock manager implementation
 */
@RunWith( ITRunner.class )
@UseModules( { TestLockModule.class } )
public class CassandraLockManagerTest {

    private static final Logger logger = LoggerFactory.getLogger( CassandraLockManagerTest.class );

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected CassandraLockManager cassandraLockManager;


    protected ApplicationScope scope;


    private static final int ONE_HOUR_TTL = 1;

    private static final TimeUnit HOURS = TimeUnit.HOURS;


    @Before
    public void setup() {
        scope = mock( ApplicationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getApplication() ).thenReturn( orgId );
    }


    @Test
    public void testConcurrency() {

        final int numConcurrent = 10;

        final LockId sharedLock = createLockId();

        final CountDownLatch countDownLatch = new CountDownLatch( numConcurrent );


        final LockResults lockResults = Observable.range( 0, numConcurrent ).flatMap( input -> {
            return Observable.just( input ).map( intValue -> {

                try {
                    countDownLatch.countDown();
                    countDownLatch.await();
                }
                catch ( InterruptedException e ) {
                    throw new RuntimeException( "Unable to countdown latch" );
                }

                final Lock lockId = cassandraLockManager.createMultiRegionLock( sharedLock );

                final LockTuple lockTuple = new LockTuple( intValue, lockId, lockId.tryLock( ONE_HOUR_TTL, HOURS ) );


                logger.info( "Result of lock index {} is {}", lockTuple.indexId, lockTuple.hasLock );

                return lockTuple;
            } ).subscribeOn( Schedulers.io() );
        }, numConcurrent ).collect( () -> new LockResults(), ( results, lockTuple ) -> results.addLock( lockTuple ) )
                                                  .toBlocking().last();


        assertEquals( "Only 1 lock should be present", 1, lockResults.lockedTuples.size() );


        final int expectedSize = numConcurrent - 1;

        assertEquals( "Only 1 lock present", expectedSize, lockResults.unlockedTuples.size() );


        //now get the 1 lock and ensure it's current

        final LockTuple tuple = lockResults.lockedTuples.iterator().next();

        //lets unlock and ensure it works.

        tuple.lock.unlock();
    }


    private static final class LockTuple {

        private final int indexId;

        private final Lock lock;

        private final boolean hasLock;


        private LockTuple( final int indexId, final Lock lock, final boolean hasLock ) {
            this.indexId = indexId;
            this.lock = lock;
            this.hasLock = hasLock;
        }
    }


    private static final class LockResults {

        private final Set<LockTuple> unlockedTuples;

        private final Set<LockTuple> lockedTuples;


        private LockResults() {
            unlockedTuples = new HashSet<>();
            lockedTuples = new HashSet<>();
        }


        /**
         * Add the lock to the tuple
         */
        public void addLock( final LockTuple lock ) {
            if ( lock.hasLock ) {
                lockedTuples.add( lock );
            }
            else {
                unlockedTuples.add( lock );
            }
        }
    }


    @Test( expected = UnsupportedOperationException.class )
    public void failLocalLocks() {

        final LockId sharedLock = createLockId();


        //should throw an unsupported operation exception
        cassandraLockManager.createLocalLock( sharedLock );
    }


    @Test
    public void testLockUnlock() {

        final LockId sharedLock = createLockId();


        ///
        final Lock lock1 = cassandraLockManager.createMultiRegionLock( sharedLock );

        //even though it's second, it should lock if we call first
        final Lock lock2 = cassandraLockManager.createMultiRegionLock( sharedLock );

        //lock #3
        final Lock lock3 = cassandraLockManager.createMultiRegionLock( sharedLock );


        assertTrue( lock2.tryLock( ONE_HOUR_TTL, HOURS ) );

        assertFalse( lock1.tryLock( ONE_HOUR_TTL, HOURS ) );

        assertFalse(lock3.tryLock( ONE_HOUR_TTL, HOURS ));

        //now unlock lock 2 and try again

        lock2.unlock();

        assertTrue( lock1.tryLock( ONE_HOUR_TTL, HOURS ) );

        assertFalse( lock3.tryLock( ONE_HOUR_TTL, HOURS ) );

        //now unlock 1 and lock 3

        lock1.unlock();

        assertTrue(lock3.tryLock(ONE_HOUR_TTL, HOURS));
    }


    private LockId createLockId() {

        LockId lockId = mock( LockId.class );
        //mock up scope
        when( lockId.getApplicationScope() ).thenReturn( scope );

        when( lockId.generateKey() ).thenReturn( UUIDGenerator.newTimeUUID() + "" );

        return lockId;
    }
}
