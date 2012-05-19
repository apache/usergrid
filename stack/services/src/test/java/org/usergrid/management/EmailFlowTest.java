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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION;
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
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS;
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS;
import static org.usergrid.management.cassandra.ManagementServiceImpl.PROPERTIES_SYSADMIN_EMAIL;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.cassandra.ManagementTestHelperImpl;

public class EmailFlowTest {

	private static final Logger logger = LoggerFactory
			.getLogger(EmailFlowTest.class);

	static ManagementService management;

	static ManagementTestHelper helper;

	static Properties properties;

	@BeforeClass
	public static void setup() throws Exception {
		logger.info("setup");
		assertNull(helper);
		helper = new ManagementTestHelperImpl();
		// helper.setClient(this);
		helper.setup();
		management = helper.getManagementService();
		properties = helper.getProperties();
	}

	@AfterClass
	public static void teardown() throws Exception {
		logger.info("teardown");
		helper.teardown();
	}

	@Test
	public void testCreateOrganizationAndAdminWithConfirmationOnly()
			throws Exception {

		properties.setProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS,
				"false");
		properties.setProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS,
				"false");
		properties.setProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION,
				"true");
		properties.setProperty(PROPERTIES_SYSADMIN_EMAIL,
				"sysadmin-1@mockserver.com");

		OrganizationOwnerInfo org_owner = management
				.createOwnerAndOrganization("test-org-1", "test-user-1",
						"Test User", "test-user-1@mockserver.com",
						"testpassword", false, false, true);
		assertNotNull(org_owner);

		List<Message> inbox = org.jvnet.mock_javamail.Mailbox
				.get("test-user-1@mockserver.com");

		assertFalse(inbox.isEmpty());

		MockImapClient client = new MockImapClient("mockserver.com",
				"test-user-1", "somepassword");
		client.processMail();

		Message account_confirmation_message = inbox.get(0);
		assertEquals("User Account Confirmation: test-user-1@mockserver.com",
				account_confirmation_message.getSubject());

		String token = getTokenFromMessage(account_confirmation_message);
		logger.info(token);

		assertEquals(
				ActivationState.ACTIVATED,
				management.handleConfirmationTokenForAdminUser(
						org_owner.owner.getUuid(), token));

		Message account_activation_message = inbox.get(2);
		assertEquals("User Account Activated",
				account_activation_message.getSubject());

		client = new MockImapClient("mockserver.com", "test-user-1",
				"somepassword");
		client.processMail();

	}

	@Test
	public void testCreateOrganizationAndAdminWithConfirmationAndActivation()
			throws Exception {

		properties
				.setProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "true");
		properties.setProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS,
				"false");
		properties.setProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION,
				"true");
		properties.setProperty(PROPERTIES_SYSADMIN_EMAIL,
				"sysadmin-2@mockserver.com");

		OrganizationOwnerInfo org_owner = management
				.createOwnerAndOrganization("test-org-2", "test-user-2",
						"Test User", "test-user-2@mockserver.com",
						"testpassword", false, false, true);
		assertNotNull(org_owner);

		List<Message> user_inbox = org.jvnet.mock_javamail.Mailbox
				.get("test-user-2@mockserver.com");

		assertFalse(user_inbox.isEmpty());

		Message account_confirmation_message = user_inbox.get(0);
		assertEquals("User Account Confirmation: test-user-2@mockserver.com",
				account_confirmation_message.getSubject());

		String token = getTokenFromMessage(account_confirmation_message);
		logger.info(token);

		assertEquals(
				ActivationState.CONFIRMED_AWAITING_ACTIVATION,
				management.handleConfirmationTokenForAdminUser(
						org_owner.owner.getUuid(), token));

		Message account_confirmed_message = user_inbox.get(2);
		assertEquals("User Account Confirmed",
				account_confirmed_message.getSubject());

		List<Message> sysadmin_inbox = org.jvnet.mock_javamail.Mailbox
				.get("sysadmin-2@mockserver.com");
		assertFalse(sysadmin_inbox.isEmpty());

		Message account_activation_message = sysadmin_inbox.get(0);
		assertEquals(
				"Request For Admin User Account Activation test-user-2@mockserver.com",
				account_activation_message.getSubject());

		token = getTokenFromMessage(account_activation_message);
		logger.info(token);

		assertEquals(
				ActivationState.ACTIVATED,
				management.handleActivationTokenForAdminUser(
						org_owner.owner.getUuid(), token));

		Message account_activated_message = user_inbox.get(3);
		assertEquals("User Account Activated",
				account_activated_message.getSubject());

		MockImapClient client = new MockImapClient("mockserver.com",
				"test-user-2", "somepassword");
		client.processMail();

	}

	public String getTokenFromMessage(Message msg) throws IOException,
			MessagingException {
		String body = ((MimeMultipart) msg.getContent()).getBodyPart(0)
				.getContent().toString();
		String token = StringUtils.substringAfterLast(body, "token=");
		// TODO better token extraction
		// this is going to get the wrong string if the first part is not
		// text/plain and the url isn't the last character in the email
		return token;
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
