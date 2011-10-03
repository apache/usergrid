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
