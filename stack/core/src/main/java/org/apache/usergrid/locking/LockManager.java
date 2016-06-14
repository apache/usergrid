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


import java.util.UUID;

import org.apache.usergrid.locking.exception.UGLockException;


/**
 * This Interface to a class responsible for distributed lock across system.
 *
 * @author tnine
 */
public interface LockManager {

    /**
     * Acquires a lock on a particular path.
     *
     * @param applicationId application UUID
     * @param path a unique path
     *
     * @throws UGLockException if the lock cannot be acquired
     */
    public Lock createLock( final UUID applicationId, final String... path );

    /**
     * Setup lock persistence mechanism.
     */
    public void setup();
}
