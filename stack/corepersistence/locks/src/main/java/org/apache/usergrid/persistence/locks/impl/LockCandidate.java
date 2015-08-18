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

import com.fasterxml.uuid.UUIDComparator;
import com.google.common.base.Optional;


/**
 * Represents a proposed value
 */
public class LockCandidate {


    private final UUID first;
    private final Optional<UUID> second;
    private final Optional<UUID> secondAcked;


    public LockCandidate( final UUID first, final Optional<UUID> second, final Optional<UUID> secondAcked ) {
        this.first = first;
        this.second = second;
        this.secondAcked = secondAcked;
    }


    /**
     * Return true if the proposedUuid is the first UUID
     */
    public boolean isFirst( final UUID proposedUuid ) {
        return UUIDComparator.staticCompare( first, proposedUuid ) == 0;
    }


    /**
     * Return trus if the proposedUuid is the
     */
    public boolean isLocked( final UUID proposedUuid ) {
        /**
         * The first uuid in the list, we might have the lock.  Proceed with further checks, otherwise short circuit
         */
        if ( !isFirst( proposedUuid ) ) {
            return false;
        }


        /**
         *  Check if we need to poll, if so, we can't have the lock, we need to check to ensure the send proposal has seen us
         */
        if ( shouldPoll( proposedUuid ) ) {
            return false;
        }

        //we have an "acked" value from a previous time uuid.  Compare them and be sure the
        //second has recognized the first as the being first, otherwise we can't proceed.
        if ( secondAcked.isPresent() ) {
            return UUIDComparator.staticCompare( first, secondAcked.get() ) == 0;
        }

        //there is no second we have the lock
        return true;
    }


    /**
     * Return true if we may have a lock, but just need to be acknowldeged
     */
    public boolean shouldPoll( final UUID proposedUuid ) {

        //we have a second, but it hasn't acknowledged the first (us), we can't lock
        if ( isFirst( proposedUuid ) && second.isPresent() && !secondAcked.isPresent() ) {
            return true;
        }

        return false;
    }


    /**
     * Optionally return the value to ack.  Will be absent if an ack is not required
     */
    public Optional<UUID> getValueToAck( final UUID proposedUuid ) {

        //no second, or the second isn't equal to our proposed
        if ( !second.isPresent() || UUIDComparator.staticCompare( proposedUuid, second.get() ) != 0 ) {
            return Optional.absent();
        }

        /**
         * If our second has already been acked, it's absent
         */
        if ( secondAcked.isPresent() ) {
            return Optional.absent();
        }

        //send back the first to ack
        return Optional.of( first );
    }
}
