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
import org.usergrid.persistence.EntityManager;

public class GroupServiceTest extends AbstractServiceTest {

	private static final Logger logger = LoggerFactory
			.getLogger(GroupServiceTest.class);

	@Test
	public void testGroups() throws Exception {

		UUID applicationId = createApplication("testGroups");

		ServiceManager sm = smf.getServiceManager(applicationId);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("path", "test/test");
		properties.put("title", "Test group");

		Entity group = testRequest(sm, ServiceAction.POST, 1, properties,
				"groups").getEntity();
		assertNotNull(group);

		testRequest(sm, ServiceAction.GET, 1, null, "groups", "test", "test");

		testRequest(sm, ServiceAction.GET, 0, null, "groups", "test", "test",
				"messages");

		testRequest(sm, ServiceAction.GET, 1, null, "groups");

		properties = new LinkedHashMap<String, Object>();
		properties.put("username", "edanuff");
		properties.put("email", "ed@anuff.com");

		Entity user = testRequest(sm, ServiceAction.POST, 1, properties,
				"users").getEntity();
		assertNotNull(user);

		testRequest(sm, ServiceAction.GET, 0, null, "groups", "test", "test",
				"users");

		testRequest(sm, ServiceAction.POST, 1, null, "groups", "test", "test",
				"users", user.getUuid());

		testRequest(sm, ServiceAction.GET, 1, null, "groups", "test", "test",
				"users");

		testRequest(sm, ServiceAction.GET, 1, null, "users", user.getUuid(),
				"groups");

		testRequest(sm, ServiceAction.GET, 0, null, "users", user.getUuid(),
				"activities");

		testRequest(sm, ServiceAction.GET, 0, null, "groups", group.getUuid(),
				"users", user.getUuid(), "activities");

	}

	@Test
	public void testPermissions() throws Exception {
		logger.info("PermissionsTest.testPermissions");

		UUID applicationId = createApplication("testPermissions");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("path", "mmmeow");

		Entity group = em.create("group", properties);
		assertNotNull(group);

		em.createGroupRole(group.getUuid(), "admin");
		em.createGroupRole(group.getUuid(), "author");

		em.grantGroupRolePermission(group.getUuid(), "admin", "users:access:*");
		em.grantGroupRolePermission(group.getUuid(), "admin", "groups:access:*");
		em.grantGroupRolePermission(group.getUuid(), "author",
				"assets:access:*");

		ServiceManager sm = smf.getServiceManager(applicationId);

		testDataRequest(sm, ServiceAction.GET, null, "groups", group.getUuid(),
				"rolenames");

		testDataRequest(sm, ServiceAction.GET, null, "groups", group.getUuid(),
				"roles", "admin", "permissions");

		testDataRequest(sm, ServiceAction.GET, null, "groups", group.getUuid(),
				"roles", "author", "permissions");

	}

}
