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
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.*;
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

    public Entity postAddAdminToOrg(String organizationName, String email, String password) throws IOException {

        Entity user = this
            .management()
            .orgs()
            .organization(organizationName)
            .users()
            .post(this.getAdminToken(), new User().chainPut("email", email).chainPut("password", password)  );

        assertNotNull(user);
        return user;
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

            postAddAdminToOrg(this.clientSetup.getOrganizationName(), UUIDGenerator.newTimeUUID()+"@email.com", "password");
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
            String adminToken = getAdminToken().getAccessToken();
            Entity node = postAddAdminToOrg("test-organization", "test-admin-nopwd@mockserver.com", "");
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


    /**
     * Adds an existing user to the organization by creating it first in the management collection. Then adding it later.
     * @throws Exception
     */
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
            String adminUserName = "AdminUserFromOtherOrg";
            String adminUserEmail = "AdminUserFromOtherOrg@otherorg.com";

            //A form is REQUIRED to post a user to a management application
            Form userForm = new Form();
            userForm.add( "username", adminUserEmail );
            userForm.add( "name", adminUserEmail );
            userForm.add( "email", adminUserEmail );
            userForm.add( "password", "password1" );

            //Disgusting data manipulation to parse the form response.
            Map adminUserResponse = ( Map<String, Object> ) (management().users().post( User.class, userForm )).get( "data" );
            Entity adminUser = new Entity( ( Map<String, Object> ) adminUserResponse.get( "user" ) );

            refreshIndex();

            assertNotNull(adminUser);

            // this should NOT send resetpwd link in email to newly added org admin user(that
            // already exists in usergrid) only "User Invited To Organization" email
            Entity node = postAddAdminToOrg(this.clientSetup.getOrganizationName(),
                adminUserEmail, "password1");
            UUID userId = node.getUuid();

            assertEquals(adminUser.getUuid(), userId);

            Message[] msgs = getMessages("otherorg.com", adminUserName, "password1");

            // only 1 invited msg
            assertEquals(1, msgs.length);

            String invited = "User Invited To Organization";
            assertEquals(invited, msgs[0].getSubject());
        } finally {
            setTestProperties( originalProperties );
        }
    }


}
