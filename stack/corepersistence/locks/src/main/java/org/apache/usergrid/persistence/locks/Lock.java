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

package org.apache.usergrid.persistence.locks;


import java.util.concurrent.TimeUnit;


/**
 * Interface for lock operations
 */
public interface Lock {

    /**
     * Try the lock. Return true if it was acquired, false otherwise.
     *
     * @param timeToLive The maximum amount of time the lock can exist.  After this time, the lock will no longer be valid.
     *
     * @return True if the lock was acquired, false otherwise
     */
    boolean tryLock( final long timeToLive, final TimeUnit timeUnit );

    /**
     * Unlock this lock
     */
    void unlock();


}
