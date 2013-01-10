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
package org.usergrid.locking.zookeeper;

import java.util.concurrent.TimeUnit;

import org.usergrid.locking.Lock;
import org.usergrid.locking.exception.UGLockException;

import com.netflix.curator.framework.recipes.locks.InterProcessMutex;

/**
 * Wrapper for locks using curator
 * @author tnine
 *
 */
public class ZookeeperLockImpl implements Lock {
  

  private InterProcessMutex zkMutex;
  
  /**
   * 
   */
  public ZookeeperLockImpl(InterProcessMutex zkMutex) {
    this.zkMutex = zkMutex;
  }

  /* (non-Javadoc)
   * @see org.usergrid.locking.Lock#tryLock(long, java.util.concurrent.TimeUnit)
   */
  @Override
  public boolean tryLock(long timeout, TimeUnit time) throws UGLockException {
  
    try {
      return zkMutex.acquire(timeout, time);
    } catch (Exception e) {
      throw new UGLockException("Unable to obtain lock", e);
    }
  }

  /* (non-Javadoc)
   * @see org.usergrid.locking.Lock#lock()
   */
  @Override
  public void lock() throws UGLockException {
    try {
      zkMutex.acquire();
    } catch (Exception e) {
      throw new UGLockException("Unable to obtain lock", e);
    }
  }

  /* (non-Javadoc)
   * @see org.usergrid.locking.Lock#unlock()
   */
  @Override
  public void unlock() throws UGLockException {
    try {
      zkMutex.release();
    } catch (Exception e) {
      throw new UGLockException("Unable to obtain lock", e);
    } 
  }


}
