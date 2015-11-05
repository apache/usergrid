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


import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

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
import org.apache.usergrid.persistence.locks.LockId;
import org.apache.usergrid.persistence.locks.guice.TestLockModule;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith( ITRunner.class )
@UseModules( { TestLockModule.class } )
public class LockProposalSerializationTest {

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected LockProposalSerialization serialization;


    protected ApplicationScope scope;

    protected final AtomicLong atomicLong = new AtomicLong();


    private static final int ONE_HOUR_TTL = 360;


    @Before
    public void setup() {
        scope = mock( ApplicationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getApplication() ).thenReturn( orgId );
    }


    @Test
    public void testOnlyLock() {

        final LockId testId = createLockId();
        final UUID proposed = UUIDGenerator.newTimeUUID();


        final LockCandidate candidate = serialization.writeNewValue( testId, proposed, ONE_HOUR_TTL );


        assertNotNull( candidate );

        assertTrue( "First written value is the only proposal", candidate.isFirst( proposed ) );

        assertTrue( "No second value present", candidate.isLocked( proposed ) );
        assertFalse( "Should not ack", candidate.getValueToAck( proposed ).isPresent() );
    }


    @Test
    public void testTwoLocks() {

        final LockId testId = createLockId();
        final UUID proposedFirst = UUIDGenerator.newTimeUUID();

        final UUID proposedSecond = UUIDGenerator.newTimeUUID();


        final LockCandidate candidateFirst = serialization.writeNewValue( testId, proposedFirst, ONE_HOUR_TTL );

        final LockCandidate candidateSecond = serialization.writeNewValue( testId, proposedSecond, ONE_HOUR_TTL );

        assertNotNull( candidateFirst );

        assertTrue( "First written value is the only proposal", candidateFirst.isFirst( proposedFirst ) );

        assertTrue( "No second value present", candidateFirst.isLocked( proposedFirst ) );

        assertFalse( "Should not ack", candidateFirst.getValueToAck( proposedFirst ).isPresent() );

        assertTrue( "Lock is present", candidateFirst.isLocked( proposedFirst ) );


        assertNotNull( candidateSecond );

        assertFalse( "Second candidate not present", candidateSecond.isFirst( proposedSecond ) );

        assertFalse( "Second does not have lock", candidateSecond.isLocked( proposedSecond ) );

        assertEquals( "Second should ack", proposedFirst, candidateSecond.getValueToAck( proposedSecond ).get() );

        assertFalse( "Lock is not present", candidateSecond.isLocked( proposedSecond ) );


        //now remove first, second should get lock

        final LockCandidate ackedResponse =
            serialization.ackProposed( testId, proposedSecond, proposedFirst, ONE_HOUR_TTL );


        //now check if we can lock, still, we should be able to

        assertTrue( "First written value is the only proposal", ackedResponse.isFirst( proposedFirst ) );

        assertTrue( "No second value present", ackedResponse.isLocked( proposedFirst ) );

        assertFalse( "Should not ack", ackedResponse.getValueToAck( proposedFirst ).isPresent() );

        assertTrue( "Lock is present", ackedResponse.isLocked( proposedFirst ) );


        //
        assertFalse( "Second candidate is present", ackedResponse.isFirst( proposedSecond ) );

        assertFalse( "Second does not have lock", ackedResponse.isLocked( proposedSecond ) );

        //now we don't need to ack
        assertFalse( "Second should ack", ackedResponse.getValueToAck( proposedSecond ).isPresent() );

        assertFalse( "Lock is not present", ackedResponse.isLocked( proposedSecond ) );
    }



    @Test
    public void testTwoLocksFirstDeleted() {

        final LockId testId = createLockId();
        final UUID proposedFirst = UUIDGenerator.newTimeUUID();

        final UUID proposedSecond = UUIDGenerator.newTimeUUID();


        final LockCandidate candidateFirst = serialization.writeNewValue( testId, proposedFirst, ONE_HOUR_TTL );

        final LockCandidate candidateSecond = serialization.writeNewValue( testId, proposedSecond, ONE_HOUR_TTL );

        assertNotNull( candidateFirst );

        assertTrue( "First written value is the only proposal", candidateFirst.isFirst( proposedFirst ) );

        assertTrue( "No second value present", candidateFirst.isLocked( proposedFirst ) );

        assertFalse( "Should not ack", candidateFirst.getValueToAck( proposedFirst ).isPresent() );


        assertNotNull( candidateSecond );

        assertFalse( "Second candidate not present", candidateSecond.isFirst( proposedSecond ) );

        assertFalse( "Second does not have lock", candidateSecond.isLocked( proposedSecond ) );

        assertEquals( "Second should ack", proposedFirst,  candidateSecond.getValueToAck( proposedSecond ).get() );


        //now remove first, second should get lock

        final LockCandidate ackedResponse =
            serialization.ackProposed( testId, proposedSecond, proposedFirst, ONE_HOUR_TTL );


        //now check if we can lock, still, we should be able to

        assertTrue( "First written value is the only proposal", ackedResponse.isFirst( proposedFirst ) );

        assertTrue( "No second value present", ackedResponse.isLocked( proposedFirst ) );

        assertFalse( "Should not ack", ackedResponse.getValueToAck( proposedFirst ).isPresent() );

        //
        assertFalse( "Second candidate is present", ackedResponse.isFirst( proposedSecond ) );

        assertFalse( "Second does not have lock", ackedResponse.isLocked( proposedSecond ) );

        //now we don't need to ack
        assertFalse( "Second should ack", ackedResponse.getValueToAck( proposedSecond ).isPresent() );
    }


    private LockId createLockId() {

        LockId lockId = mock( LockId.class );
        //mock up scope
        when( lockId.getApplicationScope() ).thenReturn( scope );

        when( lockId.generateKey() ).thenReturn( atomicLong.incrementAndGet() + "" );

        return lockId;
    }
}
