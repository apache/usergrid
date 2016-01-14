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
package org.apache.usergrid.locking.cassandra;


import com.netflix.astyanax.recipes.locks.ColumnPrefixDistributedRowLock;

import org.apache.usergrid.locking.Lock;
import org.apache.usergrid.locking.exception.UGLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


public class AstyanaxLockImpl implements Lock {

    private static final Logger logger = LoggerFactory.getLogger( AstyanaxLockImpl.class );



    ColumnPrefixDistributedRowLock lock;

    public AstyanaxLockImpl(//AstyanaxLockManagerImpl lockManager,
                            ColumnPrefixDistributedRowLock lock) {

        this.lock = lock;

    }


    @Override
    public boolean tryLock( long timeout, TimeUnit time ) throws UGLockException {
        lock.withTtl( (int) timeout, time);

        try {
            lock.acquire();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void lock() throws UGLockException {

        try {
            lock.acquire();
        } catch (Exception e) {
            throw new UGLockException("Unable to acquire lock with id: " + lock.getLockId());
        }
    }

    @Override
    public void unlock() throws UGLockException {

        try {
            lock.release();
        } catch (Exception e) {
            throw new UGLockException("Unable to release lock with id: " + lock.getLockId());
        }

    }

}
