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

import java.util.UUID;

import javax.annotation.PostConstruct;

import org.scale7.zookeeper.cages.ZkSessionManager;
import org.scale7.zookeeper.cages.ZkWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.locking.Lock;
import org.usergrid.locking.LockManager;
import org.usergrid.locking.LockPathBuilder;
import org.usergrid.locking.exception.UGLockException;

/**
 * Implementation for Zookeeper service that handles global locks.
 * 
 */
public final class ZooKeeperLockManagerImpl implements LockManager {

  private String hostPort;

  private int sessionTimeout;

  private int maxAttemps;

  public ZooKeeperLockManagerImpl(String hostPort, int sessionTimeout, int maxAttemps) {
    this.hostPort = hostPort;
    this.sessionTimeout = sessionTimeout;
    this.maxAttemps = maxAttemps;
    init();
  }

  public ZooKeeperLockManagerImpl() {
  }

  @PostConstruct
  public void init() {
    ZkSessionManager.initializeInstance(hostPort, sessionTimeout, maxAttemps);
  }

  protected static final Logger logger = LoggerFactory.getLogger(ZooKeeperLockManagerImpl.class);

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.locking.LockManager#createLock(java.util.UUID,
   * java.lang.String[])
   */
  @Override
  public Lock createLock(UUID applicationId, String... path) throws UGLockException {
    String lockPath = LockPathBuilder.buildPath(applicationId, path);

    return new ZookeeperLockImpl(new ZkWriteLock(lockPath));

  }

  public String getHostPort() {
    return hostPort;
  }

  public void setHostPort(String hostPort) {
    this.hostPort = hostPort;
  }

  public int getSessionTimeout() {
    return sessionTimeout;
  }

  public void setSessionTimeout(int sessionTimeout) {
    this.sessionTimeout = sessionTimeout;
  }

  public int getMaxAttemps() {
    return maxAttemps;
  }

  public void setMaxAttemps(int maxAttemps) {
    this.maxAttemps = maxAttemps;
  }

}
