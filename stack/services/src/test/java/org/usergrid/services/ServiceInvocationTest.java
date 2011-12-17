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
import static org.usergrid.services.ServiceParameter.parameters;
import static org.usergrid.services.ServicePayload.payload;
import static org.usergrid.utils.InflectionUtils.pluralize;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.Query;
import org.usergrid.utils.JsonUtils;

public class ServiceInvocationTest extends AbstractServiceTest {

	private static final Logger logger = LoggerFactory
			.getLogger(ServiceInvocationTest.class);

	@Test
	public void testServices() throws Exception {

		UUID applicationId = createApplication("test");

		ServiceManager sm = smf.getServiceManager(applicationId);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("username", "edanuff");
		properties.put("email", "ed@anuff.com");

		Entity user = testRequest(sm, ServiceAction.POST, 1, properties,
				"users").getEntity();
		assertNotNull(user);

		// test collection
		// /application/00000000-0000-0000-0000-000000000001/users
		// Service application = sm.getService(Application.ENTITY_TYPE);

		testRequest(sm, ServiceAction.GET, 1, null, "users");

		testRequest(sm, ServiceAction.GET, 1, null, "users", user.getUuid());

		testRequest(sm, ServiceAction.GET, 1, null, "users",
				Query.fromQL("select * where username='edanuff'"));

		properties = new LinkedHashMap<String, Object>();
		properties.put("foo", "bar");

		testRequest(sm, ServiceAction.PUT, 1, properties, "users",
				user.getUuid());

		properties = new LinkedHashMap<String, Object>();
		properties.put("name", "nico");

		testRequest(sm, ServiceAction.POST, 1, properties, "cats");

		testRequest(sm, ServiceAction.GET, 0, null, "users", user.getUuid(),
				"messages");

		testRequest(sm, ServiceAction.GET, 0, null, "users",
				Query.fromQL("select * where username='edanuff'"), "messages");

		Entity cat = doCreate(sm, "cat", "dylan");

		testRequest(sm, ServiceAction.GET, 2, null, "cats");

		testRequest(sm, ServiceAction.GET, 1, null, "cats",
				Query.fromQL("select * where name='dylan'"));

		testRequest(sm, ServiceAction.POST, 1, null, "users", "edanuff",
				"likes", cat.getUuid());

		Entity restaurant = doCreate(sm, "restaurant", "Brickhouse");

		sm.getEntityManager().createConnection(user, "likes", restaurant);

		restaurant = doCreate(sm, "restaurant", "Axis Cafe");

		testRequest(sm, ServiceAction.GET, 2, null, "restaurants");

		testRequest(sm, ServiceAction.POST, 1, null, "users", user.getUuid(),
				"connections", "likes", restaurant.getUuid());

		testRequest(sm, ServiceAction.GET, 1, null, "users", "edanuff",
				"likes", "cats");

		testRequest(sm, ServiceAction.GET, 3, null, "users", "edanuff", "likes");

		testRequest(sm, ServiceAction.GET, 2, null, "users", "edanuff",
				"likes", "restaurants");

		testRequest(sm, ServiceAction.GET, 1, null, "users", "edanuff",
				"likes", "restaurants",
				Query.fromQL("select * where name='Brickhouse'"));

		testRequest(sm, ServiceAction.GET, 1, null, "users", "edanuff",
				"likes", Query.fromQL("select * where name='axis*'"));

		testRequest(sm, ServiceAction.GET, 3, null, "users", "edanuff",
				"connections");

		testRequest(sm, ServiceAction.GET, 1, null, "entities", cat.getUuid(),
				"connecting");

		testRequest(sm, ServiceAction.DELETE, 1, null, "users", user.getUuid(),
				"connections", "likes", restaurant.getUuid());

		testRequest(sm, ServiceAction.GET, 2, null, "users", "edanuff",
				"connections");

		testRequest(sm, ServiceAction.GET, 1, null, "users", "edanuff",
				"likes", "restaurants");

		UUID uuid = UUID.randomUUID();
		properties = new LinkedHashMap<String, Object>();
		properties.put("visits", 5);
		testRequest(sm, ServiceAction.PUT, 1, properties, "devices", uuid);

	}

	@Override
	public Entity doCreate(ServiceManager sm, String entityType, String name)
			throws Exception {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", name);

		return testRequest(sm, ServiceAction.POST, 1, properties,
				pluralize(entityType)).getEntity();
	}

	@Override
	public ServiceResults testRequest(ServiceManager sm, ServiceAction action,
			int expectedCount, Map<String, Object> properties, Object... params)
			throws Exception {
		ServiceRequest request = sm.newRequest(action, parameters(params),
				payload(properties));
		logger.info("Request: " + action + " " + request.toString());
		dumpProperties(properties);
		ServiceResults results = request.execute();
		dumpResults(results);
		assertNotNull(results);
		assertEquals(expectedCount, results.getEntities().size());
		return results;
	}

	@Override
	public void dumpProperties(Map<String, Object> properties) {
		if (properties != null) {
			logger.info("Input:\n"
					+ JsonUtils.mapToFormattedJsonString(properties));
		}
	}

	@Override
	public void dumpResults(ServiceResults results) {
		if (results != null) {
			List<Entity> entities = results.getEntities();
			logger.info("Results:\n"
					+ JsonUtils.mapToFormattedJsonString(entities));
		}
	}

	@Override
	public void dumpEntity(Entity entity) {
		if (entity != null) {
			logger.info("Entity:\n"
					+ JsonUtils.mapToFormattedJsonString(entity));
		}
	}

}
