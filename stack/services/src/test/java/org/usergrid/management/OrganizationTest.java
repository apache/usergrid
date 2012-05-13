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

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_EMAIL_ADMIN_ACTIVATED;
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_EMAIL_ADMIN_CONFIRMATION;
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_EMAIL_ADMIN_PASSWORD_RESET;
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_EMAIL_ADMIN_USER_ACTIVATION;
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_EMAIL_ORGANIZATION_ACTIVATED;
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_EMAIL_ORGANIZATION_CONFIRMATION;
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_EMAIL_SYSADMIN_ADMIN_ACTIVATION;
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_EMAIL_SYSADMIN_ORGANIZATION_ACTIVATION;
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_EMAIL_USER_ACTIVATED;
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_EMAIL_USER_CONFIRMATION;
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_EMAIL_USER_PASSWORD_RESET;
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_EMAIL_USER_PIN_REQUEST;

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
import org.usergrid.security.AuthPrincipalInfo;

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

		management.activateOrganization(organization2);

		UserInfo u = management.verifyAdminUserPasswordCredentials(user
				.getUuid().toString(), "test");
		assertNotNull(u);

		String token = management.getAccessTokenForAdminUser(user.getUuid());
		assertNotNull(token);

		AuthPrincipalInfo principal = ((ManagementServiceImpl) management)
				.getPrincipalFromAccessToken(token, null, null);
		assertNotNull(principal);
		assertEquals(user.getUuid(), principal.getUuid());

	}

	@Test
	public void testEmailStrings() {

		testProperty(PROPERTIES_EMAIL_ADMIN_ACTIVATED, false);
		testProperty(PROPERTIES_EMAIL_ADMIN_CONFIRMATION, true);
		testProperty(PROPERTIES_EMAIL_ADMIN_PASSWORD_RESET, true);
		testProperty(PROPERTIES_EMAIL_ADMIN_USER_ACTIVATION, true);
		testProperty(PROPERTIES_EMAIL_ORGANIZATION_ACTIVATED, true);
		testProperty(PROPERTIES_EMAIL_ORGANIZATION_CONFIRMATION, true);
		testProperty(PROPERTIES_EMAIL_SYSADMIN_ADMIN_ACTIVATION, true);
		testProperty(PROPERTIES_EMAIL_SYSADMIN_ORGANIZATION_ACTIVATION, true);
		testProperty(PROPERTIES_EMAIL_USER_ACTIVATED, false);
		testProperty(PROPERTIES_EMAIL_USER_CONFIRMATION, true);
		testProperty(PROPERTIES_EMAIL_USER_PASSWORD_RESET, true);
		testProperty(PROPERTIES_EMAIL_USER_PIN_REQUEST, true);

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
			valuesMap.put("confirmation_url", "test-url");
			valuesMap.put("user_email", "test-email");
			valuesMap.put("pin", "test-pin");
			StrSubstitutor sub = new StrSubstitutor(valuesMap);
			String resolvedString = sub.replace(propertyValue);
			assertNotSame(propertyValue, resolvedString);
		}
	}
}
