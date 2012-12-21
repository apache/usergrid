/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.locking.singlenode;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.usergrid.locking.Lock;
import org.usergrid.locking.LockManager;
import org.usergrid.locking.LockPathBuilder;
import org.usergrid.locking.exception.UGLockException;

/**
 * Single Node implementation for {@link LocalManager}
 * 
 */
public class SingleNodeLockManagerImpl implements LockManager {

  private final HashMap<String, ReentrantLock> globalLocks;

  /**
   * Default constructor.
   */
  public SingleNodeLockManagerImpl() {
    globalLocks = new HashMap<String, ReentrantLock>();
  }


 

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.locking.LockManager#createLock(java.util.UUID,
   * java.lang.String[])
   */
  @Override
  public Lock createLock(UUID applicationId, String... path) throws UGLockException {

    String lockPath = LockPathBuilder.buildPath(applicationId, path);
    
    // check first if it is already own by this thread.
    ReentrantLock lock = globalLocks.get(lockPath);
    

    if (lock == null) {
      synchronized (this) {
        // Check in the Global collection in case someone else owns it.
        lock = globalLocks.get(lockPath);

        if (lock == null) {
          // if lock does not exist, null is return but intermediateLock
          // is added to the map.
          lock = new ReentrantLock();
          globalLocks.put(lockPath, lock);
        }
      }
    }

    // So at this point, the lock was added to the threadLocal collection as
    // well as
    // the general collection.
    return new SingleNodeLockImpl(lock);
  }

}
