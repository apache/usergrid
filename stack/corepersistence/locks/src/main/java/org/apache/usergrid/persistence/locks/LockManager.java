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


/**
 * A simple interface for distributed locks.  These locks can be assumed to be cluster wide in multi regions. It is up
 * to the implementor to determine how to best accomplish this.
 */
public interface LockManager {


    /**
     * Create a lock instance with the given key that is valid across all regions
     *
     * @param key The key to use when locking
     *
     *
     * @return A new instance of the lock, without any lock acquired
     */
    Lock createMultiRegionLock( final LockId key );

    /**
     * Create a lock in the local region
     * @param key
     * @return
     */
    Lock createLocalLock(final LockId key);
}
