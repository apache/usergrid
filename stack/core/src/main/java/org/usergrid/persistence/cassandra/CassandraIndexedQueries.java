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
package org.usergrid.persistence.cassandra;

import static me.prettyprint.hector.api.factory.HFactory.createIndexedSlicesQuery;
import static org.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.usergrid.utils.ConversionUtils.bytebuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.query.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.DynamicEntity;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.Schema;

public class CassandraIndexedQueries {

	private static final Logger logger = LoggerFactory
			.getLogger(CassandraIndexedQueries.class);

	public static final StringSerializer se = new StringSerializer();
	public static final ByteBufferSerializer be = new ByteBufferSerializer();
	public static final EntityValueSerializer ve = new EntityValueSerializer();
	public static final UUIDSerializer ue = new UUIDSerializer();

	public static List<UUID> findEntityIds(Keyspace ko, String entityType,
			Map<String, Object> subkeyProperties, UUID start, int count) {
		List<UUID> ids = new ArrayList<UUID>();

		IndexedSlicesQuery<UUID, String, ByteBuffer> q = createIndexedSlicesQuery(
				ko, ue, se, be);
		q.setColumnFamily(ApplicationCF.ENTITY_PROPERTIES.toString());
		if (start != null) {
			q.setStartKey(start);
		}
		q.setRange(null, null, false, count);
		q.setColumnNames(PROPERTY_UUID);
		q.addEqualsExpression(PROPERTY_TYPE, bytebuffer(entityType));
		if (subkeyProperties != null) {
			for (Map.Entry<String, Object> entry : subkeyProperties.entrySet()) {
				q.addEqualsExpression(entry.getKey(),
						bytebuffer(entry.getValue()));
			}
		}
		QueryResult<OrderedRows<UUID, String, ByteBuffer>> r = q.execute();
		OrderedRows<UUID, String, ByteBuffer> rows = r.get();
		for (Row<UUID, String, ByteBuffer> row : rows) {
			// ColumnSlice<String, byte[]> slice = row.getColumnSlice();
			ids.add(row.getKey());
		}

		return ids;
	}

	public static List<Entity> findEntities(Keyspace ko, String entityType,
			Map<String, Object> subkeyProperties, UUID start, int count,
			String... propertyNames) {
		List<Entity> entities = new ArrayList<Entity>();

		IndexedSlicesQuery<UUID, String, ByteBuffer> q = createIndexedSlicesQuery(
				ko, ue, se, be);
		q.setColumnFamily(ApplicationCF.ENTITY_PROPERTIES.toString());
		if (start != null) {
			q.setStartKey(start);
		}
		q.setRange(null, null, false, count);
		// q.setColumnNames(PROPERTY_ID);
		if ((propertyNames == null) || (propertyNames.length == 0)) {
			propertyNames = Schema.getDefaultSchema()
					.getAllPropertyNamesAsArray();
		}
		q.setColumnNames(propertyNames);
		q.addEqualsExpression(PROPERTY_TYPE, bytebuffer(entityType));
		if (subkeyProperties != null) {
			for (Map.Entry<String, Object> entry : subkeyProperties.entrySet()) {
				q.addEqualsExpression(entry.getKey(),
						bytebuffer(entry.getValue()));
			}
		}
		QueryResult<OrderedRows<UUID, String, ByteBuffer>> r = q.execute();
		OrderedRows<UUID, String, ByteBuffer> rows = r.get();

		for (Row<UUID, String, ByteBuffer> row : rows) {

			ColumnSlice<String, ByteBuffer> slice = row.getColumnSlice();
			if (slice == null) {
				logger.warn("Unable to get slice for row " + row.getKey());
				continue;
			}

			List<HColumn<String, ByteBuffer>> columns = slice.getColumns();
			if (columns == null) {
				logger.warn("Unable to get columns for row " + row.getKey());
				continue;
			}

			Map<String, Object> entityProperties = Schema
					.deserializeEntityProperties(columns);
			if (entityProperties == null) {
				logger.warn("Unable to get correct entities properties from row "
						+ row.getKey());
				continue;
			}

			entityType = (String) entityProperties.get(PROPERTY_TYPE);
			UUID id = (UUID) entityProperties.get(PROPERTY_UUID);

			entities.add(new DynamicEntity(entityType, id, entityProperties));

		}

		return entities;
	}

	/**
	 * Do search entity index.
	 * 
	 * @param applicationId
	 *            the application id
	 * @param entityType
	 *            the entity type
	 * @param outputList
	 *            the output list
	 * @param outputEntities
	 *            the output entities
	 * @param propertyName
	 *            the property name
	 * @param propertyValue
	 *            the property value
	 * @param propertyNames
	 *            the property names
	 * @throws Exception
	 *             the exception
	 */
	public static void doSearchEntityIndex(Keyspace ko, String entityType,
			List<UUID> outputList, List<Entity> outputEntities,
			String propertyName, Object propertyValue, String... propertyNames)
			throws Exception {

		IndexedSlicesQuery<UUID, String, ByteBuffer> q = createIndexedSlicesQuery(
				ko, ue, se, be);
		q.setColumnFamily(ApplicationCF.ENTITY_PROPERTIES.toString());
		if ((propertyNames != null) && (propertyNames.length > 0)
				&& (outputEntities != null)) {
			q.setColumnNames(propertyNames);
		} else {
			q.setColumnNames(PROPERTY_UUID);
		}
		if (entityType != null) {
			q.addEqualsExpression(PROPERTY_TYPE, bytebuffer(entityType));
		}
		if ((propertyName != null) && (propertyValue != null)) {
			q.addEqualsExpression(propertyName, bytebuffer(propertyValue));
		}

		QueryResult<OrderedRows<UUID, String, ByteBuffer>> r = q.execute();
		OrderedRows<UUID, String, ByteBuffer> rows = r.get();

		for (Row<UUID, String, ByteBuffer> row : rows) {
			UUID entityId = row.getKey();

			ColumnSlice<String, ByteBuffer> slice = row.getColumnSlice();
			List<HColumn<String, ByteBuffer>> columns = slice.getColumns();

			logger.info("Indexed Entity " + entityId.toString() + " found");

			if (outputList != null) {
				outputList.add(entityId);
			}
			if (outputEntities != null) {
				Map<String, Object> entityProperties = Schema
						.deserializeEntityProperties(columns);
				if (entityProperties == null) {
					continue;
				}

				String type = (String) entityProperties.get(PROPERTY_TYPE);
				UUID id = (UUID) entityProperties.get(PROPERTY_UUID);
				// EntityInfo entity = new EntityInfo(entityType,
				// id, entityProperties);
				logger.info("Entity2 " + id + " found");

				Entity entity = new DynamicEntity(type, id);
				for (Map.Entry<String, Object> entry : entityProperties
						.entrySet()) {
					entity.setProperty(entry.getKey(), entry.getValue());
				}

				outputEntities.add(entity);
			}
		}

	}

	/**
	 * Search entity index.
	 * 
	 * @param applicationId
	 *            the application id
	 * @param entityType
	 *            the type
	 * @param propertyName
	 *            the property name
	 * @param propertyValue
	 *            the property value
	 * @param propertyNames
	 *            the property names
	 * @return list of entity uuids
	 * @throws Exception
	 *             the exception
	 */
	public static List<UUID> searchEntityIndex(Keyspace ko, String entityType,
			String propertyName, Object propertyValue, String... propertyNames)
			throws Exception {
		List<UUID> results = new ArrayList<UUID>();

		doSearchEntityIndex(ko, entityType, results, null, propertyName,
				propertyValue, propertyNames);

		return results;
	}

	/**
	 * Search entities.
	 * 
	 * @param applicationId
	 *            the application id
	 * @param entityType
	 *            the entity type
	 * @param propertyName
	 *            the property name
	 * @param propertyValue
	 *            the property value
	 * @return list of entity uuids
	 * @throws Exception
	 *             the exception
	 */
	public static List<Entity> searchEntities(Keyspace ko, String entityType,
			String propertyName, Object propertyValue) throws Exception {

		List<Entity> results = new ArrayList<Entity>();

		doSearchEntityIndex(ko, entityType, null, results, propertyName,
				propertyValue, Schema.getDefaultSchema()
						.getPropertyNamesAsArray(entityType));

		return results;
	}

}
