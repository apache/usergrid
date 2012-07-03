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
import org.usergrid.persistence.entities.Activity;

public class ActivitiesServiceTest extends AbstractServiceTest {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory
			.getLogger(ActivitiesServiceTest.class);

	@Test
	public void testActivites() throws Exception {

		UUID applicationId = createApplication("testOrganization", "test");

		ServiceManager sm = smf.getServiceManager(applicationId);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("username", "edanuff");
		properties.put("email", "ed@anuff.com");

		Entity userA = testRequest(sm, ServiceAction.POST, 1, properties,
				"users").getEntity();
		assertNotNull(userA);

		properties = new LinkedHashMap<String, Object>();
		properties.put("username", "djacobs");
		properties.put("email", "djacobs@gmail.com");

		Entity userB = testRequest(sm, ServiceAction.POST, 1, properties,
				"users").getEntity();
		assertNotNull(userB);

		properties = new LinkedHashMap<String, Object>();
		properties.put("username", "natpo");
		properties.put("email", "npodrazik@gmail.com");

		Entity userC = testRequest(sm, ServiceAction.POST, 1, properties,
				"users").getEntity();
		assertNotNull(userC);

		testRequest(sm, ServiceAction.POST, 1, null, "users", userB.getUuid(),
				"connections", "following", userA.getUuid());

		testRequest(sm, ServiceAction.POST, 1, null, "users", userC.getUuid(),
				"connections", "following", userA.getUuid());

		properties = Activity.newActivity(Activity.VERB_POST, null,
				"I ate a sammich", null, userA, null, "tweet", null, null)
				.getProperties();

		Entity activity = testRequest(sm, ServiceAction.POST, 1, properties,
				"users", userA.getUuid(), "activities").getEntity();
		assertNotNull(activity);

		testRequest(sm, ServiceAction.GET, 1, null, "users", userA.getUuid(),
				"activities");

		testRequest(sm, ServiceAction.GET, 1, null, "activities");

		testRequest(sm, ServiceAction.GET, 1, null, "users", userB.getUuid(),
				"feed");

		testRequest(sm, ServiceAction.GET, 1, null, "users", userC.getUuid(),
				"feed");

		properties = Activity
				.newActivity(Activity.VERB_POST, null, "I ate another sammich",
						null, userA, null, "tweet", null, null).getProperties();

		activity = testRequest(sm, ServiceAction.POST, 1, properties, "users",
				userA.getUuid(), "activities").getEntity();
		assertNotNull(activity);

		properties = Activity.newActivity(Activity.VERB_POST, null,
				"I ate a cookie", null, userA, null, "tweet", null, null)
				.getProperties();

		activity = testRequest(sm, ServiceAction.POST, 1, properties, "users",
				userA.getUuid(), "activities").getEntity();
		assertNotNull(activity);

		properties = Activity.newActivity(Activity.VERB_CHECKIN, null,
				"I'm at the cookie shop", null, userA, null,
				Activity.OBJECT_TYPE_PLACE, "Cookie Shop", null)
				.getProperties();

		activity = testRequest(sm, ServiceAction.POST, 1, properties, "users",
				userA.getUuid(), "activities").getEntity();
		assertNotNull(activity);

		testRequest(sm, ServiceAction.GET, 4, null, "users", userC.getUuid(),
				"feed");

		testRequest(sm, ServiceAction.GET, 2, null, "users", userC.getUuid(),
				"feed",
				Query.fromQL("select * where content contains 'cookie'"));

		testRequest(
				sm,
				ServiceAction.GET,
				1,
				null,
				"users",
				userC.getUuid(),
				"feed",
				Query.fromQL("select * where verb='post' and content contains 'cookie'"));

		properties = new LinkedHashMap<String, Object>();
		properties.put("username", "finn");
		properties.put("email", "finn@ooo.com");

		Entity userD = testRequest(sm, ServiceAction.POST, 1, properties,
				"users").getEntity();
		assertNotNull(userD);

		testRequest(sm, ServiceAction.POST, 1, null, "users", userD.getUuid(),
				"connections", "following", userA.getUuid());

		testRequest(sm, ServiceAction.GET, 4, null, "users", userD.getUuid(),
				"feed");

	}

}
