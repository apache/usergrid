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
package org.usergrid.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.utils.JsonUtils;

public class CollectionTest extends AbstractPersistenceTest {

	private static final Logger logger = LoggerFactory
			.getLogger(CollectionTest.class);

	@SuppressWarnings("serial")
	@Test
	public void testCollection() throws Exception {
		UUID applicationId = createApplication("testCollection");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("username", "edanuff");
		properties.put("email", "ed@anuff.com");

		Entity user = em.create("user", properties);
		assertNotNull(user);

		properties = new LinkedHashMap<String, Object>();
		properties.put("actor", new LinkedHashMap<String, Object>() {
			{
				put("displayName", "Ed Anuff");
				put("objectType", "person");
			}
		});
		properties.put("verb", "tweet");
		properties.put("content", "I ate a sammich");

		Entity activity = em.create("activity", properties);
		assertNotNull(activity);

		logger.info("" + activity.getClass());
		logger.info(JsonUtils.mapToFormattedJsonString(activity));

		activity = em.get(activity.getUuid());

		logger.info("" + activity.getClass());
		logger.info(JsonUtils.mapToFormattedJsonString(activity));

		em.addToCollection(user, "activities", activity);

		Results r = em.getCollection(user, "activities", null, null, 10,
				Results.Level.LINKED_PROPERTIES, false);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		assertEquals(1, r.size());

		properties = new LinkedHashMap<String, Object>();
		properties.put("foo", "bar");
		em.updateProperties(new SimpleCollectionRef(user, "activities",
				activity), properties);

		r = em.getCollection(user, "activities", null, null, 10,
				Results.Level.LINKED_PROPERTIES, false);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		assertEquals(1, r.size());

		em.removeFromCollection(user, "activities", activity);

		r = em.getCollection(user, "activities", null, null, 10,
				Results.Level.LINKED_PROPERTIES, false);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		assertEquals(0, r.size());

	}

	@Test
	public void testGroups() throws Exception {
		UUID applicationId = createApplication("testGroups");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("username", "edanuff");
		properties.put("email", "ed@anuff.com");

		Entity user1 = em.create("user", properties);
		assertNotNull(user1);

		properties = new LinkedHashMap<String, Object>();
		properties.put("username", "djacobs");
		properties.put("email", "djacobs@gmail.com");

		Entity user2 = em.create("user", properties);
		assertNotNull(user2);

		properties = new LinkedHashMap<String, Object>();
		properties.put("path", "group1");
		Entity group = em.create("group", properties);
		assertNotNull(group);

		em.addToCollection(group, "users", user1);
		em.addToCollection(group, "users", user2);

		Results r = em.getCollection(group, "users", null, null, 10,
				Results.Level.LINKED_PROPERTIES, false);
		logger.info("Users in group: "
				+ JsonUtils.mapToFormattedJsonString(r.getEntities()));
		assertEquals(2, r.size());

		r = em.getCollection(user1, "groups", null, null, 10,
				Results.Level.LINKED_PROPERTIES, false);
		logger.info("User in groups: "
				+ JsonUtils.mapToFormattedJsonString(r.getEntities()));
		assertEquals(1, r.size());

		properties = new LinkedHashMap<String, Object>();
		properties.put("nickname", "ed");
		em.updateProperties(new SimpleCollectionRef(group, "users", user1),
				properties);

		r = em.getCollection(group, "users", null, null, 10,
				Results.Level.LINKED_PROPERTIES, false);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		assertEquals(2, r.size());

		r = em.searchCollection(group, "users",
				new Query().addEqualityFilter("member.nickname", "ed")
						.withResultsLevel(Results.Level.LINKED_PROPERTIES));
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		assertEquals(1, r.size());

		em.removeFromCollection(user1, "groups", group);

		r = em.getCollection(group, "users", null, null, 10,
				Results.Level.LINKED_PROPERTIES, false);
		logger.info("Users in group: "
				+ JsonUtils.mapToFormattedJsonString(r.getEntities()));
		assertEquals(1, r.size());

		r = em.getCollection(user1, "groups", null, null, 10,
				Results.Level.LINKED_PROPERTIES, false);
		logger.info("User in groups: "
				+ JsonUtils.mapToFormattedJsonString(r.getEntities()));
		assertEquals(0, r.size());

	}

	@Test
	public void testSubkeys() throws Exception {

		UUID applicationId = createApplication("testSubkeys");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("username", "edanuff");
		properties.put("email", "ed@anuff.com");

		Entity user = em.create("user", properties);
		assertNotNull(user);

		properties = new LinkedHashMap<String, Object>();
		properties.put("actor",
				hashMap("displayName", "Ed Anuff").map("objectType", "person"));
		properties.put("verb", "tweet");
		properties.put("content", "I ate a sammich");

		em.addToCollection(user, "activities",
				em.create("activity", properties));

		properties = new LinkedHashMap<String, Object>();
		properties.put("actor",
				hashMap("displayName", "Ed Anuff").map("objectType", "person"));
		properties.put("verb", "post");
		properties.put("content", "I wrote a blog post");

		em.addToCollection(user, "activities",
				em.create("activity", properties));

		properties = new LinkedHashMap<String, Object>();
		properties.put("actor",
				hashMap("displayName", "Ed Anuff").map("objectType", "person"));
		properties.put("verb", "tweet");
		properties.put("content", "I ate another sammich");

		em.addToCollection(user, "activities",
				em.create("activity", properties));

		properties = new LinkedHashMap<String, Object>();
		properties.put("actor",
				hashMap("displayName", "Ed Anuff").map("objectType", "person"));
		properties.put("verb", "post");
		properties.put("content", "I wrote another blog post");

		em.addToCollection(user, "activities",
				em.create("activity", properties));

		Results r = em.searchCollection(user, "activities",
				Query.searchForProperty("verb", "post"));
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		assertEquals(2, r.size());

	}
}
