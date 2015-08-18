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

import org.junit.Test;

import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;

import static org.junit.Assert.*;


public class LockCandidateTest {

    @Test
    public void testOnlyUUID(){
        final UUID proposedUuid = UUIDGenerator.newTimeUUID();

        final LockCandidate candidate = new LockCandidate( proposedUuid, Optional.absent(), Optional.absent() );

        assertTrue( "UUID should be first", candidate.isFirst( proposedUuid ) );

        final UUID otherUuid = UUIDGenerator.newTimeUUID();

        assertFalse("UUID is not first", candidate.isFirst( otherUuid ));

        assertTrue( "Should have lock", candidate.isLocked( proposedUuid ) );

        assertFalse("Should not have lock", candidate.isLocked( otherUuid ));
    }
}
