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
