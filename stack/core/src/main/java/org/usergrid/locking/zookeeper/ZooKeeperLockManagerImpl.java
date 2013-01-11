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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.locking.Lock;
import org.usergrid.locking.LockManager;
import org.usergrid.locking.LockPathBuilder;
import org.usergrid.locking.exception.UGLockException;

import com.netflix.curator.RetryPolicy;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.framework.recipes.locks.InterProcessMutex;
import com.netflix.curator.retry.ExponentialBackoffRetry;

/**
 * Implementation for Zookeeper service that handles global locks.
 * 
 */
public final class ZooKeeperLockManagerImpl implements LockManager {

  private String hostPort;

  private int sessionTimeout = 2000;

  private int maxAttempts = 5;
  
  private CuratorFramework client;

  public ZooKeeperLockManagerImpl(String hostPort, int sessionTimeout, int maxAttemps) {
    this.hostPort = hostPort;
    this.sessionTimeout = sessionTimeout;
    this.maxAttempts = maxAttemps;
    init();
  }

  public ZooKeeperLockManagerImpl() {
  }

  @PostConstruct
  public void init() {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(sessionTimeout, maxAttempts);
    client = CuratorFrameworkFactory.newClient(hostPort, retryPolicy);
    client.start();
    
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

    
    
    return new ZookeeperLockImpl(new InterProcessMutex(client, lockPath));

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

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(int maxAttemps) {
    this.maxAttempts = maxAttemps;
  }

}
