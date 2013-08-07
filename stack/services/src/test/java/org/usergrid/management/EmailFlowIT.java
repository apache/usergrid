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
import static org.junit.Assert.assertTrue;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ADMIN_ACTIVATED;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ADMIN_CONFIRMATION;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ADMIN_PASSWORD_RESET;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ADMIN_USER_ACTIVATION;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ORGANIZATION_ACTIVATED;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ORGANIZATION_CONFIRMATION;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_SYSADMIN_ADMIN_ACTIVATION;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_SYSADMIN_ORGANIZATION_ACTIVATION;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_USER_ACTIVATED;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_USER_CONFIRMATION;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_USER_PASSWORD_RESET;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_USER_PIN_REQUEST;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_NOTIFY_ADMIN_OF_ACTIVATION;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_ORGANIZATIONS_REQUIRE_CONFIRMATION;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_EMAIL;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_USER_ACTIVATION_URL;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_USER_CONFIRMATION_URL;
import static org.usergrid.management.AccountCreationProps.PROPERTIES_USER_RESETPW_URL;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.cassandra.ClearShiroSubject;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.SimpleEntityRef;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.entities.Application;
import org.usergrid.persistence.entities.User;


@Ignore
@Concurrent()
public class EmailFlowIT {

    private static final Logger logger = LoggerFactory
			.getLogger(EmailFlowIT.class);
    private static final String ORGANIZATION_NAME = "email-test-org-1";
    public static final String ORGANIZATION_NAME_2 = "email-test-org-2";

    static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static ServiceTestRule setup = new ServiceTestRule( cassandraResource );


	@Test
	public void testCreateOrganizationAndAdminWithConfirmationOnly()
			throws Exception {

		setup.getProps().setProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS,
                "false");
        setup.getProps().setProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS,
                "false");
        setup.getProps().setProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION,
                "true");
        setup.getProps().setProperty(PROPERTIES_SYSADMIN_EMAIL,
                "sysadmin-1@mockserver.com");
        setup.getProps().setProperty(PROPERTIES_NOTIFY_ADMIN_OF_ACTIVATION,
                "true");

		OrganizationOwnerInfo org_owner = setup.getMgmtSvc()
				.createOwnerAndOrganization(ORGANIZATION_NAME, "test-user-1",
                        "Test User", "test-user-1@mockserver.com",
                        "testpassword", false, false);
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
                setup.getMgmtSvc().handleConfirmationTokenForAdminUser(
                        org_owner.owner.getUuid(), token));

		Message account_activation_message = inbox.get(1);
		assertEquals("User Account Activated",
				account_activation_message.getSubject());

		client = new MockImapClient("mockserver.com", "test-user-1",
				"somepassword");
		client.processMail();
	}

	@Test
	public void testCreateOrganizationAndAdminWithConfirmationAndActivation()
			throws Exception {

        setup.getProps()
				.setProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "true");
        setup.getProps().setProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS,
                "false");
        setup.getProps().setProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION,
                "true");
        setup.getProps().setProperty(PROPERTIES_SYSADMIN_EMAIL,
                "sysadmin-2@mockserver.com");

		OrganizationOwnerInfo org_owner = setup.getMgmtSvc()
				.createOwnerAndOrganization(ORGANIZATION_NAME_2, "test-user-2",
                        "Test User", "test-user-2@mockserver.com",
                        "testpassword", false, false);
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
                setup.getMgmtSvc().handleConfirmationTokenForAdminUser(
                        org_owner.owner.getUuid(), token));

		Message account_confirmed_message = user_inbox.get(1);
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
                setup.getMgmtSvc().handleActivationTokenForAdminUser(
                        org_owner.owner.getUuid(), token));

		Message account_activated_message = user_inbox.get(2);
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
        // TODO better token extraction
		// this is going to get the wrong string if the first part is not
		// text/plain and the url isn't the last character in the email
		return StringUtils.substringAfterLast(body, "token=");
	}

  @Test
  public void skipAllEmailConfiguration() throws Exception {
      setup.getProps().setProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS,
              "false");
      setup.getProps().setProperty(PROPERTIES_ORGANIZATIONS_REQUIRE_CONFIRMATION,
              "false");
      setup.getProps().setProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false");
      setup.getProps().setProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION,
              "false");
    OrganizationOwnerInfo ooi = setup.getMgmtSvc().createOwnerAndOrganization("org-skipallemailtest",
            "user-skipallemailtest", "name-skipallemailtest",
            "nate+skipallemailtest@apigee.com", "password");
    EntityManager em = setup.getEmf().getEntityManager(CassandraService.MANAGEMENT_APPLICATION_ID);
    User user = em.get(ooi.getOwner().getUuid(), User.class);
    assertTrue(user.activated());
    assertFalse(user.disabled());
    assertTrue(user.confirmed());
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

		String propertyValue = setup.getProps().getProperty(propertyName);
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

    @Test
    public void testAppUserActivationResetpwdMail() throws Exception {


    	ApplicationInfo appInfo = setup.getMgmtSvc().getApplicationInfo("test-organization/test-app");
		User user = setupAppUser(appInfo.getId(),"registration_requires_admin_approval", Boolean.TRUE,
				"testAppUserMailUrl", "testAppUserMailUrl@test.com", false);

		String subject = "Request For User Account Activation testAppUserMailUrl@test.com";
		String activation_url = String.format(setup.getProps().getProperty(PROPERTIES_USER_ACTIVATION_URL), "test-organization", "test-app", user.getUuid().toString());
		//Activation
        setup.getMgmtSvc().startAppUserActivationFlow(appInfo.getId(), user);

		List<Message> inbox = org.jvnet.mock_javamail.Mailbox.get("test@usergrid.com");
		assertFalse(inbox.isEmpty());
		MockImapClient client = new MockImapClient("usergrid.com","test", "somepassword");
		client.processMail();

		// subject ok
		Message account_activation_message = inbox.get(0);
		assertEquals(subject, account_activation_message.getSubject());

		// activation url ok
		String mailContent = (String)((MimeMultipart)account_activation_message.getContent()).getBodyPart(1).getContent();
	    logger.info(mailContent);
	    assertTrue(StringUtils.contains(mailContent, activation_url));

	    // token ok
	    String token = getTokenFromMessage(account_activation_message);
		logger.info(token);
		ActivationState activeState = setup.getMgmtSvc().handleActivationTokenForAppUser(appInfo.getId(), user.getUuid(), token);
		assertEquals(ActivationState.ACTIVATED, activeState);

	    subject = "Password Reset";
	    String reset_url = String.format(setup.getProps().getProperty(PROPERTIES_USER_RESETPW_URL), "test-organization", "test-app", user.getUuid().toString());
	    // reset_pwd
        setup.getMgmtSvc().startAppUserPasswordResetFlow(appInfo.getId(), user);


		inbox = org.jvnet.mock_javamail.Mailbox.get("testAppUserMailUrl@test.com");
		assertFalse(inbox.isEmpty());
		client = new MockImapClient("test.com",	"testAppUserMailUrl", "somepassword");
		client.processMail();

		// subject ok
		Message password_reset_message = inbox.get(1);
		assertEquals(subject, password_reset_message.getSubject());

		// resetpwd url ok
		mailContent = (String)((MimeMultipart)password_reset_message.getContent()).getBodyPart(1).getContent();
	    logger.info(mailContent);
	    assertTrue(StringUtils.contains(mailContent, reset_url));

	    // token ok
	    token = getTokenFromMessage(password_reset_message);
		logger.info(token);
	    assertTrue(setup.getMgmtSvc().checkPasswordResetTokenForAppUser(appInfo.getId(), user.getUuid(), token));

      // ensure revoke works
        setup.getMgmtSvc().revokeAccessTokenForAppUser(token);
      assertFalse(setup.getMgmtSvc().checkPasswordResetTokenForAppUser(appInfo.getId(), user.getUuid(), token));
    }

    @Test
    public void testAppUserConfirmationMail() throws Exception {

    	ApplicationInfo appInfo = setup.getMgmtSvc().getApplicationInfo("test-organization/test-app");
        assertNotNull(appInfo);
		User user = setupAppUser(appInfo.getId(),"registration_requires_email_confirmation", Boolean.TRUE,
						"testAppUserConfMail", "testAppUserConfMail@test.com",true);

		String subject = "User Account Confirmation: testAppUserConfMail@test.com";
		String confirmation_url = String.format(setup.getProps().getProperty(PROPERTIES_USER_CONFIRMATION_URL), "test-organization", "test-app", user.getUuid().toString());
		// confirmation
        setup.getMgmtSvc().startAppUserActivationFlow(appInfo.getId(), user);

		List<Message> inbox = org.jvnet.mock_javamail.Mailbox.get("testAppUserConfMail@test.com");
		assertFalse(inbox.isEmpty());
		MockImapClient client = new MockImapClient("test.com","testAppUserConfMail", "somepassword");
		client.processMail();

		// subject ok
		Message account_confirmation_message = inbox.get(0);
		assertEquals(subject, account_confirmation_message.getSubject());

		// confirmation url ok
		String mailContent = (String)((MimeMultipart)account_confirmation_message.getContent()).getBodyPart(1).getContent();
	    logger.info(mailContent);
	    assertTrue(StringUtils.contains(mailContent, confirmation_url));

	    // token ok
	    String token = getTokenFromMessage(account_confirmation_message);
		logger.info(token);
		ActivationState activeState = setup.getMgmtSvc().handleConfirmationTokenForAppUser(appInfo.getId(), user.getUuid(), token);
        assertEquals(ActivationState.CONFIRMED_AWAITING_ACTIVATION, activeState);
    }

    private User setupAppUser(UUID appId, String property, Object value , String username, String email, boolean activated) throws Exception {
    	org.jvnet.mock_javamail.Mailbox.clearAll();

    	EntityManager em = setup.getEmf().getEntityManager(appId);

        em.setProperty(new SimpleEntityRef(Application.ENTITY_TYPE, appId), property, value);

        Map<String, Object> userProps = new LinkedHashMap<String, Object>();
		userProps.put("username", username);
		userProps.put("email", email);
		userProps.put("activated", activated);

        return em.create(User.ENTITY_TYPE, User.class, userProps);
    }
}
