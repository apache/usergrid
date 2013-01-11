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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.usergrid.locking.Lock;
import org.usergrid.locking.exception.UGLockException;

/**
 * @author tnine
 *
 */
public class SingleNodeLockImpl implements Lock {

  private ReentrantLock lock;
  
  /**
   * 
   */
  public SingleNodeLockImpl(ReentrantLock lock) {
    this.lock = lock;
  }

 
  /* (non-Javadoc)
   * @see org.usergrid.locking.Lock#acquire(long)
   */
  @Override
  public boolean tryLock(long timeout, TimeUnit time) throws UGLockException {
    try {
     return this.lock.tryLock(timeout, time);
    } catch (InterruptedException e) {
      throw new UGLockException("Couldn't get the lock", e);
    }
  }

  /* (non-Javadoc)
   * @see org.usergrid.locking.Lock#lock()
   */
  @Override
  public void lock() throws UGLockException {
    this.lock.lock();
  }


  /* (non-Javadoc)
   * @see org.usergrid.locking.Lock#release()
   */
  @Override
  public void unlock() throws UGLockException {
    this.lock.unlock();
  }

}
