/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.rest.management;


import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.representation.Form;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.management.AccountCreationProps;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.*;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;

import static org.apache.usergrid.management.AccountCreationProps.*;
import static org.junit.Assert.*;


public class RegistrationIT extends AbstractRestIT {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationIT.class);

    public Map<String, Object> getRemoteTestProperties() {
        return clientSetup.getRestClient().testPropertiesResource().get().getProperties();
    }

    /**
     * Sets a management service property locally and remotely.
     */
    public void setTestProperty(String key, Object value) {
        // set the value remotely (in the Usergrid instance running in Tomcat classloader)
        Entity props = new Entity();
        props.put(key, value);
        clientSetup.getRestClient().testPropertiesResource().post(props);

    }

    public void setTestProperties(Map<String, Object> props) {
        Entity properties = new Entity();
        // set the values locally (in the Usergrid instance here in the JUnit classloader
        for (String key : props.keySet()) {
            properties.put(key, props.get(key));

        }

        // set the values remotely (in the Usergrid instance running in Tomcat classloader)
        clientSetup.getRestClient().testPropertiesResource().post(properties);
    }

    public String getTokenFromMessage(Message msg) throws IOException, MessagingException {
        String body = ((MimeMultipart) msg.getContent()).getBodyPart(0).getContent().toString();
        // TODO better token extraction
        // this is going to get the wrong string if the first part is not
        // text/plain and the url isn't the last character in the email
        return StringUtils.substringAfterLast(body, "token=");
    }

    public User postAddAdminToOrg(String organizationName, String email, String password, Token token) throws IOException {

        ApiResponse apiResponse = this
            .management()
            .orgs()
            .organization(organizationName)
            .users()
            .getResource(true, token)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_ATOM_XML_TYPE)
            .post(ApiResponse.class, new User().chainPut("email", email).chainPut("password", password));

        assertNotNull(apiResponse);
        LinkedHashMap map = (LinkedHashMap) apiResponse.getData();
        return new User((LinkedHashMap) map.get("user"));
    }

    private Message[] getMessages(String host, String user, String password) throws MessagingException, IOException {

        Session session = Session.getDefaultInstance(new Properties());
        Store store = session.getStore("imap");
        store.connect(host, user, password);

        Folder folder = store.getFolder("inbox");
        folder.open(Folder.READ_ONLY);
        Message[] msgs = folder.getMessages();

        for (Message m : msgs) {
            logger.info("Subject: " + m.getSubject());
            logger.info(
                "Body content 0 " + ((MimeMultipart) m.getContent()).getBodyPart(0).getContent());
            logger.info(
                "Body content 1 " + ((MimeMultipart) m.getContent()).getBodyPart(1).getContent());
        }
        return msgs;
    }

    @Test
    public void postCreateOrgAndAdmin() throws Exception {

        Map<String, Object> originalProperties = getRemoteTestProperties();

        try {
            setTestProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false");
            setTestProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false");
            setTestProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "false");
            setTestProperty(PROPERTIES_SYSADMIN_EMAIL, "sysadmin-1@usergrid");

            final String username = "registrationUser" + UUIDGenerator.newTimeUUID();
            final String email = username + "@usergrid.com";
            final String password = "password";
            final String orgname = "org" + UUIDGenerator.newTimeUUID();
            final String appname = "app" + UUIDGenerator.newTimeUUID();

            Organization organization = this
                .management()
                .orgs()
                .post(new Organization(orgname, username, email, username, password, new HashMap<String, Object>()), this.getAdminToken());
            Token orgToken = this.management().token().post(new Token(username, password));
            this.management().orgs().organization(organization.getName()).app().post(new Application(appname), orgToken);


            List<Message> inbox = org.jvnet.mock_javamail.Mailbox.get(email);
            Message[] messages = inbox.toArray(new Message[inbox.size()]);
            assertNotEquals(messages.length, 0);

            Message account_confirmation_message = messages[0];
            assertEquals("User Account Confirmation: " + email,
                account_confirmation_message.getSubject());

            String token = getTokenFromMessage(account_confirmation_message);
            logger.info(token);

            setTestProperty(AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_ALLOWED, "false");

            refreshIndex();
            Collection appCollection = clientSetup
                .getRestClient()
                .org(orgname)
                .app(appname)
                .withToken(orgToken).get();
            Entity application = appCollection.next();
            assertNotNull(application);
            Entity appUser = clientSetup
                .getRestClient()
                .org(orgname)
                .app(appname)
                .users()
                .withToken(orgToken)
                .post(new User("testuser", "Test User", "test@usergrid.org", "password"));
            try {
                this.management().orgs().organization(organization.getName()).users().user(username)
                    .getResource(true, getAdminToken())
                    .get(String.class);
                fail("request for disabled admin should fail");
            } catch (UniformInterfaceException uie) {
                assertEquals(401, uie.getResponse().getStatus());
            }
            setTestProperty(AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_ALLOWED, "true");
            Token objToken = management()
                .token()
                .post(new Token(username, password));
            try {
                clientSetup
                    .getRestClient()
                    .org(orgname)
                    .app(appname)
                    .users()
                    .user("testuser")
                    .revoketokens()
                    .withToken(objToken)
                    .put();
            } catch (UniformInterfaceException uie0) {
                fail("Failed to revoke user tokens");
            }
            try {
                clientSetup
                    .getRestClient()
                    .org(orgname)
                    .app(appname)
                    .users()
                    .user("testuser")
                    .withToken(objToken)
                    .put(appUser.chainPut("activated", "false").chainPut("deactivated", System.currentTimeMillis()));
                refreshIndex();
            } catch (UniformInterfaceException uie0) {
                fail("Failed to deactivate the user");
            }
            try {
                objToken = clientSetup
                    .getRestClient()
                    .org(orgname)
                    .app(appname)
                    .token()
                    .post(new Token("testuser", "password"));
                assertNull(objToken.getAccessToken());
                fail("token request for deactivated user should fail");
            } catch (UniformInterfaceException uie) {
                assertTrue(uie.getResponse().getEntity(String.class).contains("user not activated"));
            }

        } finally {
            setTestProperties(originalProperties);
        }
    }


    @Test
    public void putAddToOrganizationFail() throws Exception {

        Map<String, Object> originalProperties = getRemoteTestProperties();

        try {
            setTestProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false");
            setTestProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false");
            setTestProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "false");
            setTestProperty(PROPERTIES_SYSADMIN_EMAIL, "sysadmin-1@mockserver.com");

            String t = this.getAdminToken().getAccessToken();
            Form form = new Form();
            form.add("foo", "bar");
            try {
                this.org().getResource(false).path("/users/test-admin-null@mockserver.com")
                    .queryParam("access_token", t).accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_FORM_URLENCODED).put(String.class, form);
            } catch (UniformInterfaceException e) {
                assertEquals("Should receive a 400 Not Found", 400, e.getResponse().getStatus());
            }
        } finally {
            setTestProperties(originalProperties);
        }
    }


    @Test
    public void postAddToOrganization() throws Exception {

        Map<String, Object> originalProperties = getRemoteTestProperties();

        try {
            setTestProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false");
            setTestProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false");
            setTestProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "false");
            setTestProperty(PROPERTIES_SYSADMIN_EMAIL, "sysadmin-1@mockserver.com");

            postAddAdminToOrg(clientSetup.getOrganizationName(), "test-admin@mockserver.com", "password", getAdminToken());
        } finally {
            setTestProperties(originalProperties);
        }
    }


    @Test
    public void addNewAdminUserWithNoPwdToOrganization() throws Exception {

        Map<String, Object> originalProperties = getRemoteTestProperties();

        try {
            Mailbox.clearAll();
            setTestProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false");
            setTestProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false");
            setTestProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "false");
            setTestProperty(PROPERTIES_SYSADMIN_EMAIL, "sysadmin-1@mockserver.com");

            // this should send resetpwd  link in email to newly added org admin user(that did not exist
            ///in usergrid) and "User Invited To Organization" email
            Entity node = postAddAdminToOrg("test-organization", "test-admin-nopwd@mockserver.com", "", getAdminToken());
            UUID userId = (UUID) node.getMap("data").get("user").get("uuid");

            refreshIndex();

            String subject = "Password Reset";
            Map<String, Object> testProperties = this.getRemoteTestProperties();
            String reset_url = String.format((String) testProperties.get(PROPERTIES_ADMIN_RESETPW_URL), userId.toString());
            String invited = "User Invited To Organization";

            Message[] msgs = getMessages("mockserver.com", "test-admin-nopwd", "password");

            // 1 Invite and 1 resetpwd
            assertTrue(msgs.length == 2);

            //email subject
            assertEquals(subject, msgs[0].getSubject());
            assertEquals(invited, msgs[1].getSubject());

            // reseturl
            String mailContent = (String) ((MimeMultipart) msgs[0].getContent()).getBodyPart(1).getContent();
            logger.info(mailContent);
            assertTrue(StringUtils.contains(mailContent, reset_url));

            //token
            String token = getTokenFromMessage(msgs[0]);
            this
                .management()
                .orgs()
                .organization("test-organization")
                .users()
                .getResource(false)
                .queryParam("access_token", token)
                .get(String.class);
            fail("Should not be able to authenticate an admin with no admin access allowed");
        } catch (UniformInterfaceException uie) {
            assertEquals(401, uie.getResponse().getStatus());
        } finally {
            setTestProperties(originalProperties);
        }
    }


    @Test
    public void addExistingAdminUserToOrganization() throws Exception {

        Map<String, Object> originalProperties = getRemoteTestProperties();

        try {
            Mailbox.clearAll();
            setTestProperty(PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false");
            setTestProperty(PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false");
            setTestProperty(PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "false");
            setTestProperty(PROPERTIES_SYSADMIN_EMAIL, "sysadmin-1@mockserver.com");

            // svcSetup an admin user
            String adminUserName = "AdminUserFromOtherOrg" + UUIDGenerator.newTimeUUID();
            String adminUserEmail = adminUserName + "@otherorg.com";

            User adminUser = new User(adminUserEmail, adminUserEmail, adminUserEmail, "password1");
            Entity apiResponse = management().orgs().organization(clientSetup.getOrganizationName()).users().post(adminUser);
            assertNotNull(apiResponse);

            User adminUserEntity = new User(apiResponse.getMap("data").get("user"));
            assertNotNull(adminUserEntity);

            Message[] msgs = getMessages("otherorg.com", adminUserName, "password1");
            assertNotEquals(0, msgs.length);

            // this should NOT send resetpwd link in email to newly added org admin user(that
            // already exists in usergrid) only "User Invited To Organization" email
            User node = postAddAdminToOrg(clientSetup.getOrganizationName(),
                adminUserEmail, "password1", getAdminToken());

            String uuid = node.getUuid().toString();
            UUID userId = UUID.fromString(uuid);
            assertEquals(adminUserEntity.getUuid(), userId);

            msgs = getMessages("otherorg.com", adminUserName, "password1");

            assertNotEquals(0, msgs.length);

            // check email subject
            String resetpwd = "Password Reset";
            assertNotSame(resetpwd, msgs[1].getSubject());

            String invited = "User Invited To Organization";
            assertEquals(invited, msgs[1].getSubject());
        } finally {
            setTestProperties(originalProperties);
        }
    }
}
