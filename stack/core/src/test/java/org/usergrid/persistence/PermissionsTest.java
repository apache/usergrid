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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionsTest extends AbstractPersistenceTest {

	private static final Logger logger = LoggerFactory
			.getLogger(PermissionsTest.class);

	public PermissionsTest() {
		super();
	}

	@Test
	public void testPermissions() throws Exception {
		logger.info("PermissionsTest.testPermissions");

		UUID applicationId = createApplication("testPermissions");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		// em.createRole("admin", null);
		em.createRole("manager", null);
		em.createRole("member", null);

		Map<String, String> roles = em.getRoles();
		assertEquals("proper number of roles not set", 5, roles.size());
		dump("roles", roles);

		em.deleteRole("member");

		roles = em.getRoles();
		assertEquals("proper number of roles not set", 4, roles.size());
		dump("roles", roles);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("username", "edanuff");
		properties.put("email", "ed@anuff.com");

		Entity user = em.create("user", properties);
		assertNotNull(user);

		properties = new LinkedHashMap<String, Object>();
		properties.put("path", "mmmeow");

		Entity group = em.create("group", properties);
		assertNotNull(user);

		em.addToCollection(group, "users", user);

		em.createGroupRole(group.getUuid(), "admin");
		em.createGroupRole(group.getUuid(), "author");

		roles = em.getGroupRoles(group.getUuid());
		assertEquals("proper number of group roles not set", 2, roles.size());
		dump("group roles", roles);

		em.deleteGroupRole(group.getUuid(), "author");

		roles = em.getGroupRoles(group.getUuid());
		assertEquals("proper number of group roles not set", 1, roles.size());
		dump("group roles", roles);

		em.addUserToGroupRole(user.getUuid(), group.getUuid(), "admin");

		Results r = em.getUsersInGroupRole(group.getUuid(), "admin",
				Results.Level.ALL_PROPERTIES);
		assertEquals("proper number of users in group role not set", 1,
				r.size());
		dump("entities", r.getEntities());

		em.grantRolePermission("admin", "users:access:*");
		em.grantRolePermission("admin", "groups:access:*");

		Set<String> permissions = em.getRolePermissions("admin");
		assertEquals("proper number of role permissions not set", 2,
				permissions.size());
		dump("permissions", permissions);

		em.revokeRolePermission("admin", "groups:access:*");

		permissions = em.getRolePermissions("admin");
		assertEquals("proper number of role permissions not set", 1,
				permissions.size());
		dump("permissions", permissions);

		em.grantGroupRolePermission(group.getUuid(), "admin", "users:access:*");
		em.grantGroupRolePermission(group.getUuid(), "admin", "groups:access:*");

		permissions = em.getGroupRolePermissions(group.getUuid(), "admin");
		assertEquals("proper number of group role permissions not set", 2,
				permissions.size());
		dump("group permissions", permissions);

		em.revokeGroupRolePermission(group.getUuid(), "admin",
				"groups:access:*");

		permissions = em.getGroupRolePermissions(group.getUuid(), "admin");
		assertEquals("proper number of group role permissions not set", 1,
				permissions.size());
		dump("group permissions", permissions);

		roles = em.getRoles();
		assertEquals("proper number of roles not set", 4, roles.size());
		dump("roles", roles);

		em.grantUserPermission(user.getUuid(), "users:access:*");
		em.grantUserPermission(user.getUuid(), "groups:access:*");

		permissions = em.getUserPermissions(user.getUuid());
		assertEquals("proper number of user permissions not set", 2,
				permissions.size());
		dump("user permissions", permissions);

	}
}
