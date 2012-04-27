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
import org.usergrid.persistence.EntityManager;

public class UsersServiceTest extends AbstractServiceTest {

	private static final Logger logger = LoggerFactory
			.getLogger(UsersServiceTest.class);

	@Test
	public void testPermissions() throws Exception {
		logger.info("PermissionsTest.testPermissions");

		UUID applicationId = createApplication("testOrganization","testPermissions");
		assertNotNull(applicationId);

		ServiceManager sm = smf.getServiceManager(applicationId);
		assertNotNull(sm);

		EntityManager em = sm.getEntityManager();
		assertNotNull(em);

		// em.createRole("admin", null);
		em.createRole("manager", null);
		em.createRole("member", null);

		em.grantRolePermission("admin", "users:access:*");
		em.grantRolePermission("admin", "groups:access:*");

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("username", "edanuff");
		properties.put("email", "ed@anuff.com");

		Entity user = em.create("user", properties);
		assertNotNull(user);

		// em.addUserToRole(user.getUuid(), "admin");
		testRequest(sm, ServiceAction.POST, 1, null, "users", user.getUuid(),
				"roles", "admin");
		// em.addUserToRole(user.getUuid(), "manager");
		testRequest(sm, ServiceAction.POST, 1, null, "users", user.getUuid(),
				"roles", "manager");

		em.grantUserPermission(user.getUuid(), "users:access:*");
		em.grantUserPermission(user.getUuid(), "groups:access:*");

		testDataRequest(sm, ServiceAction.GET, null, "users", user.getUuid(),
				"rolenames");

		testDataRequest(sm, ServiceAction.GET, null, "users", user.getUuid(),
				"permissions");

		testDataRequest(sm, ServiceAction.GET, null, "users", user.getUuid(),
				"roles", "admin", "permissions");

	}

}
