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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityManagerTest extends AbstractPersistenceTest {

	private static final Logger logger = LoggerFactory
			.getLogger(EntityManagerTest.class);

	public EntityManagerTest() {
		super();
	}

	@Test
	public void testEntityManager() throws Exception {
		logger.info("EntityManagerTest.testEntityManagerTest");

		UUID applicationId = createApplication("testEntityManagerTest");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("username", "edanuff");
		properties.put("email", "ed@anuff.com");

		Entity user = em.create("user", properties);
		assertNotNull(user);

		user = em.get(user);
		assertNotNull(user);
		assertEquals("user.username not expected value", "edanuff",
				user.getProperty("username"));
		assertEquals("user.email not expected value", "ed@anuff.com",
				user.getProperty("email"));

		EntityRef userRef = em.getAlias(applicationId, "user", "edanuff");
		assertNotNull(userRef);
		assertEquals("userRef.id not expected value", user.getUuid(),
				userRef.getUuid());
		assertEquals("userRef.type not expected value", "user",
				userRef.getType());

		logger.info("user.username: " + user.getProperty("username"));
		logger.info("user.email: " + user.getProperty("email"));

		Results results = em.searchCollection(em.getApplicationRef(), "users",
				new Query().addEqualityFilter("username", "edanuff"));
		assertNotNull(results);
		assertEquals(1, results.size());
		user = results.getEntity();
		assertNotNull(user);
		assertEquals("user.username not expected value", "edanuff",
				user.getProperty("username"));
		assertEquals("user.email not expected value", "ed@anuff.com",
				user.getProperty("email"));

		logger.info("user.username: " + user.getProperty("username"));
		logger.info("user.email: " + user.getProperty("email"));

		results = em.searchCollection(em.getApplicationRef(), "users",
				new Query().addEqualityFilter("email", "ed@anuff.com"));
		assertNotNull(results);
		assertEquals(1, results.size());
		user = results.getEntity();
		assertNotNull(user);
		assertEquals("user.username not expected value", "edanuff",
				user.getProperty("username"));
		assertEquals("user.email not expected value", "ed@anuff.com",
				user.getProperty("email"));

		logger.info("user.username: " + user.getProperty("username"));
		logger.info("user.email: " + user.getProperty("email"));
	}

	@Test
	public void testCreateAndGet() throws Exception {
		logger.info("EntityDaoTest.testCreateAndGet");

		UUID applicationId = createApplication("testCreateAndGet");

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

	}

	@Test
	public void testDictionaries() throws Exception {
		logger.info("EntityDaoTest.testDictionaries");

		UUID applicationId = createApplication("testDictionaries");

		EntityManager em = emf.getEntityManager(applicationId);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", "thing");
		Entity thing = em.create("thing", properties);
		assertNotNull(thing);
		em.addToDictionary(thing, "stuff", "alpha");
		em.addToDictionary(thing, "stuff", "beta");
		em.addToDictionary(thing, "stuff", "gamma");

		Set<Object> set = em.getDictionaryAsSet(thing, "stuff");
		assertNotNull("list should not be null", set);
		assertEquals("Wrong number of items in list", 3, set.size());

		Iterator<Object> i = set.iterator();
		logger.info("first item is " + i.next());
		logger.info("second item is " + i.next());
		logger.info("third item is " + i.next());

		i = set.iterator();
		assertEquals("first item should be alpha", "alpha", i.next());
		assertEquals("second item should be beta", "beta", i.next());
		assertEquals("third item should be gamma", "gamma", i.next());

		em.addToDictionary(thing, "test", "foo", "bar");
		String val = (String) em
				.getDictionaryElementValue(thing, "test", "foo");
		assertEquals("val should be bar", "bar", val);

		/*
		 * Results r = em.searchCollection(em.getApplicationRef(), "things",
		 * Query.findForProperty("stuff", "beta"));
		 * assertNotNull("results should not be null", r);
		 * assertEquals("Wrong number of items in list", 1, r.size());
		 */
	}

	@Test
	public void testProperties() throws Exception {
		logger.info("EntityDaoTest.testProperties");

		UUID applicationId = createApplication("testProperties");

		EntityManager em = emf.getEntityManager(applicationId);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", "testprop");
		Entity thing = em.create("thing", properties);

		Entity entity = em.get(thing.getUuid());
		assertNotNull("entity should not be null", entity);
		em.setProperty(entity, "alpha", 1);
		em.setProperty(entity, "beta", 2);
		em.setProperty(entity, "gamma", 3);

		Map<String, Object> props = em.getProperties(entity);
		assertNotNull("properties should not be null", props);
		assertEquals("wrong number of properties", 8, props.size());

		assertEquals("wrong value for property alpha", new Long(1),
				props.get("alpha"));
		assertEquals("wrong value for property beta", new Long(2),
				props.get("beta"));
		assertEquals("wrong value for property gamma", new Long(3),
				props.get("gamma"));

		for (Entry<String, Object> entry : props.entrySet()) {
			logger.info(entry.getKey() + " : " + entry.getValue());
		}

		em.deleteProperty(entity, "alpha");

		props = em.getProperties(entity);
		assertNotNull("properties should not be null", props);
		assertEquals("wrong number of properties", 7, props.size());
	}

	@Test
	public void testCreateAndDelete() throws Exception {
		logger.info("EntityDaoTest.testCreateAndDelete");

		UUID applicationId = createApplication("testCreateAndDelete");

		EntityManager em = emf.getEntityManager(applicationId);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", "test.thing.1");
		properties.put("foo", "bar");

		logger.info("Starting entity create");
		Entity thing = em.create("thing", properties);
		logger.info("Entity created");

		logger.info("Starting entity delete");
		em.delete(thing);
		logger.info("Entity deleted");

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testJson() throws Exception {
		logger.info("EntityDaoTest.testProperties");

		UUID applicationId = createApplication("testJson");

		EntityManager em = emf.getEntityManager(applicationId);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", "testprop");
		Entity thing = em.create("thing", properties);

		Entity entity = em.get(thing.getUuid());
		assertNotNull("entity should not be null", entity);

		Map<String, Object> json = new LinkedHashMap<String, Object>();
		json.put("a", "alpha");
		json.put("b", "beta");
		json.put("c", "gamma");

		em.setProperty(entity, "json", json);

		Map<String, Object> props = em.getProperties(entity);
		assertNotNull("properties should not be null", props);
		assertEquals("wrong number of properties", 6, props.size());

		json = (Map<String, Object>) props.get("json");
		assertEquals("wrong size for property alpha", 3, json.size());
		assertEquals("wrong value for property beta", "alpha", json.get("a"));

		em.deleteProperty(entity, "json");

	}

}
