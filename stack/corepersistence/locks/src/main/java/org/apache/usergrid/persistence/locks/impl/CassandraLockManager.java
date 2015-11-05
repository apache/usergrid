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


import org.apache.usergrid.persistence.locks.Lock;
import org.apache.usergrid.persistence.locks.LockId;
import org.apache.usergrid.persistence.locks.LockManager;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Create a lock manager that is cassandra based
 */
@Singleton
public class CassandraLockManager implements LockManager {

    private final CassandraLockFig cassandraLockFig;
    private final LockProposalSerialization lockProposalSerialization;


    @Inject
    public CassandraLockManager( final CassandraLockFig cassandraLockFig, final LockProposalSerialization lockProposalSerialization ) {


        this.cassandraLockFig = cassandraLockFig;
        this.lockProposalSerialization = lockProposalSerialization;
    }


    @Override
    public Lock createMultiRegionLock( final LockId key ) {
        Preconditions.checkNotNull(key, "Key is required");

        return new CassandraMultiRegionLock( cassandraLockFig, key, lockProposalSerialization );
    }


    @Override
    public Lock createLocalLock( final LockId key ) {
        throw new UnsupportedOperationException( "Only multi region locks are supported" );
    }
}
