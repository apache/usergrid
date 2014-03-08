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


import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.usergrid.locking.Lock;
import org.apache.usergrid.locking.exception.UGLockException;

import me.prettyprint.hector.api.locking.HLock;
import me.prettyprint.hector.api.locking.HLockManager;
import me.prettyprint.hector.api.locking.HLockTimeoutException;


/** @author tnine */
public class HectorLockImpl implements Lock {

    private HLock lock;
    private HLockManager lm;
    private AtomicInteger count = new AtomicInteger();


    /**
     *
     */
    public HectorLockImpl( HLock lock, HLockManager lm ) {
        this.lock = lock;
        this.lm = lm;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.locking.Lock#acquire(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public boolean tryLock( long timeout, TimeUnit time ) throws UGLockException {
        try {
            lm.acquire( this.lock, time.toMillis( timeout ) );
            count.incrementAndGet();
        }
        catch ( HLockTimeoutException hlte ) {
            return false;
        }

        return true;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.locking.Lock#lock()
     */
    @Override
    public void lock() throws UGLockException {
        lm.acquire( lock );
        count.incrementAndGet();
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.locking.Lock#release()
     */
    @Override
    public void unlock() throws UGLockException {
        int current = count.decrementAndGet();

        if ( current == 0 ) {
            lm.release( this.lock );
        }
    }
}
