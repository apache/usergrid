/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Core.
 * 
 * Usergrid Core is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Usergrid Core is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Usergrid Core. If not, see <http://www.gnu.org/licenses/>.
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
