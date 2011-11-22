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
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;
import org.usergrid.persistence.entities.Group;
import org.usergrid.persistence.entities.User;
import org.usergrid.utils.JsonUtils;

public class EntityTest {

	private static final Logger logger = LoggerFactory.getLogger(EntityTest.class);

	@Test
	public void testEntityClasses() throws Exception {
		logger.info("testEntityClasses");

		Schema mapper = Schema.getDefaultSchema();

		assertEquals("group", mapper.getEntityType(Group.class));

		assertEquals(User.class, mapper.getEntityClass("user"));

		Entity entity = EntityFactory.newEntity(null, "user");
		assertEquals(User.class, entity.getClass());

		User user = (User) entity;
		user.setUsername("testuser");
		assertEquals(user.getUsername(), user.getProperty("username"));

		user.setProperty("username", "blahblah");
		assertEquals("blahblah", user.getUsername());

		entity = EntityFactory.newEntity(null, "foobar");
		assertEquals(DynamicEntity.class, entity.getClass());

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put(Schema.PROPERTY_UUID, new UUID(1, 2));
		properties.put("foo", "bar");
		entity.setProperties(properties);

		assertEquals(new UUID(1, 2), entity.getUuid());
		assertEquals(new UUID(1, 2), entity.getProperty(Schema.PROPERTY_UUID));
		assertEquals("bar", entity.getProperty("foo"));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testJson() throws Exception {

		User user = new User();
		// user.setId(UUIDUtils.newTimeUUID());
		user.setProperty("foo", "bar");
		assertEquals("{\"type\":\"user\",\"foo\":\"bar\"}",
				JsonUtils.mapToJsonString(user));

		String json = "{\"username\":\"edanuff\", \"bar\" : \"baz\" }";
		Map<String, Object> p = (Map<String, Object>) JsonUtils.parse(json);
		user = new User();
		user.addProperties(p);
		assertEquals("edanuff", user.getUsername());
		assertEquals("baz", user.getProperty("bar"));

		json = "{\"username\":\"edanuff\", \"foo\" : {\"a\":\"bar\", \"b\" : \"baz\" } }";
		p = (Map<String, Object>) JsonUtils.parse(json);
		user = new User();
		user.addProperties(p);
		assertEquals("edanuff", user.getUsername());
		assertTrue(Map.class.isAssignableFrom(user.getProperty("foo")
				.getClass()));
		assertEquals("baz",
				((Map<String, Object>) user.getProperty("foo")).get("b"));

	}
}
