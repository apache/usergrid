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
package org.usergrid.locking.noop;

import java.util.UUID;

import org.usergrid.locking.Lock;
import org.usergrid.locking.LockManager;
import org.usergrid.locking.exception.UGLockException;

/**
 * This is a no-op manager used for testing.
 * 
 */
public class NoOpLockManagerImpl implements LockManager {

	public NoOpLockManagerImpl() {

	}

  /* (non-Javadoc)
   * @see org.usergrid.locking.LockManager#createLock(java.util.UUID, java.lang.String[])
   */
  @Override
  public Lock createLock(UUID applicationId, String... path) throws UGLockException {
    return new NoOpLockImpl();
  }



}
