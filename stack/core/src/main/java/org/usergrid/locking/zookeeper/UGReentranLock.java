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

import org.scale7.zookeeper.cages.ILock;
import org.scale7.zookeeper.cages.ZkCagesException;
import org.scale7.zookeeper.cages.ZkWriteLock;
import org.usergrid.locking.exception.UGLockException;

/**
 * TODO create an Interface for this
 * 
 */
public class UGReentranLock {

	ZkWriteLock _lock;

	int holdCount = 0;

	public UGReentranLock(ZkWriteLock zkWriteLock) {
		_lock = zkWriteLock;
	}

	void lock() throws UGLockException {

		// This if statement only makes sense because we are holding
		// the lock in a private space for this thread.
		// Otherwise there wouldn't be a way to know if we own it. Specially
		// because this lock is a link to a remote lock manager (Zookeeper).
		if (holdCount > 0) {
			holdCount++;
			return;
		}

		boolean acquired;
		try {
			acquired = _lock.tryAcquire();

			if (!acquired) {
				throw new UGLockException("Timeout");
			}

			// Increment hold count.
			holdCount++;

			assert _lock.getState().equals(ILock.LockState.Acquired);

		} catch (ZkCagesException e) {
			throw new UGLockException(e);
		} catch (InterruptedException e) {
			throw new UGLockException(e);
		}
	}

	void unlock() throws UGLockException {

		// Decrement hold count.
		if (holdCount > 0) {
			holdCount--;
		}

		if (holdCount == 0) {
			_lock.release();
			assert _lock.getState().equals(ILock.LockState.Released);
		}
	}

	int getHoldCount() {
		return holdCount;
	}

	/**
	 * This method skip the holdCount value and release the lock. It should be
	 * called for cleanup purposes.
	 * 
	 * @throws UGLockException
	 */
	void cleanup() throws UGLockException {
		if (holdCount > 0) {
			_lock.release();
			holdCount = 0;
		}
	}

}
