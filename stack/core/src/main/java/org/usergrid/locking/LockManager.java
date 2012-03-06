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
package org.usergrid.locking;

import java.util.UUID;

import org.usergrid.locking.exception.UGLockException;
import org.usergrid.persistence.EntityRef;

/**
 * This Interface to a class responsible for distributed lock across system.
 * 
 */
public interface LockManager {

	/**
	 * Acquires a reentrant lock for a single entity.
	 * 
	 * @param namaspace
	 *            application where the entity belongs to
	 * @param entity
	 *            an entity whose id needs to be locked
	 * @return a single path read lock
	 * @throws UGLockException
	 */
	void lock(final EntityRef application, final EntityRef entity)
			throws UGLockException;

	void unlock(final EntityRef application, final EntityRef entity)
			throws UGLockException;

	void lock(final EntityRef application, final EntityRef... entities)
			throws UGLockException;

	void unlock(final EntityRef application, final EntityRef... entities)
			throws UGLockException;

	/**
	 * Acquires a reentrant lock for a single entity.
	 * 
	 * @param namaspaceId
	 *            application where the entity belongs to
	 * @param entityId
	 *            entity id
	 * @throws UGLockException
	 */
	void lock(final UUID applicationId, final UUID entityId)
			throws UGLockException;

	void unlock(final UUID applicationId, final UUID entityId)
			throws UGLockException;

	void lock(final UUID application, final UUID... entities)
			throws UGLockException;

	void unlock(final UUID application, final UUID... entities)
			throws UGLockException;

	/**
	 * Acquires a lock on a particular path.
	 * 
	 * @param applicationId
	 *            application UUID
	 * @param path
	 *            a path
	 * @throws UGLockException
	 *             if the lock cannot be acquired
	 */
	void lock(final UUID applicationId, final String path)
			throws UGLockException;

	void unlock(final UUID applicationId, final String path)
			throws UGLockException;

	void lock(final UUID applicationId, final String... paths)
			throws UGLockException;

	void unlock(final UUID applicationId, final String... paths)
			throws UGLockException;

	/**
	 * Acquires a lock on a particular entity property.
	 * 
	 * @param applicationId
	 *            application UUID
	 * @param path
	 *            a path
	 * @throws UGLockException
	 *             if the lock cannot be acquired
	 */
	void lockProperty(final UUID applicationId, final String entityType,
			final String propertyName) throws UGLockException;

	void unlockProperty(final UUID applicationId, final String entityType,
			final String path, final String propertyName)
			throws UGLockException;

	void lockProperty(final UUID applicationId, final String entityType,
			final String... propertyNames) throws UGLockException;

	void unlockProperty(final UUID applicationId, final String entityType,
			final String... propertyNames) throws UGLockException;

	/**
	 * Intended for performing a cleanup for typical web systems where a filter
	 * can invoke this method and make sure the current thread is not holding
	 * any lock.
	 * 
	 * @throws UGLockException
	 */
	void cleanupLocksForThread() throws UGLockException;

}
