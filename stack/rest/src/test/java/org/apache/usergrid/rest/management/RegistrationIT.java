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


import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.eaio.uuid.UUIDGen;
import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.ITSetup;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.security.TestAppUser;
import org.apache.usergrid.rest.test.security.TestUser;

import org.apache.commons.lang.StringUtils;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_ADMIN_RESETPW_URL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_EMAIL;
import static org.apache.usergrid.utils.MapUtils.hashMap;


public class RegistrationIT extends AbstractRestIT {

    private static final Logger logger = LoggerFactory.getLogger( RegistrationIT.class );

    private static final ITSetup setup = ITSetup.getInstance();

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test
    public void postCreateOrgAndAdmin() throws Exception {

        Map<String, String> originalProperties = getRemoteTestProperties();

        try {
            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false" );
            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
            setTestProperty( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "false" );
            setTestProperty( PROPERTIES_SYSADMIN_EMAIL, "sysadmin-1@mockserver.com" );

//            JsonNode node = postCreateOrgAndAdmin( "test-org-1", "test-user-1", "Test User",
//                    "test-user-1@mockserver.com", "testpassword" );


            final String username = "registrationUser"+UUIDGenerator.newTimeUUID();
            final String email = username+"@usergrid.com" ;
            final String password = "password";

            final TestUser user1 = new TestAppUser(username , password, email);

            context.withOrg( "org" + UUIDGenerator.newTimeUUID() ).withApp( "app" + UUIDGenerator.newTimeUUID() ).withUser( user1 ).createNewOrgAndUser();
            context.createAppForOrg();

            final UUID owner_uuid = context.getActiveUser().getUuid();

//            refreshIndex("test-organization", "test-app");
//
//            UUID owner_uuid =
//                    UUID.fromString( node.findPath( "data" ).findPath( "owner" ).findPath( "uuid" ).textValue() );

            List<Message> inbox = org.jvnet.mock_javamail.Mailbox.get( "test-user-1@mockserver.com" );

            assertFalse( inbox.isEmpty() );

            Message account_confirmation_message = inbox.get( 0 );
            assertEquals( "User Account Confirmation: " + email,
                    account_confirmation_message.getSubject() );

            String token = getTokenFromMessage( account_confirmation_message );
            logger.info( token );

            setup.getMgmtSvc().disableAdminUser( owner_uuid );

            refreshIndex(context.getOrgName(), context.getAppName());

            try {
                resource().path( "/management/token" ).queryParam( "grant_type", "password" )
                        .queryParam( "username", username ).queryParam( "password", password )
                        .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                        .get( String.class );
                fail( "request for disabled user should fail" );
            }
            catch ( UniformInterfaceException uie ) {
                ClientResponse.Status status = uie.getResponse().getClientResponseStatus();
                JsonNode body = mapper.readTree( uie.getResponse().getEntity( String.class ));
                assertEquals( "user disabled", body.findPath( "error_description" ).textValue() );
            }

            setup.getMgmtSvc().deactivateUser( setup.getEmf().getManagementAppId(), owner_uuid );
            try {
                resource().path( "/management/token" ).queryParam( "grant_type", "password" )
                        .queryParam( "username", username ).queryParam( "password", password)
                        .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                        .get( String.class );
                fail( "request for deactivated user should fail" );
            }
            catch ( UniformInterfaceException uie ) {
                ClientResponse.Status status = uie.getResponse().getClientResponseStatus();
                JsonNode body = mapper.readTree( uie.getResponse().getEntity( String.class ));
                assertEquals( "user not activated", body.findPath( "error_description" ).textValue() );
            }

            // assertEquals(ActivationState.ACTIVATED,
            // svcSetup.getMgmtSvc().handleConfirmationTokenForAdminUser(
            // owner_uuid, token));

            // need to enable JSP usage in the test version of Jetty to make this test run
            //      String response = resource()
            //        .path("/management/users/" + owner_uuid + "/confirm").get(String.class);
            //      logger.info(response);
            //      Message account_activation_message = inbox.get(1);
            //      assertEquals("User Account Activated", account_activation_message.getSubject());

        }
        finally {
            setTestProperties( originalProperties );
        }
    }


    public String getTokenFromMessage( Message msg ) throws IOException, MessagingException {
        String body = ( ( MimeMultipart ) msg.getContent() ).getBodyPart( 0 ).getContent().toString();
        String token = StringUtils.substringAfterLast( body, "token=" );
        // TODO better token extraction
        // this is going to get the wrong string if the first part is not
        // text/plain and the url isn't the last character in the email
        return token;
    }



    @SuppressWarnings({ "unchecked", "rawtypes" })
    public JsonNode postAddAdminToOrg( String organizationName, String email, String password, String token ) throws IOException {
        JsonNode node = null;

        MultivaluedMap formData = new MultivaluedMapImpl();
        formData.add( "email", email );
        formData.add( "password", password );

        node = mapper.readTree( resource().path( "/management/organizations/" + organizationName + "/users" )
                .queryParam( "access_token", token ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_FORM_URLENCODED ).post( String.class, formData ));

        assertNotNull( node );
        logNode( node );
        return node;
    }


    @Test
    public void putAddToOrganizationFail() throws Exception {

        Map<String, String> originalProperties = getRemoteTestProperties();

        try {
            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false" );
            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
            setTestProperty( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "false" );
            setTestProperty( PROPERTIES_SYSADMIN_EMAIL, "sysadmin-1@mockserver.com" );

            String t = adminToken();
            MultivaluedMap formData = new MultivaluedMapImpl();
            formData.add( "foo", "bar" );
            try {
                resource().path( "/management/organizations/test-organization/users/test-admin-null@mockserver.com" )
                        .queryParam( "access_token", t ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_FORM_URLENCODED ).put( String.class, formData );
            }
            catch ( UniformInterfaceException e ) {
                assertEquals( "Should receive a 400 Not Found", 400, e.getResponse().getStatus() );
            }
        }
        finally {
            setTestProperties( originalProperties );
        }
    }


    @Test
    public void postAddToOrganization() throws Exception {

        Map<String, String> originalProperties = getRemoteTestProperties();

        try {
            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false" );
            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
            setTestProperty( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "false" );
            setTestProperty( PROPERTIES_SYSADMIN_EMAIL, "sysadmin-1@mockserver.com" );

            String t = adminToken();
            postAddAdminToOrg( "test-organization", "test-admin@mockserver.com", "password", t );
        }
        finally {
            setTestProperties( originalProperties );
        }
    }


    @Test
    public void addNewAdminUserWithNoPwdToOrganization() throws Exception {

        Map<String, String> originalProperties = getRemoteTestProperties();

        try {
            Mailbox.clearAll();
            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false" );
            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
            setTestProperty( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "false" );
            setTestProperty( PROPERTIES_SYSADMIN_EMAIL, "sysadmin-1@mockserver.com" );

            // this should send resetpwd  link in email to newly added org admin user(that did not exist
            ///in usergrid) and "User Invited To Organization" email
            String adminToken = adminToken();
            JsonNode node = postAddAdminToOrg( "test-organization", "test-admin-nopwd@mockserver.com", "", adminToken );
            String uuid = node.get( "data" ).get( "user" ).get( "uuid" ).textValue();
            UUID userId = UUID.fromString( uuid );

            refreshIndex("test-organization", "test-app");

            String subject = "Password Reset";
            String reset_url = String.format( setup.getProps().getProperty( PROPERTIES_ADMIN_RESETPW_URL ), uuid );
            String invited = "User Invited To Organization";

            Message[] msgs = getMessages( "mockserver.com", "test-admin-nopwd", "password" );

            // 1 Invite and 1 resetpwd
            assertTrue( msgs.length == 2 );

            //email subject
            assertEquals( subject, msgs[0].getSubject() );
            assertEquals( invited, msgs[1].getSubject() );

            // reseturl
            String mailContent = ( String ) ( ( MimeMultipart ) msgs[0].getContent() ).getBodyPart( 1 ).getContent();
            logger.info( mailContent );
            assertTrue( StringUtils.contains( mailContent, reset_url ) );

            //token
            String token = getTokenFromMessage( msgs[0] );
            assertTrue( setup.getMgmtSvc().checkPasswordResetTokenForAdminUser( userId, token ) );
        }
        finally {
            setTestProperties( originalProperties );
        }
    }


    @Test
    public void addExistingAdminUserToOrganization() throws Exception {

        Map<String, String> originalProperties = getRemoteTestProperties();

        try {
            Mailbox.clearAll();
            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false" );
            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
            setTestProperty( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "false" );
            setTestProperty( PROPERTIES_SYSADMIN_EMAIL, "sysadmin-1@mockserver.com" );

            // svcSetup an admin user
            String adminUserName = "AdminUserFromOtherOrg";
            String adminUserEmail = "AdminUserFromOtherOrg@otherorg.com";

            UserInfo adminUser = setup.getMgmtSvc().createAdminUser(
                    adminUserEmail, adminUserEmail, adminUserEmail, "password1", true, false );

            refreshIndex("test-organization", "test-app");

            assertNotNull( adminUser );
            Message[] msgs = getMessages( "otherorg.com", adminUserName, "password1" );
            assertEquals( 1, msgs.length );

            // add existing admin user to org

            // this should NOT send resetpwd link in email to newly added org admin user(that
            // already exists in usergrid) only "User Invited To Organization" email
            String adminToken = adminToken();
            JsonNode node = postAddAdminToOrg( "test-organization",
                    adminUserEmail, "password1", adminToken );
            String uuid = node.get( "data" ).get( "user" ).get( "uuid" ).textValue();
            UUID userId = UUID.fromString( uuid );

            assertEquals( adminUser.getUuid(), userId );

            msgs = getMessages( "otherorg.com", adminUserName, "password1" );

            // only 1 invited msg
            assertEquals( 2, msgs.length );

            // check email subject
            String resetpwd = "Password Reset";
            assertNotSame( resetpwd, msgs[1].getSubject() );

            String invited = "User Invited To Organization";
            assertEquals( invited, msgs[1].getSubject() );
        }
        finally {
            setTestProperties( originalProperties );
        }
    }


    private Message[] getMessages( String host, String user, String password ) throws MessagingException, IOException {

        Session session = Session.getDefaultInstance( new Properties() );
        Store store = session.getStore( "imap" );
        store.connect( host, user, password );

        Folder folder = store.getFolder( "inbox" );
        folder.open( Folder.READ_ONLY );
        Message[] msgs = folder.getMessages();

        for ( Message m : msgs ) {
            logger.info( "Subject: " + m.getSubject() );
            logger.info(
                    "Body content 0 " + ( ( MimeMultipart ) m.getContent() ).getBodyPart( 0 ).getContent());
            logger.info(
                    "Body content 1 " + ( ( MimeMultipart ) m.getContent() ).getBodyPart( 1 ).getContent());
        }
        return msgs;
    }
}
