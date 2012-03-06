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
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;
import org.usergrid.utils.JsonUtils;

public class IndexTest extends AbstractPersistenceTest {

	private static final Logger logger = LoggerFactory.getLogger(CollectionTest.class);

	public static final String[] alphabet = { "Alpha", "Bravo", "Charlie",
			"Delta", "Echo", "Foxtrot", "Golf", "Hotel", "India", "Juliet",
			"Kilo", "Lima", "Mike", "November", "Oscar", "Papa", "Quebec",
			"Romeo", "Sierra", "Tango", "Uniform", "Victor", "Whiskey",
			"X-ray", "Yankee", "Zulu" }

	;

	@Test
	public void testCollectionOrdering() throws Exception {
		logger.info("testCollectionOrdering");

		UUID applicationId = createApplication("testCollectionOrdering");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		for (int i = alphabet.length - 1; i >= 0; i--) {
			String name = alphabet[i];
			Map<String, Object> properties = new LinkedHashMap<String, Object>();
			properties.put("name", name);

			em.create("item", properties);

		}

		int i = 0;

		Query query = Query.fromQL("order by name");
		Results r = em.searchCollection(em.getApplicationRef(), "items", query);
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}

		query = Query.fromQL("order by name").withCursor(r.getCursor());
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}

		query = Query.fromQL("order by name").withCursor(r.getCursor());
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}

		assertEquals(alphabet.length, i);

		i = alphabet.length;

		query = Query.fromQL("order by name desc");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		for (Entity entity : r.getEntities()) {
			i--;
			assertEquals(alphabet[i], entity.getProperty("name"));
		}

		query = Query.fromQL("order by name desc").withCursor(r.getCursor());
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		// logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		for (Entity entity : r.getEntities()) {
			i--;
			assertEquals(alphabet[i], entity.getProperty("name"));
		}

		query = Query.fromQL("order by name desc").withCursor(r.getCursor());
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		for (Entity entity : r.getEntities()) {
			i--;
			assertEquals(alphabet[i], entity.getProperty("name"));
		}

		assertEquals(0, i);

	}

	@Test
	public void testCollectionFilters() throws Exception {
		logger.info("testCollectionFilters");

		UUID applicationId = createApplication("testCollectionFilters");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		for (int i = alphabet.length - 1; i >= 0; i--) {
			String name = alphabet[i];
			Map<String, Object> properties = new LinkedHashMap<String, Object>();
			properties.put("name", name);

			em.create("item", properties);

		}

		Query query = Query.fromQL("name < 'delta'");
		Results r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		int i = 0;
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}
		assertEquals(3, i);

		query = Query.fromQL("name <= 'delta'");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 0;
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}
		assertEquals(4, i);

		query = Query.fromQL("name <= 'foxtrot' and name > 'bravo'");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 2;
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}
		assertEquals(6, i);

		query = Query.fromQL("name < 'foxtrot' and name > 'bravo'");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 2;
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}
		assertEquals(5, i);

		query = Query.fromQL("name < 'foxtrot' and name >= 'bravo'");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 1;
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}
		assertEquals(5, i);

		query = Query.fromQL("name <= 'foxtrot' and name >= 'bravo'");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 1;
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}
		assertEquals(6, i);

		query = Query
				.fromQL("name <= 'foxtrot' and name >= 'bravo' order by name desc");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 6;
		for (Entity entity : r.getEntities()) {
			i--;
			assertEquals(alphabet[i], entity.getProperty("name"));
		}
		assertEquals(1, i);

		query = Query
				.fromQL("name < 'foxtrot' and name > 'bravo' order by name desc");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 5;
		for (Entity entity : r.getEntities()) {
			i--;
			assertEquals(alphabet[i], entity.getProperty("name"));
		}
		assertEquals(2, i);

		query = Query
				.fromQL("name < 'foxtrot' and name >= 'bravo' order by name desc");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 5;
		for (Entity entity : r.getEntities()) {
			i--;
			assertEquals(alphabet[i], entity.getProperty("name"));
		}
		assertEquals(1, i);

		query = Query.fromQL("name = 'foxtrot'");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		assertEquals(1, r.size());

		long created = r.getEntity().getCreated();
		query = Query.fromQL("created = " + created);
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		assertTrue(r.size() > 0);

	}

	@Test
	public void testSecondarySorts() throws Exception {
		logger.info("testSecondarySorts");

		UUID applicationId = createApplication("testSecondarySorts");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		for (int i = alphabet.length - 1; i >= 0; i--) {
			String name = alphabet[i];
			Map<String, Object> properties = new LinkedHashMap<String, Object>();
			properties.put("name", name);
			properties.put("group", i / 3);
			properties.put("reverse_name", alphabet[alphabet.length - 1 - i]);

			em.create("item", properties);

		}

		Query query = Query.fromQL("group = 1 order by name desc");
		Results r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		int i = 6;
		for (Entity entity : r.getEntities()) {
			i--;
			assertEquals(1L, entity.getProperty("group"));
			assertEquals(alphabet[i], entity.getProperty("name"));
		}
		assertEquals(3, i);

	}
}
