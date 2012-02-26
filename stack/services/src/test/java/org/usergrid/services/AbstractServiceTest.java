/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Stack.
 * 
 * Usergrid Stack is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * Usergrid Stack is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License along
 * with Usergrid Stack. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU AGPL version 3 section 7
 * 
 * Linking Usergrid Stack statically or dynamically with other modules is making
 * a combined work based on Usergrid Stack. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 * 
 * In addition, as a special exception, the copyright holders of Usergrid Stack
 * give you permission to combine Usergrid Stack with free software programs or
 * libraries that are released under the GNU LGPL and with independent modules
 * that communicate with Usergrid Stack solely through:
 * 
 *   - Classes implementing the org.usergrid.services.Service interface
 *   - Apache Shiro Realms and Filters
 *   - Servlet Filters and JAX-RS/Jersey Filters
 * 
 * You may copy and distribute such a system following the terms of the GNU AGPL
 * for Usergrid Stack and the licenses of the other code concerned, provided that
 ******************************************************************************/
package org.usergrid.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.usergrid.persistence.cassandra.CassandraService.DEFAULT_APPLICATION_ID;
import static org.usergrid.services.ServiceParameter.parameters;
import static org.usergrid.services.ServicePayload.payload;
import static org.usergrid.utils.InflectionUtils.pluralize;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.PersistenceTestHelper;
import org.usergrid.persistence.cassandra.EntityManagerFactoryImpl;
import org.usergrid.persistence.cassandra.PersistenceTestHelperImpl;
import org.usergrid.utils.JsonUtils;

public abstract class AbstractServiceTest {
	public static final boolean USE_DEFAULT_DOMAIN = false;

	private static final Logger logger = LoggerFactory
			.getLogger(AbstractServiceTest.class);

	static PersistenceTestHelper helper;

	public AbstractServiceTest() {
		emf = (EntityManagerFactoryImpl) helper.getEntityManagerFactory();
		smf = new ServiceManagerFactory(emf);
		smf.setApplicationContext(helper.getApplicationContext());
	}

	@BeforeClass
	public static void setup() throws Exception {
		logger.info("setup");
		assertNull(helper);
		helper = new PersistenceTestHelperImpl();
		// helper.setClient(this);
		helper.setup();
	}

	@AfterClass
	public static void teardown() throws Exception {
		logger.info("teardown");
		helper.teardown();
	}

	EntityManagerFactoryImpl emf;
	ServiceManagerFactory smf;

	public ServiceManagerFactory getServiceManagerFactory() {
		return smf;
	}

	public void setServiceManagerFactory(ServiceManagerFactory smf) {
		this.smf = smf;
	}

	@Autowired
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = (EntityManagerFactoryImpl) emf;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return emf;
	}

	UUID dId = null;

	public UUID createApplication(String name) throws Exception {
		if (USE_DEFAULT_DOMAIN) {
			return DEFAULT_APPLICATION_ID;
		}
		return emf.createApplication(name);
	}

	public Entity doCreate(ServiceManager sm, String entityType, String name)
			throws Exception {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", name);

		return testRequest(sm, ServiceAction.POST, 1, properties,
				pluralize(entityType)).getEntity();
	}

	public ServiceResults testRequest(ServiceManager sm, ServiceAction action,
			int expectedCount, Map<String, Object> properties, Object... params)
			throws Exception {
		ServiceRequest request = sm.newRequest(action, parameters(params),
				payload(properties));
		logger.info("Request: " + action + " " + request.toString());
		dumpProperties(properties);
		ServiceResults results = request.execute();
		assertNotNull(results);
		assertEquals(expectedCount, results.getEntities().size());
		dumpResults(results);
		return results;
	}

	public void dumpProperties(Map<String, Object> properties) {
		dump("Input", properties);
	}

	public void dumpResults(ServiceResults results) {
		if (results != null) {
			List<Entity> entities = results.getEntities();
			dump("Results", entities);
		}
	}

	public void dumpEntity(Entity entity) {
		dump("Entity", entity);
	}

	public void dump(Object obj) {
		dump("Object", obj);
	}

	public void dump(String name, Object obj) {
		if (obj != null) {
			logger.info(name + ":\n" + JsonUtils.mapToFormattedJsonString(obj));
		}
	}

	public ServiceResults testDataRequest(ServiceManager sm,
			ServiceAction action, Map<String, Object> properties,
			Object... params) throws Exception {
		ServiceRequest request = sm.newRequest(action, parameters(params),
				payload(properties));
		logger.info("Request: " + action + " " + request.toString());
		dumpProperties(properties);
		ServiceResults results = request.execute();
		assertNotNull(results);
		assertNotNull(results.getData());
		dump(results.getData());
		return results;
	}

}
