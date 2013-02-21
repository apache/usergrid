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
package org.usergrid.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.management.cassandra.ManagementServiceImpl;
import org.usergrid.management.cassandra.ManagementTestHelperImpl;
import org.usergrid.security.AuthPrincipalInfo;

@RunWith(CassandraRunner.class)
public class OrganizationTest {

	private static final Logger logger = LoggerFactory
			.getLogger(OrganizationTest.class);

	static ManagementService management;


	@BeforeClass
	public static void setup() throws Exception {
		management = CassandraRunner.getBean(ManagementService.class);
	}


	@Test
	public void testCreateOrganization() throws Exception {

		UserInfo user = management.createAdminUser("edanuff", "Ed Anuff",
				"ed@anuff.com", "test", false, false);
		assertNotNull(user);

		OrganizationInfo organization = management.createOrganization(
				"ed-organization", user, false);
		assertNotNull(organization);

		Map<UUID, String> userOrganizations = management
				.getOrganizationsForAdminUser(user.getUuid());
		assertEquals("wrong number of organizations", 1,
				userOrganizations.size());

		List<UserInfo> users = management
				.getAdminUsersForOrganization(organization.getUuid());
		assertEquals("wrong number of users", 1, users.size());

		UUID applicationId = management.createApplication(
				organization.getUuid(), "ed-application")
            .getId();
		assertNotNull(applicationId);

		Map<UUID, String> applications = management
				.getApplicationsForOrganization(organization.getUuid());
		assertEquals("wrong number of applications", 1, applications.size());

		OrganizationInfo organization2 = management
				.getOrganizationForApplication(applicationId);
		assertNotNull(organization2);
		assertEquals("wrong organization name", "ed-organization",
				organization2.getName());

		boolean verified = management.verifyAdminUserPassword(user.getUuid(),
				"test");
		assertTrue(verified);

		management.activateOrganization(organization2);

		UserInfo u = management.verifyAdminUserPasswordCredentials(user
				.getUuid().toString(), "test");
		assertNotNull(u);

		String token = management.getAccessTokenForAdminUser(user.getUuid(), 0);
		assertNotNull(token);

		AuthPrincipalInfo principal = ((ManagementServiceImpl) management)
				.getPrincipalFromAccessToken(token, null, null);
		assertNotNull(principal);
		assertEquals(user.getUuid(), principal.getUuid());

		UserInfo new_user = management.createAdminUser("test-user-1",
				"Test User", "test-user-1@mockserver.com", "testpassword",
				true, true);
		assertNotNull(new_user);

		management.addAdminUserToOrganization(new_user, organization2, false);

	}

}
