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
package org.usergrid.management;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang.text.StrSubstitutor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.cassandra.ManagementServiceImpl;
import org.usergrid.management.cassandra.ManagementTestHelperImpl;

public class OrganizationTest {

	private static final Logger logger = LoggerFactory
			.getLogger(OrganizationTest.class);

	static ManagementService management;

	static ManagementTestHelper helper;

	@BeforeClass
	public static void setup() throws Exception {
		logger.info("setup");
		assertNull(helper);
		helper = new ManagementTestHelperImpl();
		// helper.setClient(this);
		helper.setup();
		management = helper.getManagementService();
	}

	@AfterClass
	public static void teardown() throws Exception {
		logger.info("teardown");
		helper.teardown();
	}

	@Test
	public void testCreateOrganization() throws Exception {

		UserInfo user = management.createAdminUser("edanuff", "Ed Anuff",
				"ed@anuff.com", "test", false, false, false);
		assertNotNull(user);

		OrganizationInfo organization = management.createOrganization(
				"ed-organization", user);
		assertNotNull(organization);

		Map<UUID, String> userOrganizations = management
				.getOrganizationsForAdminUser(user.getUuid());
		assertEquals("wrong number of organizations", 1,
				userOrganizations.size());

		List<UserInfo> users = management
				.getAdminUsersForOrganization(organization.getUuid());
		assertEquals("wrong number of users", 1, users.size());

		UUID applicationId = management.createApplication(
				organization.getUuid(), "ed-application");
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

		management.activateOrganization(user.getUuid());

		UserInfo u = management.verifyAdminUserPasswordCredentials(user
				.getUuid().toString(), "test");
		assertNotNull(u);

		String token = management.getAccessTokenForAdminUser(user.getUuid());
		assertNotNull(token);

		UUID userId = management.getAdminUserIdFromAccessToken(token);
		assertEquals(user.getUuid(), userId);

	}

	@Test
	public void testEmailStrings() {

		testProperty(ManagementServiceImpl.EMAIL_ADMIN_ACTIVATED, false);
		testProperty(ManagementServiceImpl.EMAIL_ADMIN_ACTIVATION, true);
		testProperty(ManagementServiceImpl.EMAIL_ADMIN_PASSWORD_RESET, true);
		testProperty(ManagementServiceImpl.EMAIL_ADMIN_USER_ACTIVATION, true);
		testProperty(ManagementServiceImpl.EMAIL_ORGANIZATION_ACTIVATED, true);
		testProperty(ManagementServiceImpl.EMAIL_ORGANIZATION_ACTIVATION, true);
		testProperty(ManagementServiceImpl.EMAIL_SYSADMIN_ADMIN_ACTIVATION,
				true);
		testProperty(
				ManagementServiceImpl.EMAIL_SYSADMIN_ORGANIZATION_ACTIVATION,
				true);
		testProperty(ManagementServiceImpl.EMAIL_USER_ACTIVATED, false);
		testProperty(ManagementServiceImpl.EMAIL_USER_ACTIVATION, true);
		testProperty(ManagementServiceImpl.EMAIL_USER_PASSWORD_RESET, true);
		testProperty(ManagementServiceImpl.EMAIL_USER_PIN_REQUEST, true);

	}

	public void testProperty(String propertyName, boolean containsSubstitution) {
		Properties properties = helper.getProperties();
		String propertyValue = properties.getProperty(propertyName);
		assertTrue(propertyName + " was not found", isNotBlank(propertyValue));
		logger.info(propertyName + "=" + propertyValue);

		if (containsSubstitution) {
			Map<String, String> valuesMap = new HashMap<String, String>();
			valuesMap.put("reset_url", "test-url");
			valuesMap.put("organization_name", "test-org");
			valuesMap.put("activation_url", "test-url");
			valuesMap.put("user_email", "test-email");
			valuesMap.put("pin", "test-pin");
			StrSubstitutor sub = new StrSubstitutor(valuesMap);
			String resolvedString = sub.replace(propertyValue);
			assertNotSame(propertyValue, resolvedString);
		}
	}
}
