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
package org.apache.usergrid.locking.noop;


import java.util.concurrent.TimeUnit;

import org.apache.usergrid.locking.Lock;
import org.apache.usergrid.locking.exception.UGLockException;


/** @author tnine */
public class NoOpLockImpl implements Lock {

    /**
     *
     */
    public NoOpLockImpl() {
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.locking.Lock#acquire(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public boolean tryLock( long timeout, TimeUnit time ) throws UGLockException {
        //no op
        return true;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.locking.Lock#lock()
     */
    @Override
    public void lock() throws UGLockException {
        //no op
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.locking.Lock#release()
     */
    @Override
    public void unlock() throws UGLockException {
        //no op
    }
}
