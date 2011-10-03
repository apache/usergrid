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

import java.util.UUID;

import org.usergrid.locking.LockManager;
import org.usergrid.locking.exception.UGLockException;
import org.usergrid.persistence.EntityRef;

/**
 * This is a no-op manager used for testing preferently.
 * 
 */
public class NoOpLockManagerImpl implements LockManager {

	public NoOpLockManagerImpl() {

	}

	@Override
	public void lock(EntityRef application, EntityRef entity)
			throws UGLockException {
		// TODO Auto-generated method stub
	}

	@Override
	public void unlock(EntityRef application, EntityRef entity)
			throws UGLockException {
		// TODO Auto-generated method stub
	}

	@Override
	public void lock(EntityRef application, EntityRef... entities)
			throws UGLockException {
		// TODO Auto-generated method stub

	}

	@Override
	public void unlock(EntityRef application, EntityRef... entities)
			throws UGLockException {
		// TODO Auto-generated method stub

	}

	@Override
	public void lock(UUID applicationId, UUID entityId) throws UGLockException {
		// TODO Auto-generated method stub

	}

	@Override
	public void unlock(UUID applicationId, UUID entityId)
			throws UGLockException {
		// TODO Auto-generated method stub

	}

	@Override
	public void lock(UUID application, UUID... entities) throws UGLockException {
		// TODO Auto-generated method stub
	}

	@Override
	public void unlock(UUID application, UUID... entities)
			throws UGLockException {
		// TODO Auto-generated method stub
	}

	@Override
	public void lock(UUID applicationId, String path) throws UGLockException {
		// TODO Auto-generated method stub
	}

	@Override
	public void unlock(UUID applicationId, String path) throws UGLockException {
		// TODO Auto-generated method stub
	}

	@Override
	public void lock(UUID applicationId, String... paths)
			throws UGLockException {
		// TODO Auto-generated method stub
	}

	@Override
	public void unlock(UUID applicationId, String... paths)
			throws UGLockException {
		// TODO Auto-generated method stub
	}

	@Override
	public void cleanupLocksForThread() throws UGLockException {
		// TODO Auto-generated method stub
	}

	@Override
	public void lockProperty(UUID applicationId, String entityType,
			String propertyName) throws UGLockException {
		// TODO Auto-generated method stub

	}

	@Override
	public void unlockProperty(UUID applicationId, String entityType,
			String path, String propertyName) throws UGLockException {
		// TODO Auto-generated method stub

	}

	@Override
	public void lockProperty(UUID applicationId, String entityType,
			String... propertyNames) throws UGLockException {
		// TODO Auto-generated method stub

	}

	@Override
	public void unlockProperty(UUID applicationId, String entityType,
			String... propertyNames) throws UGLockException {
		// TODO Auto-generated method stub

	}

}
