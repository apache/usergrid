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
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.usergrid.exception.NotImplementedException;
import org.usergrid.locking.LockManager;
import org.usergrid.locking.LockPathBuilder;
import org.usergrid.locking.exception.UGLockException;
import org.usergrid.persistence.EntityRef;

/**
 * Single Node implementation for {@link LocalManager}
 * 
 */
public class SingleNodeLockManagerImpl implements LockManager {

	private final HashMap<String, ReentrantLock> globalLocks;

	/**
	 * Holds a reference to all the locks owned by this thread. When a lock is
	 * acquired, it is added to this collection. When a lock is released, the
	 * lock is removed from this collection.
	 */
	private final ThreadLocal<HashMap<String, ReentrantLock>> localLocks = new ThreadLocal<HashMap<String, ReentrantLock>>();

	/**
	 * Default constructor.
	 */
	public SingleNodeLockManagerImpl() {
		globalLocks = new HashMap<String, ReentrantLock>();
	}

	public ReentrantLock getCreateLockInternal(String lockPath) {

		// check first if it is already own by this thread.
		ReentrantLock lock = localLocks.get().get(lockPath);

		if (lock != null) {
			return lock;
		}

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

		// So at this point, the lock was added to the threadLocal collection as
		// well as
		// the general collection.
		return lock;
	}

	@Override
	public void lock(UUID applicationId, UUID entityId) throws UGLockException {
		String lockPath = LockPathBuilder.buildPath(applicationId, entityId);
		lockInternal(lockPath);
	}

	@Override
	public void lock(final EntityRef application, final EntityRef entity)
			throws UGLockException {
		String lockPath = LockPathBuilder.buildPath(application, entity);
		lockInternal(lockPath);
	}

	private void lockInternal(String lockPath) throws UGLockException {

		if (localLocks.get() == null) {
			localLocks.set(new HashMap<String, ReentrantLock>());
		}

		ReentrantLock lock = getCreateLockInternal(lockPath);

		// TODO make it configurable and inject it through Spring.
		try {
			boolean acquired = lock.tryLock(2000, TimeUnit.MILLISECONDS);
			if (!acquired) {
				throw new UGLockException("Timeout for locked: " + lockPath);
			}
		} catch (InterruptedException e) {
			throw new UGLockException(e);
		}

		// IF it is the first time, it will add it. Otherwise will put it again.
		localLocks.get().put(lockPath, lock);
	}

	@Override
	public void unlock(UUID applicationId, UUID entityId)
			throws UGLockException {
		String lockPath = LockPathBuilder.buildPath(applicationId, entityId);
		unlock(lockPath);
	}

	private void unlock(String lockPath) throws UGLockException {
		ReentrantLock lock = getCreateLockInternal(lockPath);
		unlockInternal(lock, lockPath);
	}

	private void unlockInternal(ReentrantLock lock, String lockPath)
			throws UGLockException {

		lock.unlock();

		if (lock.getHoldCount() == 0) {
			// Remove the lock from thread
			localLocks.get().remove(lockPath);

			synchronized (this) {

				// Only remove from global if there are no thread waiting here.
				// TODO What happens if the own times out later?
				// mmmmm memory leak?
				if (!lock.hasQueuedThreads()) {
					globalLocks.remove(lockPath);
				}
			}
		}
	}

	@Override
	public void unlock(EntityRef application, EntityRef entity)
			throws UGLockException {
		String lockPath = LockPathBuilder.buildPath(application, entity);
		unlock(lockPath);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void cleanupLocksForThread() throws UGLockException {
		if (localLocks.get() == null) {
			return;
		}

		for (Entry<String, ReentrantLock> lockEntry : localLocks.get()
				.entrySet()) {
			unlockInternal(lockEntry.getValue(), lockEntry.getKey());
		}

		// Let the collection be garbage collected.
		localLocks.set(null);
	}

	@Override
	public void lock(EntityRef application, EntityRef... entities)
			throws UGLockException {
		// TODO Auto-generated method stub
		throw new NotImplementedException();
	}

	@Override
	public void unlock(EntityRef application, EntityRef... entities)
			throws UGLockException {
		// TODO Auto-generated method stub
		throw new NotImplementedException();
	}

	@Override
	public void lock(UUID application, UUID... entities) throws UGLockException {
		// TODO Auto-generated method stub
		throw new NotImplementedException();
	}

	@Override
	public void unlock(UUID application, UUID... entities)
			throws UGLockException {
		// TODO Auto-generated method stub
		throw new NotImplementedException();
	}

	@Override
	public void lock(UUID applicationId, String path) throws UGLockException {
		throw new NotImplementedException();

	}

	@Override
	public void unlock(UUID applicationId, String path) throws UGLockException {
		throw new NotImplementedException();

	}

	@Override
	public void lock(UUID applicationId, String... paths)
			throws UGLockException {
		throw new NotImplementedException();

	}

	@Override
	public void unlock(UUID applicationId, String... paths)
			throws UGLockException {
		throw new NotImplementedException();

	}

	@Override
	public void lockProperty(UUID applicationId, String entityType,
			String propertyName) throws UGLockException {
		throw new NotImplementedException();
	}

	@Override
	public void unlockProperty(UUID applicationId, final String entityType,
			String path, String propertyName) throws UGLockException {
		throw new NotImplementedException();
	}

	@Override
	public void lockProperty(UUID applicationId, final String entityType,
			String... propertyNames) throws UGLockException {
		throw new NotImplementedException();
	}

	@Override
	public void unlockProperty(UUID applicationId, final String entityType,
			String... propertyNames) throws UGLockException {
		throw new NotImplementedException();
	}

}
