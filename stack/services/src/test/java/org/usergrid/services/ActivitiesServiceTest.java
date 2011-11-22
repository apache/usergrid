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

		UUID applicationId = createApplication("test");

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

	}

}
