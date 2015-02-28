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


import org.apache.commons.collections4.map.LinkedMap;

import org.apache.usergrid.management.MockImapClient;
import org.apache.usergrid.persistence.index.utils.StringUtils;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.RestClient;
import org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt.*;
import org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt.ManagementResource;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.rest.test.resource2point0.model.Token;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.representation.Form;

import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_NOTIFY_ADMIN_OF_ACTIVATION;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_SYSADMIN_EMAIL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_TEST_ACCOUNT_ADMIN_USER_EMAIL;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_TEST_ACCOUNT_ADMIN_USER_PASSWORD;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.usergrid.rest.test.resource2point0.model.Organization;
import org.apache.usergrid.rest.test.resource2point0.model.User;


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


    /**
     * Test that a unconfirmed admin cannot log in.
     * TODO:test for parallel test that changing the properties here won't affect other tests
     * @throws Exception
     */
    @Test
    public void testUnconfirmedAdminLogin()  throws Exception{

        ApiResponse originalTestPropertiesResponse = clientSetup.getRestClient().testPropertiesResource().get();
        Entity originalTestProperties = new Entity( originalTestPropertiesResponse );
        try {
            //Set runtime enviroment to the following settings
            Map<String, Object> testPropertiesMap = new HashMap<>();

            testPropertiesMap.put( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false" );
            testPropertiesMap.put( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
            //Requires admins to do email confirmation before they can log in.
            testPropertiesMap.put( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "true" );
            testPropertiesMap.put( PROPERTIES_SYSADMIN_EMAIL, "sysadmin-1@mockserver.com" );

            Entity testPropertiesPayload = new Entity( testPropertiesMap );

            //Send rest call to the /testProperties endpoint to persist property changes
            clientSetup.getRestClient().testPropertiesResource().post( testPropertiesPayload );

            refreshIndex();

            //Retrieve properties and ensure that they are set correctly.
            ApiResponse apiResponse = clientSetup.getRestClient().testPropertiesResource().get();

            assertEquals( "sysadmin-1@mockserver.com", apiResponse.getProperties().get( PROPERTIES_SYSADMIN_EMAIL ) );
            assertEquals( "true", apiResponse.getProperties().get( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION ) );
            assertEquals( "false", apiResponse.getProperties().get( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS ) );
            assertEquals( "false", apiResponse.getProperties().get( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS ) );

            //Create organization for the admin user to be confirmed
            Organization organization = createOrgPayload( "testUnconfirmedAdminLogin", null );

            Organization organizationResponse = clientSetup.getRestClient().management().orgs().post( organization );

            assertNotNull( organizationResponse );

            //Ensure that adminUser has the correct properties set.
            User adminUser = organizationResponse.getOwner();

            assertNotNull( adminUser );
            assertFalse( "adminUser should not be activated yet", adminUser.getActivated() );
            assertFalse( "adminUser should not be confirmed yet", adminUser.getConfirmed() );


            QueryParameters queryParameters = new QueryParameters();
            queryParameters.addParam( "grant_type", "password" ).addParam( "username", adminUser.getUsername() )
                           .addParam( "password", organization.getPassword() );


            //Check that the adminUser cannot log in and fails with a 403
            try {
                management().token().get( queryParameters );
                fail( "Admin user should not be able to log in." );
            }
            catch ( UniformInterfaceException uie ) {
                assertEquals( "Admin user should have failed with 403", 403, uie.getResponse().getStatus() );
            }

            //Create mocked inbox
            List<Message> inbox = Mailbox.get( organization.getEmail() );
            assertFalse( inbox.isEmpty() );

            MockImapClient client = new MockImapClient( "mockserver.com", "test-user-46", "somepassword" );
            client.processMail();

            //Get email with confirmation token and extract token
            Message confirmation = inbox.get( 0 );
            assertEquals( "User Account Confirmation: " + organization.getEmail(), confirmation.getSubject() );
            String token = getTokenFromMessage( confirmation );

            //Make rest call with extracted token to confirm the admin user.
            management().users().user( adminUser.getUuid().toString() ).confirm()
                        .get( new QueryParameters().addParam( "token", token ) );


            //Try the previous call and verify that the admin user can retrieve login token
            Token retToken = management().token().get( queryParameters );

            assertNotNull( retToken );
            assertNotNull( retToken.getAccessToken() );
        }finally {
            clientSetup.getRestClient().testPropertiesResource().post( originalTestProperties );
        }
    }


    /**
     * Test that the system admin doesn't need a confirmation email
     * @throws Exception
     */
    @Test
    public void testSystemAdminNeedsNoConfirmation() throws Exception{
        //Save original properties to return them to normal at the end of the test
        ApiResponse originalTestPropertiesResponse = clientSetup.getRestClient().testPropertiesResource().get();
        Entity originalTestProperties = new Entity( originalTestPropertiesResponse );
        try {
            //Set runtime enviroment to the following settings
            Map<String, Object> testPropertiesMap = new HashMap<>();

            testPropertiesMap.put( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false" );
            testPropertiesMap.put( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
            //Requires admins to do email confirmation before they can log in.
            testPropertiesMap.put( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "true" );

            Entity testPropertiesPayload = new Entity( testPropertiesMap );

            //Send rest call to the /testProperties endpoint to persist property changes
            clientSetup.getRestClient().testPropertiesResource().post( testPropertiesPayload );
            refreshIndex();

            Token superuserToken = management().token().post(
                new Token( clientSetup.getSuperuserName(), clientSetup.getSuperuserPassword() ) );



            assertNotNull( "We should have gotten a valid token back" ,superuserToken );
        }finally{
            clientSetup.getRestClient().testPropertiesResource().post( originalTestProperties );

        }
    }

    /**
     * Test that the test account doesn't need confirmation and is created automatically.
     * @throws Exception
     */
    @Ignore("Test doesn't pass because the test account isn't getting correct instantiated")
    @Test
    public void testTestUserNeedsNoConfirmation() throws Exception{
        //Save original properties to return them to normal at the end of the test
        ApiResponse originalTestPropertiesResponse = clientSetup.getRestClient().testPropertiesResource().get();
        Entity originalTestProperties = new Entity( originalTestPropertiesResponse );
        try {
            //Set runtime enviroment to the following settings
            Map<String, Object> testPropertiesMap = new HashMap<>();

            testPropertiesMap.put( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false" );
            testPropertiesMap.put( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
            //Requires admins to do email confirmation before they can log in.
            testPropertiesMap.put( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "true" );

            Entity testPropertiesPayload = new Entity( testPropertiesMap );

            //Send rest call to the /testProperties endpoint to persist property changes
            clientSetup.getRestClient().testPropertiesResource().post( testPropertiesPayload );
            refreshIndex();

            Token testToken = management().token().post(
                new Token( originalTestProperties.getAsString( PROPERTIES_TEST_ACCOUNT_ADMIN_USER_EMAIL ),
                    originalTestProperties.getAsString(  PROPERTIES_TEST_ACCOUNT_ADMIN_USER_PASSWORD ) ));

            assertNotNull( "We should have gotten a valid token back" ,testToken );
        }finally{
            clientSetup.getRestClient().testPropertiesResource().post( originalTestProperties );

        }
    }


    private String getTokenFromMessage( Message msg ) throws IOException, MessagingException {
        String body = ( ( MimeMultipart ) msg.getContent() ).getBodyPart( 0 ).getContent().toString();
        return StringUtils.substringAfterLast( body, "token=" );
    }


    /**
     * Update the current management user and make sure the change persists
     * @throws Exception
     */
    @Ignore("Fails because we cannot get a single management user without a Admin level token, but"
        + "we can put without any of those permissions. This test will work once that issue has been resolved.")
    @Test
    public void updateManagementUser() throws Exception {

        Organization newOrg = createOrgPayload( "updateManagementUser", null );


        Organization orgReturned = clientSetup.getRestClient().management().orgs().post( newOrg );

        assertNotNull( orgReturned.getOwner() );

        //Add a property to management user
        Entity userProperty = new Entity(  ).chainPut( "company","usergrid" );
        management().users().user( newOrg.getUsername() ).put( userProperty );

        Entity userUpdated = updateAdminUser( userProperty, orgReturned );

        assertEquals( "usergrid",userUpdated.getAsString( "company" ) );

        //Update property with new management value.
        userProperty = new Entity(  ).chainPut( "company","Apigee" );

        userUpdated = updateAdminUser( userProperty, orgReturned);

        assertEquals( "Apigee",userUpdated.getAsString( "company" ) );
    }

    public Entity updateAdminUser(Entity userProperty, Organization organization){
        management().users().user( organization.getUsername() ).put( userProperty );

        return management().users().user( organization.getUsername() ).get();

    }


    /**
     * Check that we send the reactivate email after calling the reactivate endpoint.
     * @throws Exception
     */
    @Test
    public void reactivateTest() throws Exception {
        //call reactivate endpoint on default user
        clientSetup.getRestClient().management().users().user( clientSetup.getUsername() ).reactivate();
        refreshIndex();

        //Create mocked inbox
        List<Message> inbox = Mailbox.get( clientSetup.getEmail());
        assertFalse( inbox.isEmpty() );
    }
    
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

    /**
     * Create an organization payload with almost the same value for everyfield.
     * @param baseName
     * @param properties
     * @return
     */
    public Organization createOrgPayload(String baseName,Map properties){
        String orgName = baseName + org.apache.usergrid.persistence.index.utils.UUIDUtils.newTimeUUID();
        return new Organization( orgName,
            orgName,orgName+"@usergrid",orgName,orgName, properties);
    }

}
