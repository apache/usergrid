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
package org.usergrid.locking;

import java.util.UUID;

import org.usergrid.locking.exception.UGLockException;

/**
 * This Interface to a class responsible for distributed lock across system.
 * 
 */
public interface LockManager {

  /**
   * Acquires a lock on a particular path.
   * 
   * @param applicationId
   *          application UUID
   * @param path
   *          a unique path
   * @throws UGLockException
   *           if the lock cannot be acquired
   */
  Lock createLock(final UUID applicationId, final String... path) throws UGLockException;

}
