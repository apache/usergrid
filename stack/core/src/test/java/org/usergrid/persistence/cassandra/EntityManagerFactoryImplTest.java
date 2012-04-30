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
package org.usergrid.persistence.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.PersistenceTestHelper;
import org.usergrid.persistence.Results;

public class EntityManagerFactoryImplTest {

	public static final boolean USE_DEFAULT_DOMAIN = !CassandraService.USE_VIRTUAL_KEYSPACES;

	private static final Logger logger = LoggerFactory
			.getLogger(EntityManagerFactoryImplTest.class);

	static PersistenceTestHelper helper;

	public EntityManagerFactoryImplTest() {
		emf = (EntityManagerFactoryImpl) helper.getEntityManagerFactory();
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

	@Autowired
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = (EntityManagerFactoryImpl) emf;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return emf;
	}

	public UUID createApplication(String organizationName, String applicationName) throws Exception {
		if (USE_DEFAULT_DOMAIN) {
			return CassandraService.DEFAULT_APPLICATION_ID;
		}
		return emf.createApplication(organizationName, applicationName);
	}

	@Test
	public void testCreateAndGet() throws Exception {
		logger.info("EntityDaoTest.testCreateAndGet");

		UUID applicationId = createApplication("testOrganization","testCreateAndGet");
		logger.info("Application id " + applicationId);

		EntityManager em = emf.getEntityManager(applicationId);

		int i = 0;
		List<Entity> things = new ArrayList<Entity>();
		for (i = 0; i < 10; i++) {
			Map<String, Object> properties = new LinkedHashMap<String, Object>();
			properties.put("name", "thing" + i);

			Entity thing = em.create("thing", properties);
			assertNotNull("thing should not be null", thing);
			assertFalse("thing id not valid",
					thing.getUuid().equals(new UUID(0, 0)));
			assertEquals("name not expected value", "thing" + i,
					thing.getProperty("name"));

			things.add(thing);
		}
		assertEquals("should be ten entities", 10, things.size());

		i = 0;
		for (Entity entity : things) {

			Entity thing = em.get(entity.getUuid());
			assertNotNull("thing should not be null", thing);
			assertFalse("thing id not valid",
					thing.getUuid().equals(new UUID(0, 0)));
			assertEquals("name not expected value", "thing" + i,
					thing.getProperty("name"));

			i++;
		}

		List<UUID> ids = new ArrayList<UUID>();
		for (Entity entity : things) {
			ids.add(entity.getUuid());

			Entity en = em.get(entity.getUuid());
			String type = en.getType();
			assertEquals("type not expected value", "thing", type);

			Object property = en.getProperty("name");
			assertNotNull("thing name property should not be null", property);
			assertTrue("thing name should start with \"thing\"", property
					.toString().startsWith("thing"));

			Map<String, Object> properties = en.getProperties();
			assertEquals("number of properties wrong", 5, properties.size());
		}

		i = 0;
		Results results = em.get(ids, Results.Level.CORE_PROPERTIES);
		for (Entity thing : results) {
			assertNotNull("thing should not be null", thing);

			assertFalse("thing id not valid",
					thing.getUuid().equals(new UUID(0, 0)));

			assertEquals("wrong type", "thing", thing.getType());

			assertNotNull("thing name should not be null",
					thing.getProperty("name"));
			String name = thing.getProperty("name").toString();
			assertEquals("unexpected name", "thing" + i, name);

			i++;

		}

		assertEquals("entities unfound entity name count incorrect", 10, i);

		/*
		 * List<UUID> entities = emf.findEntityIds(applicationId, "thing", null,
		 * null, 100); assertNotNull("entities list should not be null",
		 * entities); assertEquals("entities count incorrect", 10,
		 * entities.size());
		 */

	}

}
