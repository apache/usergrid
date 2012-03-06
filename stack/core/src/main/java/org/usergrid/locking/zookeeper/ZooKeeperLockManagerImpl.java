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

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.scale7.networking.utility.NetworkAlgorithms;
import org.scale7.zookeeper.cages.ZkSessionManager;
import org.scale7.zookeeper.cages.ZkWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.locking.LockManager;
import org.usergrid.locking.LockPathBuilder;
import org.usergrid.locking.exception.UGLockException;
import org.usergrid.persistence.EntityRef;

/**
 * Implementation for Zookeeper service that handles global locks.
 * 
 */
public final class ZooKeeperLockManagerImpl implements LockManager {

	private final int MIN_RETRY_DELAY = 125;
	private final int MAX_RETRY_DELAY = 4000;
	private final int MAX_ACQUIRE_ATTEMPTS = 5;

	private String hostPort;

	private int sessionTimeout;

	private int maxAttemps;

	/**
	 * Holds a reference to all the locks owned by this thread. When a lock is
	 * acquired, it is added to this collection. When a lock is released, the
	 * lock is removed from this collection.
	 */
	private final ThreadLocal<HashMap<String, UGReentranLock>> localLocks = new ThreadLocal<HashMap<String, UGReentranLock>>();

	public ZooKeeperLockManagerImpl(String hostPort, int sessionTimeout,
			int maxAttemps) {
		this.hostPort = hostPort;
		this.sessionTimeout = sessionTimeout;
		this.maxAttemps = maxAttemps;
		init();
	}

	public ZooKeeperLockManagerImpl() {
	}

	private void init() {
		ZkSessionManager.initializeInstance(hostPort, sessionTimeout,
				maxAttemps);
	}

	public UGReentranLock getCreateLockInternal(String lockPath) {

		// check first if it is already own by this thread.
		UGReentranLock lock = localLocks.get().get(lockPath);

		if (lock == null) {
			lock = new UGReentranLock(new ZkWriteLock(lockPath));
		}

		return lock;
	}

	protected static final Logger logger = LoggerFactory
			.getLogger(ZooKeeperLockManagerImpl.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void lock(EntityRef application, EntityRef entity)
			throws UGLockException {
		String lockPath = LockPathBuilder.buildPath(application, entity);
		lockInternal(lockPath);
	}

	@Override
	public void lock(EntityRef application, EntityRef... entities)
			throws UGLockException {
		List<String> locksPath = LockPathBuilder.buildPath(application,
				entities);
		lockInternal(locksPath);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void lock(UUID applicationId, UUID entityId) throws UGLockException {
		String lockPath = LockPathBuilder.buildPath(applicationId, entityId);
		lockInternal(lockPath);
	}

	@Override
	public void lock(UUID application, UUID... entities) throws UGLockException {
		List<String> locksPath = LockPathBuilder.buildPath(application,
				entities);
		lockInternal(locksPath);
	}

	private void lockInternal(String lockPath) throws UGLockException {
		checkCreateLocalLocks();
		UGReentranLock lock = getCreateLockInternal(lockPath);
		lockInternal(lockPath, lock);
	}

	private void lockInternal(String lockPath, UGReentranLock lock)
			throws UGLockException {
		lock.lock();

		// IF it is the first time, it will add it. Otherwise will put it again.
		localLocks.get().put(lockPath, lock);
	}

	private void lockInternal(List<String> locksPath) throws UGLockException {
		checkCreateLocalLocks();
		int attempts = 0;
		while (true) {

			if (tryAcquireAll(locksPath)) {
				return;
			}

			// Not this time, and we give up and throw after max attempts
			attempts++;
			if (attempts > MAX_ACQUIRE_ATTEMPTS) {
				logger.warn("Max attempts to acquire paths exceeded");
				throw new UGLockException("Max attemps Exceeded");
			}

			// Otherwise back off a little then try again
			try {
				Thread.sleep(NetworkAlgorithms.getBinaryBackoffDelay(attempts,
						MIN_RETRY_DELAY, MAX_RETRY_DELAY));
			} catch (InterruptedException e) {
				throw new UGLockException("An Interruption occured");
			}
		}

	}

	private boolean tryAcquireAll(List<String> locksPath)
			throws UGLockException {
		try {
			for (String aLockPath : locksPath) {
				UGReentranLock lock = getCreateLockInternal(aLockPath);
				lockInternal(aLockPath, lock);
			}
		} catch (UGLockException e) {
			unlockInternal(locksPath);
			return false;
		}

		// succeeded locking all the paths.
		return true;
	}

	private void checkCreateLocalLocks() {
		if (localLocks.get() == null) {
			localLocks.set(new HashMap<String, UGReentranLock>());
		}
	}

	private void unlockInternal(List<String> locksPath) throws UGLockException {
		for (String aLockPath : locksPath) {
			unlockInternal(aLockPath);
		}
	}

	private void unlockInternal(String lockPath) throws UGLockException {

		if (localLocks.get() == null) {
			// Nothing to release. The local map is empty.
			// Perhaps an assertion here would be better.
			return;
		}

		UGReentranLock lock = localLocks.get().get(lockPath);

		if (lock == null) {
			// Nothing to unlock. It can happen when we have multiple paths
			// locks.
			// It's safe to check.
			return;
		}

		lock.unlock();

		if (lock.getHoldCount() == 0) {
			// It's fully unlocked
			localLocks.get().remove(lockPath);
		}
	}

	@Override
	public void unlock(EntityRef application, EntityRef entity)
			throws UGLockException {
		String lockPath = LockPathBuilder.buildPath(application, entity);
		unlockInternal(lockPath);
	}

	@Override
	public void unlock(EntityRef application, EntityRef... entities)
			throws UGLockException {
		List<String> locksPath = LockPathBuilder.buildPath(application,
				entities);
		unlockInternal(locksPath);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void unlock(UUID applicationId, UUID entityId)
			throws UGLockException {
		String lockPath = LockPathBuilder.buildPath(applicationId, entityId);
		unlockInternal(lockPath);
	}

	@Override
	public void unlock(UUID application, UUID... entities)
			throws UGLockException {
		List<String> locksPath = LockPathBuilder.buildPath(application,
				entities);
		unlockInternal(locksPath);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void cleanupLocksForThread() throws UGLockException {
		for (UGReentranLock lock : localLocks.get().values()) {
			lock.cleanup();
		}

		// Let the collection be garbage collected.
		localLocks.set(null);
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

	@Override
	public void lock(UUID applicationId, String path) throws UGLockException {
		String lockPath = LockPathBuilder.buildPath(applicationId, path);
		lockInternal(lockPath);
	}

	@Override
	public void unlock(UUID applicationId, String path) throws UGLockException {
		String lockPath = LockPathBuilder.buildPath(applicationId, path);
		unlockInternal(lockPath);
	}

	@Override
	public void lock(UUID applicationId, String... paths)
			throws UGLockException {
		List<String> lockPaths = LockPathBuilder
				.buildPath(applicationId, paths);
		lockInternal(lockPaths);
	}

	@Override
	public void unlock(UUID applicationId, String... paths)
			throws UGLockException {
		List<String> lockPaths = LockPathBuilder
				.buildPath(applicationId, paths);
		unlockInternal(lockPaths);
	}

	@Override
	public void lockProperty(UUID applicationId, String entityType,
			String propertyName) throws UGLockException {
		lockInternal(LockPathBuilder.buildPropertyPath(applicationId,
				entityType, propertyName));
	}

	@Override
	public void unlockProperty(UUID applicationId, final String entityType,
			String path, String propertyName) throws UGLockException {
		unlockInternal(LockPathBuilder.buildPropertyPath(applicationId,
				entityType, propertyName));
	}

	@Override
	public void lockProperty(UUID applicationId, final String entityType,
			String... propertyNames) throws UGLockException {
		lockInternal(LockPathBuilder.buildPropertyPaths(applicationId,
				entityType, propertyNames));
	}

	@Override
	public void unlockProperty(UUID applicationId, final String entityType,
			String... propertyNames) throws UGLockException {
		unlockInternal(LockPathBuilder.buildPropertyPaths(applicationId,
				entityType, propertyNames));
	}
}
