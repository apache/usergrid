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
package org.apache.usergrid.locking;


import java.util.concurrent.TimeUnit;

import org.apache.usergrid.locking.exception.UGLockException;


/**
 * The lock object to acquire
 *
 * @author tnine
 */
public interface Lock {

    /**
     * Acquire the lock.  Wait the specified amount of time before giving up
     *
     * @param timeout The amount of time to wait
     * @param time the units of time to wait
     */
    public boolean tryLock( long timeout, TimeUnit time ) throws UGLockException;

    /** Block until a lock is available */
    public void lock() throws UGLockException;

    /** Release the lock */
    public void unlock() throws UGLockException;
}
