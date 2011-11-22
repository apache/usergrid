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
