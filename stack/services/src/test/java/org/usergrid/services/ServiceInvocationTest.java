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
package org.usergrid.services;

import static org.junit.Assert.assertNotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.Query;

public class ServiceInvocationTest extends AbstractServiceTest {

	public static final Logger logger = LoggerFactory
			.getLogger(ServiceInvocationTest.class);

	@Test
	public void testServices() throws Exception {
		logger.info("testServices");

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

}
