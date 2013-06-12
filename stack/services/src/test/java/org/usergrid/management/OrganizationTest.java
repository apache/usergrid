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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.management.cassandra.ManagementServiceImpl;
import org.usergrid.management.exceptions.RecentlyUsedPasswordException;
import org.usergrid.persistence.CredentialsInfo;
import org.usergrid.security.AuthPrincipalInfo;
import org.usergrid.test.ShiroHelperRunner;

import static org.junit.Assert.*;

@RunWith(ShiroHelperRunner.class)
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

  @Test
  public void testPasswordHistoryCheck() throws Exception {

    String[] passwords = new String[] {"password1", "password2", "password3", "password4"};

    UserInfo user = management.createAdminUser("edanuff2", "Ed Anuff", "ed2@anuff.com", passwords[0], true, false);
    assertNotNull(user);

    OrganizationInfo organization = management.createOrganization("ed-organization2", user, true);
    assertNotNull(organization);

    // no history, no problem
    management.setAdminUserPassword(user.getUuid(), passwords[1]);
    management.setAdminUserPassword(user.getUuid(), passwords[0]);
    management.setAdminUserPassword(user.getUuid(), passwords[0]);

    // set history to 2
    Map<String,Object> props = new HashMap<String,Object>();
    props.put(OrganizationInfo.PASSWORD_HISTORY_SIZE_KEY, 2);
    organization.setProperties(props);
    management.updateOrganization(organization);

    // check the history
    management.setAdminUserPassword(user.getUuid(), passwords[1]); // ok
    management.setAdminUserPassword(user.getUuid(), passwords[2]); // ok
    try {
      management.setAdminUserPassword(user.getUuid(), passwords[0]);
      fail("password change should fail");
    } catch (RecentlyUsedPasswordException e) {
      // ok
    }
    try {
      management.setAdminUserPassword(user.getUuid(), passwords[1]);
      fail("password change should fail");
    } catch (RecentlyUsedPasswordException e) {
      // ok
    }
    try {
      management.setAdminUserPassword(user.getUuid(), passwords[2]);
      fail("password change should fail");
    } catch (RecentlyUsedPasswordException e) {
      // ok
    }
    management.setAdminUserPassword(user.getUuid(), passwords[3]); // ok
    management.setAdminUserPassword(user.getUuid(), passwords[0]); // ok

    // reduce the history to 1
    props = new HashMap<String,Object>();
    props.put(OrganizationInfo.PASSWORD_HISTORY_SIZE_KEY, 1);
    organization.setProperties(props);
    management.updateOrganization(organization);

    management.setAdminUserPassword(user.getUuid(), passwords[1]); // ok
    try {
      management.setAdminUserPassword(user.getUuid(), passwords[0]);
      fail("password change should fail");
    } catch (RecentlyUsedPasswordException e) {
      // ok
    }

    // test history size w/ user belonging to 2 orgs

    OrganizationInfo organization2 = management.createOrganization("ed-organization3", user, false);
    assertNotNull(organization);

    Map<UUID, String> userOrganizations = management.getOrganizationsForAdminUser(user.getUuid());
    assertEquals("wrong number of organizations", 2, userOrganizations.size());

    props = new HashMap<String,Object>();
    props.put(OrganizationInfo.PASSWORD_HISTORY_SIZE_KEY, 2);
    organization2.setProperties(props);
    management.updateOrganization(organization2);

    try {
      management.setAdminUserPassword(user.getUuid(), passwords[1]);
      fail("password change should fail");
    } catch (RecentlyUsedPasswordException e) {
      // ok
    }
    try {
      management.setAdminUserPassword(user.getUuid(), passwords[0]);
      fail("password change should fail");
    } catch (RecentlyUsedPasswordException e) {
      // ok
    }
    management.setAdminUserPassword(user.getUuid(), passwords[2]);
    try {
      management.setAdminUserPassword(user.getUuid(), passwords[0]);
      fail("password change should fail");
    } catch (RecentlyUsedPasswordException e) {
      // ok
    }
    try {
      management.setAdminUserPassword(user.getUuid(), passwords[1]);
      fail("password change should fail");
    } catch (RecentlyUsedPasswordException e) {
      // ok
    }
  }

}
