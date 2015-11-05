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
import java.util.concurrent.TimeUnit;

import org.apache.usergrid.persistence.locks.Lock;
import org.apache.usergrid.persistence.locks.LockId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;


/**
 * Lock that uses cassandra for multiple regions
 */
public class CassandraMultiRegionLock implements Lock {

    private final LockId lockId;
    private final LockProposalSerialization lockProposalSerialization;
    private final UUID lockUUID;
    private final CassandraLockFig cassandraLockFig;


    public CassandraMultiRegionLock( final CassandraLockFig cassandraLockFig, final LockId lockId,
                                     final LockProposalSerialization lockProposalSerialization ) {
        this.cassandraLockFig = cassandraLockFig;
        this.lockId = lockId;
        this.lockProposalSerialization = lockProposalSerialization;
        this.lockUUID = UUIDGenerator.newTimeUUID();
    }


    @Override
    public boolean tryLock( final long timeToLive, final TimeUnit timeUnit ) {

        final long expirationLong = timeUnit.toSeconds( timeToLive );

        Preconditions.checkArgument( expirationLong <= Integer.MAX_VALUE,
            "Expiration cannot be longer than " + Integer.MAX_VALUE );

        final int expiration = ( int ) expirationLong;

        final LockCandidate lockCandidate =
            this.lockProposalSerialization.writeNewValue( lockId, lockUUID, expiration );

        //now check if we need to ack our previous

        if ( lockCandidate.isLocked( lockUUID ) ) {
            return true;
        }


        final Optional<UUID> uuidToAck = lockCandidate.getValueToAck( lockUUID );

        if ( uuidToAck.isPresent() ) {
            this.lockProposalSerialization.ackProposed( lockId, lockUUID, uuidToAck.get(), expiration );
        }


        //we should poll to see if we can get the lock
        if(lockCandidate.shouldPoll( lockUUID ) && pollForLock()){
            return true;
        }


        //we don't have a lock, delete our candidate
        unlock();

        return false;
    }


    /**
     * Sleep then poll the lock, if we get it, return true, otherwise short circuit
     * @return
     */
    private boolean pollForLock(){
        for(int i = 0; i < cassandraLockFig.getPollCount(); i ++){

            try {
                Thread.sleep( cassandraLockFig.getLockPollWait() );
            }
            catch ( InterruptedException e ) {
                throw new RuntimeException( "Unable to sleep on poll" );
            }

            final LockCandidate lockState = this.lockProposalSerialization.pollState( lockId );

            //done, short cuircuit
            if(lockState.isLocked( lockUUID )){
                return true;
            }

            //otherwise loop again and wait

        }

        return false;
    }

    @Override
    public void unlock() {
        //unlock and delete
        this.lockProposalSerialization.delete( lockId, lockUUID );
    }


}
