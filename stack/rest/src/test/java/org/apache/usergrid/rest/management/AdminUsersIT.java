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


/**
 * Created by ApigeeCorporation on 9/17/14.
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.management.AccountCreationProps;
import org.apache.usergrid.management.ActivationState;
import org.apache.usergrid.management.MockImapClient;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.management.organizations.OrganizationsResource;
import org.apache.usergrid.rest.test.resource.mgmt.Organization;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.RestClient;
import org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt.*;
import org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt.ManagementResource;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.Token;
import org.apache.usergrid.rest.test.resource2point0.model.User;
import org.apache.usergrid.rest.test.security.TestAdminUser;
import org.apache.usergrid.rest.test.security.TestUser;
import org.apache.usergrid.security.AuthPrincipalInfo;
import org.apache.usergrid.security.AuthPrincipalType;
import org.apache.usergrid.utils.UUIDUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.deploy.util.SessionState;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.representation.Form;

import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_NOTIFY_ADMIN_OF_ACTIVATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_EMAIL;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Contains all tests relating to Admin Users
 */
public class AdminUsersIT extends AbstractRestIT {

    ManagementResource management;

    @Before
    public void setup() {
        management= clientSetup.getRestClient().management();
    }

    /**
     * Test if we can reset an admin's password by using that same admins credentials.
     */
    @Test
    public void setSelfAdminPasswordAsAdmin() throws IOException {

        String username = clientSetup.getUsername();
        String password = clientSetup.getPassword();


        Map<String, Object> passwordPayload = new HashMap<String, Object>();
        passwordPayload.put( "newpassword", "testPassword" );
        passwordPayload.put( "oldpassword", password );

        // change the password as admin. The old password isn't required
        management.users().user( username ).password().post(passwordPayload); //entity( username ).password().post;

        this.refreshIndex();


        //assertNull( getError( node ) );

        //Get the token using the new password
        management.token().post( new Token( username, "testPassword" ) );
        //this.app().token().post(new Token(username, "testPassword"));

        //Check that we cannot get the token using the old password
        try {
            management.token().post( new Token( username, password ) );
            fail( "We shouldn't be able to get a token using the old password" );
        }catch(UniformInterfaceException uie) {
            errorParse( 400,"invalid_grant",uie );
        }
    }


    /**
     * Check that we cannot change the password by using an older password
     */
    @Test
    public void passwordMismatchErrorAdmin() {

        String username = clientSetup.getUsername();
        String password = clientSetup.getPassword();


        Map<String, Object> passwordPayload = new HashMap<String, Object>();
        passwordPayload.put( "newpassword", "testPassword" );
        passwordPayload.put( "oldpassword", password );

        // change the password as admin. The old password isn't required
        management.users().user( username ).password().post( passwordPayload );

        this.refreshIndex();


        //Get the token using the new password
        management.token().post( new Token( username, "testPassword" ) );


        // Check that we can't change the password using the old password.
        try {
            management.users().user( username ).password().post( passwordPayload );
            fail("We shouldn't be able to change the password with the same payload");
        }
        catch ( UniformInterfaceException uie ) {
            errorParse( ClientResponse.Status.BAD_REQUEST.getStatusCode(),"auth_invalid_username_or_password",uie );
        }

    }


    /**
     * Checks that as a superuser (i.e with a superuser token ) we can change the password of a admin.
     * @throws IOException
     */
    @Test
    public void setAdminPasswordAsSysAdmin() throws IOException {

        String username = clientSetup.getUsername();
        String password = clientSetup.getPassword();

        // change the password as admin. The old password isn't required
        Map<String, Object> passwordPayload = new HashMap<String, Object>();
        passwordPayload.put( "newpassword", "testPassword" );



        management.users().user( username ).password().post( clientSetup.getSuperuserToken(), passwordPayload );

        this.refreshIndex();

        assertNotNull( management.token().post( new Token( username, "testPassword" ) ) );

        //Check that we cannot get the token using the old password
        try {
            management.token().post( new Token( username, password ) );
            fail( "We shouldn't be able to get a token using the old password" );
        }catch(UniformInterfaceException uie) {
            errorParse( 400,"invalid_grant",uie );
        }
    }


    /**
     * Get the management user feed and check that it has the correct title.
     * @throws Exception
     */
    @Test
    public void mgmtUserFeed() throws Exception {

        Entity mgmtUserFeedEntity = management.users().user( clientSetup.getUsername() ).feed().get();
        String correctValue= "<a href=mailto:"+clientSetup.getUsername();  //user_org.apache.usergrid.rest.management.AdminUsersIT.mgmtUserFeed4c3e53e0-acc7-11e4-b527-0b8af3c5813f@usergrid.com">user_org.apache.usergrid.rest.management.AdminUsersIT.mgmtUserFeed4c3e53e0-acc7-11e4-b527-0b8af3c5813f (user_org.apache.usergrid.rest.management.AdminUsersIT.mgmtUserFeed4c3e53e0-acc7-11e4-b527-0b8af3c5813f@usergrid.com)</a> created a new organization account named org_org.apache.usergrid.rest.management.AdminUsersIT.mgmtUserFeed4c3ec910-acc7-11e4-94c8-33f0d48a5559

        assertNotNull( mgmtUserFeedEntity );

        ArrayList<Map<String,Object>> feedEntityMap = ( ArrayList ) mgmtUserFeedEntity.get( "entities" );
        assertNotNull( feedEntityMap );
        assertNotNull( feedEntityMap.get( 0 ).get( "title" )  );

    }

    //everything below is MUUserResourceIT

    @Test
    public void testCaseSensitivityAdminUser() throws Exception {

        //Create adminUser values
        Entity adminUserPayload = new Entity();
        String username = "testCaseSensitivityAdminUser"+ org.apache.usergrid.persistence.index.utils
            .UUIDUtils
            .newTimeUUID();
        adminUserPayload.put( "username", username );
        adminUserPayload.put( "name", username );
        adminUserPayload.put( "email", username+"@usergrid.com" );
        adminUserPayload.put( "password", username );

        //create adminUser
        //Entity adminUserResponse = restClient.management().orgs().organization( clientSetup.getOrganizationName() ).users().post( adminUserPayload );
        management.users().post( adminUserPayload );

        refreshIndex();

        Entity adminUserResponse = management.users().user( username.toLowerCase() ).get();
        assertNotNull( adminUserResponse );

//        UserInfo mixcaseUser = setup.getMgmtSvc()
//                                    .createAdminUser( "AKarasulu", "Alex Karasulu", "AKarasulu@Apache.org", "test", true, false );
//
//        refreshIndex(context.getOrgName(), context.getAppName());
//
//        AuthPrincipalInfo adminPrincipal = new AuthPrincipalInfo(
//                AuthPrincipalType.ADMIN_USER, mixcaseUser.getUuid(), UUIDUtils.newTimeUUID() );
//        OrganizationInfo organizationInfo =
//                setup.getMgmtSvc().createOrganization( "MixedCaseOrg", mixcaseUser, true );
//
//        refreshIndex(context.getOrgName(), context.getAppName());
//
//        String tokenStr = mgmtToken( "akarasulu@apache.org", "test" );

        // Should succeed even when we use all lowercase
//        JsonNode node = mapper.readTree( resource().path( "/management/users/akarasulu@apache.org" )
//                                                   .queryParam( "access_token", tokenStr )
//                                                   .accept( MediaType.APPLICATION_JSON )
//                                                   .type( MediaType.APPLICATION_JSON_TYPE )
//                                                   .get( String.class ));

    }
//
//
//    @Test
//    public void testUnconfirmedAdminLogin() throws Exception {
//
//        // Setup properties to require confirmation of users
//        // -------------------------------------------
//
//        Map<String, String> originalProperties = getRemoteTestProperties();
//
//        try {
//            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false" );
//            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
//            setTestProperty( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "true" );
//            setTestProperty( PROPERTIES_SYSADMIN_EMAIL, "sysadmin-1@mockserver.com" );
//            setTestProperty( PROPERTIES_NOTIFY_ADMIN_OF_ACTIVATION, "true" );
//
//            assertTrue( setup.getMgmtSvc().newAdminUsersRequireConfirmation() );
//            assertFalse( setup.getMgmtSvc().newAdminUsersNeedSysAdminApproval() );
//
//            // Setup org/app/user variables and create them
//            // -------------------------------------------
//            String orgName = this.getClass().getName();
//            String appName = "testUnconfirmedAdminLogin";
//            String userName = "TestUser";
//            String email = "test-user-46@mockserver.com";
//            String passwd = "testpassword";
//            OrganizationOwnerInfo orgOwner;
//
//            orgOwner = setup.getMgmtSvc().createOwnerAndOrganization(
//                    orgName, userName, appName, email, passwd, false, false );
//            assertNotNull( orgOwner );
//            String returnedUsername = orgOwner.getOwner().getUsername();
//            assertEquals( userName, returnedUsername );
//
//            UserInfo adminUserInfo = setup.getMgmtSvc().getAdminUserByUsername( userName );
//            assertNotNull( adminUserInfo );
//            assertFalse( "adminUser should not be activated yet", adminUserInfo.isActivated() );
//            assertFalse( "adminUser should not be confirmed yet", adminUserInfo.isConfirmed() );
//
//            // Attempt to authenticate but this should fail
//            // -------------------------------------------
//            JsonNode node;
//            try {
//                node = mapper.readTree( resource().path( "/management/token" )
//                                                  .queryParam( "grant_type", "password" )
//                                                  .queryParam( "username", userName )
//                                                  .queryParam( "password", passwd )
//                                                  .accept( MediaType.APPLICATION_JSON ).get( String.class ));
//
//                fail( "Unconfirmed users should not be authorized to authenticate." );
//            }
//            catch ( UniformInterfaceException e ) {
//                node = mapper.readTree( e.getResponse().getEntity( String.class ));
//                assertEquals( "invalid_grant", node.get( "error" ).textValue() );
//                assertEquals( "User must be confirmed to authenticate",
//                        node.get( "error_description" ).textValue() );
//                LOG.info( "Unconfirmed user was not authorized to authenticate!" );
//            }
//
//            // Confirm the getting account confirmation email for unconfirmed user
//            // -------------------------------------------
//            List<Message> inbox = Mailbox.get( email );
//            assertFalse( inbox.isEmpty() );
//
//            MockImapClient client = new MockImapClient( "mockserver.com", "test-user-46", "somepassword" );
//            client.processMail();
//
//            Message confirmation = inbox.get( 0 );
//            assertEquals( "User Account Confirmation: " + email, confirmation.getSubject() );
//
//            // Extract the token to confirm the user
//            // -------------------------------------------
//            String token = getTokenFromMessage( confirmation );
//            LOG.info( token );
//
//            ActivationState state = setup.getMgmtSvc().handleConfirmationTokenForAdminUser(
//                    orgOwner.getOwner().getUuid(), token );
//            assertEquals( ActivationState.ACTIVATED, state );
//
//            Message activation = inbox.get( 1 );
//            assertEquals( "User Account Activated", activation.getSubject() );
//
//            client = new MockImapClient( "mockserver.com", "test-user-46", "somepassword" );
//            client.processMail();
//
//            refreshIndex(orgName, appName);
//
//            // Attempt to authenticate again but this time should pass
//            // -------------------------------------------
//
//            node = mapper.readTree( resource().path( "/management/token" )
//                                              .queryParam( "grant_type", "password" )
//                                              .queryParam( "username", userName )
//                                              .queryParam( "password", passwd )
//                                              .accept( MediaType.APPLICATION_JSON ).get( String.class ));
//
//            assertNotNull( node );
//            LOG.info( "Authentication succeeded after confirmation: {}.", node.toString() );
//        }
//        finally {
//            setTestProperties( originalProperties );
//        }
//    }
//
//
//    @Test
//    public void testSystemAdminNeedsNoConfirmation() throws Exception {
//
//        Map<String, String> originalProperties = getRemoteTestProperties();
//
//        try {
//            // require comfirmation of new admin users
//            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false" );
//            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
//            setTestProperty( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "true" );
//
//            assertTrue( setup.getMgmtSvc().newAdminUsersRequireConfirmation() );
//            assertFalse( setup.getMgmtSvc().newAdminUsersNeedSysAdminApproval() );
//
//            String sysadminUsername = ( String ) setup.getMgmtSvc().getProperties()
//                                                      .get( AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_EMAIL );
//
//            String sysadminPassword = ( String ) setup.getMgmtSvc().getProperties()
//                                                      .get( AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_PASSWORD );
//
//            // sysadmin login should suceed despite confirmation setting
//            JsonNode node;
//            try {
//                node = mapper.readTree( resource().path( "/management/token" ).queryParam( "grant_type", "password" )
//                                                  .queryParam( "username", sysadminUsername ).queryParam( "password", sysadminPassword )
//                                                  .accept( MediaType.APPLICATION_JSON ).get( String.class ));
//            }
//            catch ( UniformInterfaceException e ) {
//                fail( "Sysadmin should need no confirmation" );
//            }
//        }
//        finally {
//            setTestProperties( originalProperties );
//        }
//    }
//
//
//    @Test
//    public void testTestUserNeedsNoConfirmation() throws Exception {
//
//        Map<String, String> originalProperties = getRemoteTestProperties();
//
//        try {
//            // require comfirmation of new admin users
//            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false" );
//            setTestProperty( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
//            setTestProperty( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "true" );
//
//            assertTrue( setup.getMgmtSvc().newAdminUsersRequireConfirmation() );
//            assertFalse( setup.getMgmtSvc().newAdminUsersNeedSysAdminApproval() );
//
//            String testUserUsername = ( String ) setup.getMgmtSvc().getProperties()
//                                                      .get( AccountCreationProps
//                                                              .PROPERTIES_TEST_ACCOUNT_ADMIN_USER_EMAIL );
//
//            String testUserPassword = ( String ) setup.getMgmtSvc().getProperties()
//                                                      .get( AccountCreationProps
//                                                              .PROPERTIES_TEST_ACCOUNT_ADMIN_USER_PASSWORD );
//
//            // test user login should suceed despite confirmation setting
//            JsonNode node;
//            try {
//                node = mapper.readTree( resource().path( "/management/token" ).queryParam( "grant_type", "password" )
//                                                  .queryParam( "username", testUserUsername ).queryParam( "password", testUserPassword )
//                                                  .accept( MediaType.APPLICATION_JSON ).get( String.class ));
//            }
//            catch ( UniformInterfaceException e ) {
//                fail( "Test User should need no confirmation" );
//            }
//        }
//        finally {
//            setTestProperties( originalProperties );
//        }
//    }
//
//
//    private String getTokenFromMessage( Message msg ) throws IOException, MessagingException {
//        String body = ( ( MimeMultipart ) msg.getContent() ).getBodyPart( 0 ).getContent().toString();
//        return StringUtils.substringAfterLast( body, "token=" );
//    }
//
//
//    @Test
//    public void updateManagementUser() throws Exception {
//        Map<String, String> payload =
//                hashMap( "email", "uort-user-1@apigee.com" ).map( "username", "uort-user-1" ).map( "name", "Test User" )
//                                                            .map( "password", "password" ).map( "organization", "uort-org" ).map( "company", "Apigee" );
//
//        JsonNode node = mapper.readTree( resource().path( "/management/organizations" ).accept( MediaType.APPLICATION_JSON )
//                                                   .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));
//        logNode( node );
//        String userId = node.get( "data" ).get( "owner" ).get( "uuid" ).asText();
//
//        assertEquals( "Apigee", node.get( "data" ).get( "owner" ).get( "properties" ).get( "company" ).asText() );
//
//        String token = mgmtToken( "uort-user-1@apigee.com", "password" );
//
//        node = mapper.readTree( resource().path( String.format( "/management/users/%s", userId ) ).queryParam( "access_token", token )
//                                          .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
//
//        logNode( node );
//
//        payload = hashMap( "company", "Usergrid" );
//        LOG.info( "sending PUT for company update" );
//        node = mapper.readTree( resource().path( String.format( "/management/users/%s", userId ) ).queryParam( "access_token", token )
//                                          .type( MediaType.APPLICATION_JSON_TYPE ).put( String.class, payload ));
//        assertNotNull( node );
//        node = mapper.readTree( resource().path( String.format( "/management/users/%s", userId ) ).queryParam( "access_token", token )
//                                          .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
//        assertEquals( "Usergrid", node.get( "data" ).get( "properties" ).get( "company" ).asText() );
//
//
//        logNode( node );
//    }
//
//
//    @Test
//    public void getUser() throws Exception {
//
//        // set an organization property
//        HashMap<String, Object> payload = new HashMap<String, Object>();
//        Map<String, Object> properties = new HashMap<String, Object>();
//        properties.put( "securityLevel", 5 );
//        payload.put( OrganizationsResource.ORGANIZATION_PROPERTIES, properties );
//
//
//        /**
//         * Get the original org admin before we overwrite the property as a super user
//         */
//        final TestUser orgAdmin = context.getActiveUser();
//        final String orgName = context.getOrgName();
//        final String superAdminToken = superAdminToken();
//
//        TestAdminUser superAdmin = new TestAdminUser( "super", "super", "superuser@usergrid.com" );
//        superAdmin.setToken( superAdminToken );
//
//        Organization org = context.withUser( superAdmin ).management().orgs().organization( orgName );
//
//        org.put( payload );
//
//
//        //now get the org
//        JsonNode node = context.withUser( orgAdmin ).management().users().user( orgAdmin.getUser() ).get();
//
//        logNode( node );
//
//        JsonNode applications = node.findValue( "applications" );
//        assertNotNull( applications );
//        JsonNode users = node.findValue( "users" );
//        assertNotNull( users );
//
//        JsonNode securityLevel = node.findValue( "securityLevel" );
//        assertNotNull( securityLevel );
//        assertEquals( 5L, securityLevel.asLong() );
//    }
//
//
//    @Test
//    public void getUserShallow() throws Exception {
//
//
//        // set an organization property
//        HashMap<String, Object> payload = new HashMap<String, Object>();
//        Map<String, Object> properties = new HashMap<String, Object>();
//        properties.put( "securityLevel", 5 );
//        payload.put( OrganizationsResource.ORGANIZATION_PROPERTIES, properties );
//
//
//        /**
//         * Get the original org admin before we overwrite the property as a super user
//         */
//        final TestUser orgAdmin = context.getActiveUser();
//        final String orgName = context.getOrgName();
//        final String superAdminToken  = superAdminToken();
//
//        TestAdminUser superAdmin = new TestAdminUser( "super", "super", "superuser@usergrid.com" );
//        superAdmin.setToken( superAdminToken );
//
//        Organization org = context.withUser( superAdmin ).management().orgs().organization( orgName );
//
//        org.put( payload );
//
//
//        //now get the org
//        JsonNode node = context.withUser( orgAdmin ).management().users().user( orgAdmin.getUser() ).withParam(
//                "shallow", "true" ).get();
//
//        logNode( node );
//
//        JsonNode applications = node.findValue( "applications" );
//        assertNull( applications );
//        JsonNode users = node.findValue( "users" );
//        assertNull( users );
//
//        JsonNode securityLevel = node.findValue( "securityLevel" );
//        assertNotNull( securityLevel );
//        assertEquals( 5L, securityLevel.asLong() );
//    }
//
//
//    @Test
//    public void reactivateMultipleSend() throws Exception {
//
//        JsonNode node = mapper.readTree( resource().path( "/management/organizations" ).accept( MediaType.APPLICATION_JSON )
//                                                   .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, buildOrgUserPayload( "reactivate" ) ));
//
//        logNode( node );
//        String email = node.get( "data" ).get( "owner" ).get( "email" ).asText();
//        String uuid = node.get( "data" ).get( "owner" ).get( "uuid" ).asText();
//        assertNotNull( email );
//        assertEquals( "MUUserResourceIT-reactivate@apigee.com", email );
//
//        refreshIndex(context.getOrgName(), context.getAppName());
//
//        // reactivate should send activation email
//
//        node = mapper.readTree( resource().path( String.format( "/management/users/%s/reactivate", uuid ) )
//                                          .queryParam( "access_token", adminAccessToken ).accept( MediaType.APPLICATION_JSON )
//                                          .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
//
//        refreshIndex(context.getOrgName(), context.getAppName());
//
//        List<Message> inbox = org.jvnet.mock_javamail.Mailbox.get( email );
//
//        assertFalse( inbox.isEmpty() );
//        logNode( node );
//    }
//
//
//    private Map<String, String> buildOrgUserPayload( String caller ) {
//        String className = this.getClass().getSimpleName();
//        Map<String, String> payload = hashMap( "email", String.format( "%s-%s@apigee.com", className, caller ) )
//                .map( "username", String.format( "%s-%s-user", className, caller ) )
//                .map( "name", String.format( "%s %s", className, caller ) ).map( "password", "password" )
//                .map( "organization", String.format( "%s-%s-org", className, caller ) );
//        return payload;
//    }
//
//
//    @Test
//    public void checkPasswordReset() throws Exception {
//
//        refreshIndex(context.getOrgName(), context.getAppName());
//
//        TestUser user = context.getActiveUser();
//
//        String email = user.getEmail();
//        UserInfo userInfo = setup.getMgmtSvc().getAdminUserByEmail( email );
//        String resetToken = setup.getMgmtSvc().getPasswordResetTokenForAdminUser( userInfo.getUuid(), 15000 );
//
//        assertTrue( setup.getMgmtSvc().checkPasswordResetTokenForAdminUser( userInfo.getUuid(), resetToken ) );
//
//        refreshIndex(context.getOrgName(), context.getAppName());
//
//        Form formData = new Form();
//        formData.add( "token", resetToken );
//        formData.add( "password1", "sesame" );
//        formData.add( "password2", "sesame" );
//
//        String html = resource().path( "/management/users/" + userInfo.getUsername() + "/resetpw" )
//                                .type( MediaType.APPLICATION_FORM_URLENCODED_TYPE ).post( String.class, formData );
//
//        assertTrue( html.contains( "password set" ) );
//
//        refreshIndex(context.getOrgName(), context.getAppName());
//
//        assertFalse( setup.getMgmtSvc().checkPasswordResetTokenForAdminUser( userInfo.getUuid(), resetToken ) );
//
//        html = resource().path( "/management/users/" + userInfo.getUsername() + "/resetpw" )
//                         .type( MediaType.APPLICATION_FORM_URLENCODED_TYPE ).post( String.class, formData );
//
//        assertTrue( html.contains( "invalid token" ) );
//    }
//
//
//    @Test
//    @Ignore( "causes problems in build" )
//    public void passwordResetIncorrectUserName() throws Exception {
//
//        String email = "test2@usergrid.com";
//        setup.getMgmtSvc().createAdminUser( "test2", "test2", "test2@usergrid.com", "sesa2me", false, false );
//        UserInfo userInfo = setup.getMgmtSvc().getAdminUserByEmail( email );
//        String resetToken = setup.getMgmtSvc().getPasswordResetTokenForAdminUser( userInfo.getUuid(), 15000 );
//
//        assertTrue( setup.getMgmtSvc().checkPasswordResetTokenForAdminUser( userInfo.getUuid(), resetToken ) );
//
//        Form formData = new Form();
//        formData.add( "token", resetToken );
//        formData.add( "password1", "sesa2me" );
//        formData.add( "password2", "sesa2me" );
//
//        String html = resource().path( "/management/users/" + "noodle" + userInfo.getUsername() + "/resetpw" )
//                                .type( MediaType.APPLICATION_FORM_URLENCODED_TYPE ).post( String.class, formData );
//
//        assertTrue( html.contains( "Incorrect username entered" ) );
//
//        html = resource().path( "/management/users/" + userInfo.getUsername() + "/resetpw" )
//                         .type( MediaType.APPLICATION_FORM_URLENCODED_TYPE ).post( String.class, formData );
//
//        assertTrue( html.contains( "password set" ) );
//    }
//
//
//    @Test
//    public void checkPasswordHistoryConflict() throws Exception {
//
//        String[] passwords = new String[] { "password1", "password2", "password3", "password4" };
//
//        UserInfo user =
//                setup.getMgmtSvc().createAdminUser( "edanuff", "Ed Anuff", "ed@anuff.com", passwords[0], true, false );
//        assertNotNull( user );
//
//        refreshIndex(context.getOrgName(), context.getAppName());
//
//        OrganizationInfo organization = setup.getMgmtSvc().createOrganization( "ed-organization", user, true );
//        assertNotNull( organization );
//
//        refreshIndex(context.getOrgName(), context.getAppName());
//
//        // set history to 1
//        Map<String, Object> props = new HashMap<String, Object>();
//        props.put( OrganizationInfo.PASSWORD_HISTORY_SIZE_KEY, 1 );
//        organization.setProperties( props );
//        setup.getMgmtSvc().updateOrganization( organization );
//
//        refreshIndex(context.getOrgName(), context.getAppName());
//
//        UserInfo userInfo = setup.getMgmtSvc().getAdminUserByEmail( "ed@anuff.com" );
//
//        Map<String, String> payload = hashMap( "oldpassword", passwords[0] ).map( "newpassword", passwords[0] ); // fail
//
//        try {
//            JsonNode node = mapper.readTree( resource().path( "/management/users/edanuff/password" )
//                                                       .accept( MediaType.APPLICATION_JSON )
//                                                       .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));
//            fail( "should fail with conflict" );
//        }
//        catch ( UniformInterfaceException e ) {
//            assertEquals( 409, e.getResponse().getStatus() );
//        }
//
//        payload.put( "newpassword", passwords[1] ); // ok
//        JsonNode node = mapper.readTree( resource().path( "/management/users/edanuff/password" )
//                                                   .accept( MediaType.APPLICATION_JSON )
//                                                   .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));
//        payload.put( "oldpassword", passwords[1] );
//
//        refreshIndex(context.getOrgName(), context.getAppName());
//
//        payload.put( "newpassword", passwords[0] ); // fail
//        try {
//            node = mapper.readTree( resource().path( "/management/users/edanuff/password" )
//                                              .accept( MediaType.APPLICATION_JSON )
//                                              .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));
//            fail( "should fail with conflict" );
//        }
//        catch ( UniformInterfaceException e ) {
//            assertEquals( 409, e.getResponse().getStatus() );
//        }
//    }
//
//
//    @Test
//    public void checkPasswordChangeTime() throws Exception {
//
//        final TestUser user = context.getActiveUser();
//        String email = user.getEmail();
//        UserInfo userInfo = setup.getMgmtSvc().getAdminUserByEmail( email );
//        String resetToken = setup.getMgmtSvc().getPasswordResetTokenForAdminUser( userInfo.getUuid(), 15000 );
//
//        refreshIndex(context.getOrgName(), context.getAppName());
//
//        Form formData = new Form();
//        formData.add( "token", resetToken );
//        formData.add( "password1", "sesame" );
//        formData.add( "password2", "sesame" );
//
//        String html = resource().path( "/management/users/" + userInfo.getUsername() + "/resetpw" )
//                                .type( MediaType.APPLICATION_FORM_URLENCODED_TYPE ).post( String.class, formData );
//        assertTrue( html.contains( "password set" ) );
//
//        refreshIndex(context.getOrgName(), context.getAppName());
//
//        JsonNode node = mapper.readTree( resource().path( "/management/token" )
//                                                   .queryParam( "grant_type", "password" )
//                                                   .queryParam( "username", email ).queryParam( "password", "sesame" )
//                                                   .accept( MediaType.APPLICATION_JSON )
//                                                   .get( String.class ));
//
//        Long changeTime = node.get( "passwordChanged" ).longValue();
//        assertTrue( System.currentTimeMillis() - changeTime < 2000 );
//
//        Map<String, String> payload = hashMap( "oldpassword", "sesame" ).map( "newpassword", "test" );
//        node = mapper.readTree( resource().path( "/management/users/" + userInfo.getUsername() + "/password" )
//                                          .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
//                                          .post( String.class, payload ));
//
//        refreshIndex(context.getOrgName(), context.getAppName());
//
//        node = mapper.readTree( resource().path( "/management/token" )
//                                          .queryParam( "grant_type", "password" )
//                                          .queryParam( "username", email )
//                                          .queryParam( "password", "test" )
//                                          .accept( MediaType.APPLICATION_JSON )
//                                          .get( String.class ));
//
//        Long changeTime2 = node.get( "passwordChanged" ).longValue();
//        assertTrue( changeTime < changeTime2 );
//        assertTrue( System.currentTimeMillis() - changeTime2 < 2000 );
//
//        node = mapper.readTree( resource().path( "/management/me" ).queryParam( "grant_type", "password" )
//                                          .queryParam( "username", email ).queryParam( "password", "test" ).accept( MediaType.APPLICATION_JSON )
//                                          .get( String.class ));
//
//        Long changeTime3 = node.get( "passwordChanged" ).longValue();
//        assertEquals( changeTime2, changeTime3 );
//    }
//
//
//    /** USERGRID-1960 */
//    @Test
//    @Ignore( "Depends on other tests" )
//    public void listOrgUsersByName() {
//        JsonNode response = context.management().orgs().organization( context.getOrgName() ).users().get();
//
//        //get the response and verify our user is there
//        JsonNode adminNode = response.get( "data" ).get( 0 );
//        assertEquals( context.getActiveUser().getEmail(), adminNode.get( "email" ).asText() );
//        assertEquals( context.getActiveUser().getUser(), adminNode.get( "username" ).asText() );
//    }
//
//    @Test
//    public void createOrgFromUserConnectionFail() throws Exception {
//
//
//        Map<String, String> payload = hashMap( "email", "orgfromuserconn@apigee.com" ).map( "password", "password" )
//                                                                                      .map( "organization", "orgfromuserconn" );
//
//        JsonNode node = mapper.readTree( resource().path( "/management/organizations" ).accept( MediaType.APPLICATION_JSON )
//                                                   .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));
//
//        String userId = node.get( "data" ).get( "owner" ).get( "uuid" ).asText();
//
//        assertNotNull( node );
//
//        String token = mgmtToken( "orgfromuserconn@apigee.com", "password" );
//
//        node = mapper.readTree( resource().path( String.format( "/management/users/%s/", userId ) ).queryParam( "access_token", token )
//                                          .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
//
//        logNode( node );
//
//        payload = hashMap( "organization", "Orgfromuserconn" );
//
//        // try to create the same org again off the connection
//        try {
//            node = mapper.readTree( resource().path( String.format( "/management/users/%s/organizations", userId ) )
//                                              .queryParam( "access_token", token ).accept( MediaType.APPLICATION_JSON )
//                                              .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));
//            fail( "Should have thrown unique exception on org name" );
//        }
//        catch ( Exception ex ) {
//        }
//    }

}
