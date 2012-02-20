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

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static me.prettyprint.hector.api.factory.HFactory.createRangeSlicesQuery;
import static org.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.usergrid.persistence.Schema.TYPE_APPLICATION;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.addInsertToMutator;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.asMap;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.batchExecute;
import static org.usergrid.persistence.cassandra.CassandraService.APPLICATIONS_CF;
import static org.usergrid.persistence.cassandra.CassandraService.PROPERTIES_CF;
import static org.usergrid.persistence.cassandra.CassandraService.RETRY_COUNT;
import static org.usergrid.utils.ConversionUtils.uuid;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.DynamicCompositeSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.DynamicEntity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.entities.Application;
import org.usergrid.persistence.exceptions.ApplicationAlreadyExistsException;
import org.usergrid.utils.UUIDUtils;

/**
 * Cassandra-specific implementation of Datastore
 * 
 * @author edanuff
 * 
 */
public class EntityManagerFactoryImpl implements EntityManagerFactory {

	private static final Logger logger = LoggerFactory
			.getLogger(EntityManagerFactoryImpl.class);

	public static String IMPLEMENTATION_DESCRIPTION = "Cassandra Entity Manager Factory 1.0";

	public static final Class<DynamicEntity> APPLICATION_ENTITY_CLASS = DynamicEntity.class;

	public static final StringSerializer se = new StringSerializer();
	public static final ByteBufferSerializer be = new ByteBufferSerializer();
	public static final UUIDSerializer ue = new UUIDSerializer();
	public static final BytesArraySerializer bae = new BytesArraySerializer();
	public static final DynamicCompositeSerializer dce = new DynamicCompositeSerializer();
	public static final LongSerializer le = new LongSerializer();

	CassandraService cass;
    CounterUtils counterUtils;

	/**
	 * Must be constructed with a CassandraClientPool.
	 * 
	 * @param cassandraClientPool
	 *            the cassandra client pool
	 */
	public EntityManagerFactoryImpl(CassandraService cass, CounterUtils counterUtils) {
		this.cass = cass;
        this.counterUtils = counterUtils;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.usergrid.core.Datastore#getImpementationDescription()
	 */
	@Override
	public String getImpementationDescription() {
		return IMPLEMENTATION_DESCRIPTION;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.usergrid.core.Datastore#getEntityDao(java.util.UUID,
	 * java.util.UUID)
	 */
	@Override
	public EntityManager getEntityManager(UUID applicationId) {
		return new EntityManagerImpl(this, cass, counterUtils, applicationId);
	}

	/**
	 * Gets the setup.
	 * 
	 * @return Setup helper
	 */
	public Setup getSetup() {
		return new Setup(this, cass);
	}

	@Override
	public void setup() throws Exception {
		Setup setup = getSetup();

		setup.setup();
		setup.checkKeyspaces();

		if (cass.getPropertiesMap() != null) {
			updateServiceProperties(cass.getPropertiesMap());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.usergrid.core.Datastore#createApplication(java.lang.String)
	 */
	@Override
	public UUID createApplication(String name) throws Exception {
		return createApplication(name, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.usergrid.core.Datastore#createApplication(java.lang.String,
	 * java.util.Map)
	 */
	@Override
	public UUID createApplication(String name, Map<String, Object> properties)
			throws Exception {

		name = name.toLowerCase();

		HColumn<String, ByteBuffer> column = cass.getColumn(
				cass.getSystemKeyspace(), APPLICATIONS_CF, name, PROPERTY_UUID);
		if (column != null) {
			throw new ApplicationAlreadyExistsException(name);
			// UUID uuid = uuid(column.getValue());
			// return uuid;
		}

		UUID applicationId = UUIDUtils.newTimeUUID();
		logger.info("New application id " + applicationId.toString());

		initializeApplication(applicationId, name, properties);

		return applicationId;
	}

	public UUID initializeApplication(UUID applicationId, String name,
			Map<String, Object> properties) throws Exception {

		name = name.toLowerCase();

		if (properties == null) {
			properties = new TreeMap<String, Object>(CASE_INSENSITIVE_ORDER);
		}

		properties.put(PROPERTY_NAME, name);

		getSetup().setupApplicationKeyspace(applicationId, name);

		getSetup().checkKeyspaces();

		Keyspace ko = cass.getSystemKeyspace();
		Mutator<ByteBuffer> m = createMutator(ko, be);

		long timestamp = cass.createTimestamp();

		addInsertToMutator(m, APPLICATIONS_CF, name, PROPERTY_UUID,
				applicationId, timestamp);
		addInsertToMutator(m, APPLICATIONS_CF, name, PROPERTY_NAME, name,
				timestamp);

		batchExecute(m, RETRY_COUNT);

		EntityManager em = getEntityManager(applicationId);
		((EntityManagerImpl) em).create(TYPE_APPLICATION,
				APPLICATION_ENTITY_CLASS, properties);

		em.resetRoles();

		return applicationId;
	}

	@Override
	public UUID importApplication(UUID applicationId, String name,
			Map<String, Object> properties) throws Exception {

		name = name.toLowerCase();

		HColumn<String, ByteBuffer> column = cass.getColumn(
				cass.getSystemKeyspace(), APPLICATIONS_CF, name, PROPERTY_UUID);
		if (column != null) {
			throw new ApplicationAlreadyExistsException(name);
			// UUID uuid = uuid(column.getValue());
			// return uuid;
		}

		return initializeApplication(applicationId, name, properties);
	}

	@Override
	public UUID lookupApplication(String name) throws Exception {
		name = name.toLowerCase();
		HColumn<String, ByteBuffer> column = cass.getColumn(
				cass.getSystemKeyspace(), APPLICATIONS_CF, name, PROPERTY_UUID);
		if (column != null) {
			UUID uuid = uuid(column.getValue());
			return uuid;
		}
		return null;
	}

	/**
	 * Gets the application.
	 * 
	 * @param name
	 *            the name
	 * @return application for name
	 * @throws Exception
	 *             the exception
	 */
	public Application getApplication(String name) throws Exception {
		name = name.toLowerCase();
		HColumn<String, ByteBuffer> column = cass.getColumn(
				cass.getSystemKeyspace(), APPLICATIONS_CF, name, PROPERTY_UUID);
		if (column == null) {
			return null;
		}

		UUID applicationId = uuid(column.getValue());

		EntityManager em = getEntityManager(applicationId);
		return ((EntityManagerImpl) em).getEntity(applicationId,
				TYPE_APPLICATION, Application.class);
	}

	@Override
	public Map<String, UUID> getApplications() throws Exception {
		Map<String, UUID> applications = new TreeMap<String, UUID>(
				CASE_INSENSITIVE_ORDER);
		Keyspace ko = cass.getSystemKeyspace();
		RangeSlicesQuery<String, String, UUID> q = createRangeSlicesQuery(ko,
				se, se, ue);
		q.setKeys("", "\uFFFF");
		q.setColumnFamily(APPLICATIONS_CF);
		q.setColumnNames(PROPERTY_UUID);
		QueryResult<OrderedRows<String, String, UUID>> r = q.execute();
		Rows<String, String, UUID> rows = r.get();
		for (Row<String, String, UUID> row : rows) {
			ColumnSlice<String, UUID> slice = row.getColumnSlice();
			HColumn<String, UUID> column = slice.getColumnByName(PROPERTY_UUID);
			applications.put(row.getKey(), column.getValue());
		}
		return applications;
	}

	@Override
	public boolean setServiceProperty(String name, String value) {
		try {
			cass.setColumn(cass.getSystemKeyspace(), PROPERTIES_CF,
					PROPERTIES_CF, name, value);
			return true;
		} catch (Exception e) {
			logger.error("Unable to set property " + name + ": "
					+ e.getMessage());
		}
		return false;
	}

	@Override
	public boolean deleteServiceProperty(String name) {
		try {
			cass.deleteColumn(cass.getSystemKeyspace(), PROPERTIES_CF,
					PROPERTIES_CF, name);
			return true;
		} catch (Exception e) {
			logger.error("Unable to delete property " + name + ": "
					+ e.getMessage());
		}
		return false;
	}

	@Override
	public boolean updateServiceProperties(Map<String, String> properties) {
		try {
			cass.setColumns(cass.getSystemKeyspace(), PROPERTIES_CF,
					PROPERTIES_CF.getBytes(), properties);
			return true;
		} catch (Exception e) {
			logger.error("Unable to update properties: " + e.getMessage());
		}
		return false;
	}

	@Override
	public Map<String, String> getServiceProperties() {
		Map<String, String> properties = null;
		try {
			properties = asMap(cass.getAllColumns(cass.getSystemKeyspace(),
					PROPERTIES_CF, PROPERTIES_CF, se, se));
			return properties;
		} catch (Exception e) {
			logger.error("Unable to load properties: " + e.getMessage());
		}
		return null;
	}

}
