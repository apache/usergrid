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
package org.usergrid.locking.cassandra;

import java.util.UUID;

import javax.annotation.PostConstruct;

import me.prettyprint.cassandra.locking.HLockManagerImpl;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.locking.HLockManager;
import me.prettyprint.hector.api.locking.HLockManagerConfigurator;

import org.usergrid.locking.Lock;
import org.usergrid.locking.LockManager;
import org.usergrid.locking.LockPathBuilder;
import org.usergrid.locking.exception.UGLockException;

/**
 * Uses the hector based locking implementation to obtain locks
 * 
 * @author tnine
 * 
 */
public class HectorLockManagerImpl implements LockManager {

  private int replicationFactor = 1;
  private int numberOfLockObserverThreads = 1;
  private long lockTtl = 2000;
  private String keyspaceName;
  private Cluster cluster;
  private HLockManager lm;

  /**
   * 
   */
  public HectorLockManagerImpl() {
  }

  @PostConstruct
  public void init() {
    HLockManagerConfigurator hlc = new HLockManagerConfigurator();
    hlc.setReplicationFactor(replicationFactor);
    hlc.setKeyspaceName(keyspaceName);
    hlc.setNumberOfLockObserverThreads(numberOfLockObserverThreads);
    hlc.setLocksTTLInMillis(lockTtl);
    lm = new HLockManagerImpl(cluster, hlc);
    lm.init();
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

    return new HectorLockImpl(lm.createLock(lockPath), lm);
   
  }

  /**
   * Note that in a real environment this MUST be an odd number. Locks are read
   * and written at QUORUM. RF >= 3 is preferred for failure tolerance and
   * replication.  Defaults to 1
   * 
   * @param replicationFactor
   *          the replicationFactor to set
   * 
   */
  public void setReplicationFactor(int replicationFactor) {
    this.replicationFactor = replicationFactor;
  }
 
  
  /**
   * Set the number of threads the lock heartbeat executor uses.  Must accommodate the total number of locks
   * that may exist in the system.  Locks are always renewed at the ttl/2 time.
   *  
   * @param numberOfLockObserverThreads the numberOfLockObserverThreads to set
   */
  public void setNumberOfLockObserverThreads(int numberOfLockObserverThreads) {
    this.numberOfLockObserverThreads = numberOfLockObserverThreads;
  }

  /**
   * The amount of time a lock must not be renewed before it times out.  Set in milliseconds.  2000 is the default
   * @param lockTtl the lockTtl to set
   */
  public void setLockTtl(long lockTtl) {
    this.lockTtl = lockTtl;
  }

  /**
   * @param keyspaceName
   *          the keyspaceName to set
   */
  public void setKeyspaceName(String keyspaceName) {
    this.keyspaceName = keyspaceName;
  }

  /**
   * @param cluster
   *          the cluster to set
   */
  public void setCluster(Cluster cluster) {
    this.cluster = cluster;
  }

}
