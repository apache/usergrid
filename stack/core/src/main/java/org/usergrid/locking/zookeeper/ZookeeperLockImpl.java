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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;

import org.scale7.zookeeper.cages.ZkCagesException;
import org.scale7.zookeeper.cages.ZkWriteLock;
import org.scale7.zookeeper.cages.ILock.LockState;
import org.usergrid.locking.Lock;
import org.usergrid.locking.exception.UGLockException;
import org.scale7.networking.utility.NetworkAlgorithms;

/**
 * @author tnine
 *
 */
public class ZookeeperLockImpl implements Lock {
  
  private final int MIN_RETRY_DELAY = 125;
  private final int MAX_RETRY_DELAY = 1000;

  private ZkWriteLock zkLock;
  private AtomicLong locks = new AtomicLong();;
  
  /**
   * 
   */
  public ZookeeperLockImpl(ZkWriteLock zkLock) {
    this.zkLock = zkLock;
  }

  /* (non-Javadoc)
   * @see org.usergrid.locking.Lock#tryLock(long, java.util.concurrent.TimeUnit)
   */
  @Override
  public boolean tryLock(long timeout, TimeUnit time) throws UGLockException {
  
    
    long endTime = System.currentTimeMillis()+time.toMillis(timeout);
    
    for(int attempt = 0; System.currentTimeMillis() <= endTime; attempt ++){
      try {
        if(this.zkLock.tryAcquire()){
          locks.incrementAndGet();
          return true;
        }
        
        Thread.sleep(NetworkAlgorithms.getBinaryBackoffDelay(attempt,
            MIN_RETRY_DELAY, MAX_RETRY_DELAY));
      } catch (ZkCagesException e) {
        throw new UGLockException("Unable to obtain lock", e);
      } catch (InterruptedException e) {
        throw new UGLockException("Unable to obtain lock", e);
      }
    }
   
    return false;
  }

  /* (non-Javadoc)
   * @see org.usergrid.locking.Lock#lock()
   */
  @Override
  public void lock() throws UGLockException {
    //already have the lock, increment the internal count
    if(this.zkLock.getState() == LockState.Acquired){
      locks.incrementAndGet();
      return;
    }
    
    try {
      this.zkLock.acquire();
      locks.incrementAndGet();
    } catch (ZkCagesException e) {
      throw new UGLockException("Unable to obtain lock", e);
    } catch (InterruptedException e) {
      throw new UGLockException("Unable to obtain lock", e);
    }
  }

  /* (non-Javadoc)
   * @see org.usergrid.locking.Lock#unlock()
   */
  @Override
  public void unlock() throws UGLockException {
    
    long count = locks.decrementAndGet();
    
    if(count == 0 && this.zkLock.getState() == LockState.Acquired){
      this.zkLock.release();
    }
    
  }


}
