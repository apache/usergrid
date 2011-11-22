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
package org.usergrid.persistence;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for creating Entity objects.
 */
public class EntityFactory {

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(EntityFactory.class);

	/**
	 * New entity.
	 * 
	 * @param <A>
	 *            the generic type
	 * @param id
	 *            the id
	 * @param type
	 *            the type
	 * @param entityClass
	 *            the entity class
	 * @return new entity
	 */
	public static <A extends Entity> A newEntity(UUID id, String type,
			Class<A> entityClass) {
		if (type == null) {
			String errorMsg = "Entity type cannot be null";
			logger.error(errorMsg);
			throw new IllegalArgumentException(errorMsg);
		}
		if ("entity".equalsIgnoreCase(type)
				|| "dynamicentity".equalsIgnoreCase(type)) {
			String errorMsg = "Unable to instantiate entity (" + type
					+ ") because that is not a valid type.";
			logger.error(errorMsg);
			throw new IllegalArgumentException(errorMsg);
		}
		Class<? extends Entity> expectedCls = Schema.getDefaultSchema()
				.getEntityClass(type);
		if ((expectedCls != null)
				&& !DynamicEntity.class.isAssignableFrom(entityClass)
				&& !expectedCls.isAssignableFrom(entityClass)) {
			String errorMsg = "Unable to instantiate entity ("
					+ type
					+ ") because type and entityClass do not match, either use DynamicClass as entityClass or fix mismatch.";
			logger.error(errorMsg);
			throw new IllegalArgumentException(errorMsg);
		} else {
			try {
				A entity = entityClass.newInstance();
				entity.setUuid(id);
				entity.setType(type);
				return entity;
			} catch (IllegalAccessException e) {
				String errorMsg = "Unable to access entity (" + type + "): "
						+ e.getMessage();
				logger.error(errorMsg);
			} catch (InstantiationException e) {
				String errorMsg = "Unable to instantiate entity (" + type
						+ "): " + e.getMessage();
				logger.error(errorMsg);
			}
		}

		return null;
	}

	/**
	 * New entity.
	 * 
	 * @param <A>
	 *            the generic type
	 * @param id
	 *            the id
	 * @param entityClass
	 *            the entity class
	 * @return new entity
	 */
	public static <A extends Entity> A newEntity(UUID id, Class<A> entityClass) {

		if (entityClass == DynamicEntity.class) {
			return null;
		}

		String type = Schema.getDefaultSchema().getEntityType(entityClass);

		return newEntity(id, type, entityClass);
	}

	/**
	 * New entity.
	 * 
	 * @param id
	 *            the id
	 * @param type
	 *            the type
	 * @return new entity
	 */
	public static Entity newEntity(UUID id, String type) {

		Class<? extends Entity> entityClass = Schema.getDefaultSchema()
				.getEntityClass(type);
		if (entityClass == null) {
			entityClass = DynamicEntity.class;
		}

		return newEntity(id, type, entityClass);
	}
}
