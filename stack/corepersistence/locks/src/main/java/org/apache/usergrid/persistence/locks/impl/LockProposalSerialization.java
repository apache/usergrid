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

import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.locks.LockId;


/**
 * Interface for serializing node shard proposals
 */
public interface LockProposalSerialization extends Migration {


    /**
     * Propose a new shard and ack shards that are before us
     * @param lockId The key for the locks
     * @param proposed The proposed time uuid key
     * @param expirationInSeconds The time to allow the proposal to live.
     *
     * @return The Proposal of the 2 items in the proposal queue
     */
    LockCandidate writeNewValue( final LockId lockId, final UUID proposed, final int expirationInSeconds );


    /**
     * Ack the proposal and re-read
     * @param lockId
     * @param proposed The proposed uuid we set
     * @param seen The uuid to set into the seen value
     * @param expirationInSeconds The time to allow the proposal to live.
     * @return
     */
    LockCandidate ackProposed(final LockId lockId, final UUID proposed, final UUID seen, final int expirationInSeconds);

    /**
     * Poll the state of the current lock
     * @param lockId
     * @return
     */
    LockCandidate pollState(final LockId lockId);

    /**
     * Remove all the proposals
     * @param lockId The key for the locks
     * @param proposed The proposed value
     */
    void delete(  final LockId lockId, final UUID proposed );
}
