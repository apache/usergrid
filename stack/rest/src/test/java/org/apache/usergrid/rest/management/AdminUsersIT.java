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
import net.jcip.annotations.NotThreadSafe;
import org.apache.usergrid.management.MockImapClient;
import org.apache.usergrid.persistence.core.util.StringUtils;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.endpoints.mgmt.ManagementResource;
import org.apache.usergrid.rest.test.resource.model.*;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

import static org.apache.usergrid.management.AccountCreationProps.*;
import static org.junit.Assert.*;


/**
 * Contains all tests relating to Admin Users
 */
@NotThreadSafe
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
        management.users().user( username ).password().post(Entity.class,passwordPayload);

        this.refreshIndex();

        //Get the token using the new password
        Token adminToken = management.token().post( false, Token.class, new Token( username, "testPassword" ) ,null );
        management.token().setToken( adminToken );

        //Check that we cannot get the token using the old password
        try {
            management.token().post(false, Token.class, new Token( username, password ),null);
            fail( "We shouldn't be able to get a token using the old password" );
        }catch(ClientErrorException uie) {
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
        management.users().user( username ).password().post(Entity.class, passwordPayload );

        this.refreshIndex();


        //Get the token using the new password
        Token adminToken = management.token().post( Token.class, new Token( username, "testPassword" )  );
        management.token().setToken( adminToken );



        // Check that we can't change the password using the old password.
        try {
            management.users().user( username ).password().post( Entity.class ,passwordPayload );
            fail("We shouldn't be able to change the password with the same payload");
        }
        catch ( ClientErrorException uie ) {
            errorParse( Response.Status.BAD_REQUEST.getStatusCode(),"auth_invalid_username_or_password",uie );
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

        management.token().setToken( clientSetup.getSuperuserToken());
        management.users().user( username ).password().post( passwordPayload );

        this.refreshIndex();

        assertNotNull( management.token().post( false, Token.class, new Token(username, "testPassword"), null ));


        // Check that we cannot get the token using the old password
        try {
            management.token().post( false,Token.class, new Token( username, password) ,null);
            fail( "We shouldn't be able to get a token using the old password" );
        } catch(ClientErrorException uie) {
            errorParse( 400,"invalid_grant",uie );
        }
    }


    /**
     * Get the management user feed and check that it has the correct title.
     * @throws Exception
     */
    @Test
    public void mgmtUserFeed() throws Exception {
        //TODO: fix or establish what the user feed should do
        management.token().setToken( this.getAdminToken() );
        ApiResponse mgmtUserFeedEntity = management.users().user( clientSetup.getUsername() ).feed().get();
        String correctValue= "<a href=mailto:"+clientSetup.getUsername();  //user_org.apache.usergrid.rest.management.AdminUsersIT.mgmtUserFeed4c3e53e0-acc7-11e4-b527-0b8af3c5813f@usergrid.com">user_org.apache.usergrid.rest.management.AdminUsersIT.mgmtUserFeed4c3e53e0-acc7-11e4-b527-0b8af3c5813f (user_org.apache.usergrid.rest.management.AdminUsersIT.mgmtUserFeed4c3e53e0-acc7-11e4-b527-0b8af3c5813f@usergrid.com)</a> created a new organization account named org_org.apache.usergrid.rest.management.AdminUsersIT.mgmtUserFeed4c3ec910-acc7-11e4-94c8-33f0d48a5559

        assertNotNull( mgmtUserFeedEntity );

        List<Entity> feedEntityMap =  mgmtUserFeedEntity.getEntities();
        assertNotNull( feedEntityMap );
        assertNotEquals( 0,feedEntityMap.size() );
        assertNotNull( feedEntityMap.get( 0 ).get( "title" )  );
        assertTrue("Needs to contain the feed of the specific management user",
            ((String)(feedEntityMap.get( 0 ).get( "title" ))).contains(clientSetup.getUsername() ));
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
            //TODO: make properties verification its own test.
            Map<String, Object> testPropertiesMap = new HashMap<>();

            testPropertiesMap.put( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false" );
            testPropertiesMap.put( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
            //Requires admins to do email confirmation before they can log in.
            testPropertiesMap.put( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "true" );
            testPropertiesMap.put( PROPERTIES_DEFAULT_SYSADMIN_EMAIL, "sysadmin-1@mockserver.com" );

            Entity testPropertiesPayload = new Entity( testPropertiesMap );

            //Send rest call to the /testProperties endpoint to persist property changes
            clientSetup.getRestClient().testPropertiesResource().post( testPropertiesPayload );

            refreshIndex();

            //Create organization for the admin user to be confirmed
            Organization organization = createOrgPayload( "testUnconfirmedAdminLogin", null );

            Organization organizationResponse = clientSetup.getRestClient().management().orgs().post( organization );

            assertNotNull( organizationResponse );

            //Ensure that adminUser has the correct properties set.
            User adminUser = organizationResponse.getOwner();

            assertNotNull( adminUser );
            assertFalse( "adminUser should not be activated yet", adminUser.getActivated() );
            assertFalse( "adminUser should not be confirmed yet", adminUser.getConfirmed() );


            //Get token grant for new admin user.
            QueryParameters queryParameters = new QueryParameters();
            queryParameters.addParam( "grant_type", "password" ).addParam( "username", adminUser.getUsername() )
                           .addParam( "password", organization.getPassword() );


            //Check that the adminUser cannot log in and fails with a 403 due to not being confirmed.
            try {
                management().token().get( queryParameters );
                fail( "Admin user should not be able to log in." );
            }
            catch ( ClientErrorException uie ) {
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

            Token superuserToken = management.token().post( Token.class,
                new Token( clientSetup.getSuperuserName(), clientSetup.getSuperuserPassword() )  );




            assertNotNull( "We should have gotten a valid token back" ,superuserToken );
        }finally{
            clientSetup.getRestClient().testPropertiesResource().post( originalTestProperties );

        }
    }

    /**
     * Test that the test account doesn't need confirmation and is created automatically.
     * @throws Exception
     */
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

            Token testToken = management().token().post(Token.class,
                new Token( originalTestProperties.getAsString( PROPERTIES_TEST_ACCOUNT_ADMIN_USER_EMAIL ),
                    originalTestProperties.getAsString(  PROPERTIES_TEST_ACCOUNT_ADMIN_USER_PASSWORD ) ));

            assertNotNull( "We should have gotten a valid token back" ,testToken );
        }finally{
            clientSetup.getRestClient().testPropertiesResource().post( originalTestProperties );

        }
    }

    @Test
    public void updateManagementUserNoToken() throws Exception {


        Organization newOrg = createOrgPayload( "updateManagementUserNoToken", null );


        Organization orgReturned = clientSetup.getRestClient().management().orgs().post( newOrg );

        assertNotNull( orgReturned.getOwner() );

        //Add a property to management user
        Entity userProperty = new Entity(  ).chainPut( "company","usergrid" );

        try{
            management().users().user( newOrg.getUsername() ).put( userProperty );
        } catch( UniformInterfaceException e ){

            int status = e.getResponse().getStatus();
            assertEquals(401, status);
        }

    }

    @Test
    public void updateManagementUserSuperuserToken() throws Exception {


        Organization newOrg = createOrgPayload( "updateManagementUserSuperuserToken", null );


        Organization orgReturned = clientSetup.getRestClient().management().orgs().post( newOrg );

        assertNotNull( orgReturned.getOwner() );

        //Add a property to management user
        Entity userProperty = new Entity(  ).chainPut( "company","usergrid" );

        management.token().setToken( clientSetup.getSuperuserToken());
        management().users().user( newOrg.getUsername() ).put( userProperty );


    }

    @Test
    public void updateManagementUserAdminToken() throws Exception {

        Organization newOrg = createOrgPayload( "updateManagementUserAdminToken", null );


        Organization orgReturned = clientSetup.getRestClient().management().orgs().post( newOrg );

        assertNotNull( orgReturned.getOwner() );

        String orgName = orgReturned.getName();

        //Add a property to management user
        Entity userProperty = new Entity(  ).chainPut( "company","usergrid" );

        User adminUser = orgReturned.getOwner();

        Token adminToken = management.token().get(adminUser.getUsername(), orgName);
        assertNotNull(adminToken);
        management.token().setToken( adminToken );
        management().users().user( newOrg.getUsername() ).put( userProperty );

    }




    /**
     * Check that we send the reactivate email to the user after calling the reactivate endpoint.
     * @throws Exception
     */
    @Test
    public void reactivateTest() throws Exception {
        //call reactivate endpoint on default user
        clientSetup.getRestClient().management().users().user( clientSetup.getUsername() ).reactivate();
        refreshIndex();

        //Create mocked inbox and check to see if you recieved an email in the users inbox.
        List<Message> inbox = Mailbox.get( clientSetup.getEmail());
        assertFalse( inbox.isEmpty() );
    }


    @Test
    public void checkFormPasswordReset() throws Exception {

        // initiate password reset
        management().users().user( clientSetup.getUsername() ).resetpw().post(new Form());
        refreshIndex();

        // create mocked inbox, get password reset email and extract token
        List<Message> inbox = Mailbox.get( clientSetup.getEmail() );
        assertFalse( inbox.isEmpty() );

        MockImapClient client = new MockImapClient( "mockserver.com", "test-user-46", "somepassword" );
        client.processMail();

        // Get email with confirmation token and extract token

        String token = null;
        Iterator<Message> msgIterator = inbox.iterator();
        while ( token == null && msgIterator.hasNext() ) {
            Message msg = msgIterator.next();
            if ( msg.getSubject().equals("Password Reset") ) {
                token = getTokenFromMessage( msg );
            }
        }
        assertNotNull( token );

        Form formData = new Form();
        formData.param( "token", token );
        formData.param( "password1", "sesame" );
        formData.param( "password2", "sesame" );

        String html = management().users().user( clientSetup.getUsername() ).resetpw().getTarget().request()
            .post( javax.ws.rs.client.Entity.form(formData), String.class );

        assertTrue( html.contains( "password set" ) );

        refreshIndex();


        html = management().users().user( clientSetup.getUsername() ).resetpw().post( formData );

        assertTrue( html.contains( "invalid token" ) );
    }


    @Test
    public void passwordResetIncorrectUserName() throws Exception {

        // initiate password reset
        management().users().user( clientSetup.getUsername() ).resetpw().post(new Form());
        refreshIndex();

        // create mocked inbox, get password reset email and extract token
        List<Message> inbox = Mailbox.get( clientSetup.getEmail() );
        assertFalse( inbox.isEmpty() );

        MockImapClient client = new MockImapClient( "mockserver.com", "test-user-47", "somepassword" );
        client.processMail();

        // Get email with confirmation token and extract token
        String token = null;
        Iterator<Message> msgIterator = inbox.iterator();
        while ( token == null && msgIterator.hasNext() ) {
            Message msg = msgIterator.next();
            if ( msg.getSubject().equals("Password Reset") ) {
                token = getTokenFromMessage( msg );
            }
        }
        assertNotNull( token );

        Form formData = new Form();
        formData.param( "token", token );
        formData.param( "password1", "sesame" );
        formData.param( "password2", "sesame" );

        try {

            String html = management().users().user( "mr_fatfinger" ).resetpw().getTarget().request()
                .post( javax.ws.rs.client.Entity.form( formData ), String.class );

            fail("Password reset request should have failed");

        } catch ( BadRequestException expected ) {
            String body = expected.getResponse().readEntity( String.class );
            assertTrue( body.contains( "Could not find" ) );
        }

        String html = management().users().user( clientSetup.getUsername() ).resetpw().getTarget().request()
            .post( javax.ws.rs.client.Entity.form(formData), String.class );

        assertTrue( html.contains( "password set" ) );
    }


    /**
     * Checks that the passwords are stored in the history and that older ones are overwritten.
     * @throws Exception
     */
    @Test
    public void checkPasswordHistoryConflict() throws Exception {

        String[] passwords = new String[] { clientSetup.getPassword(), "password2" };

        //set the number of old passwords stored to 1
        Map<String, Object> props = new HashMap<String, Object>();
        props.put( "passwordHistorySize", 1 );
        Organization orgPropertiesPayload = new Organization(  );

        orgPropertiesPayload.put("properties", props);

        management().orgs().org( clientSetup.getOrganizationName() ).put( orgPropertiesPayload );

        //Creates a payload with the same password to verify we cannot change the password to itself.
         Map<String, Object> payload = new HashMap<>(  );
         payload.put("oldpassword",passwords[0]);
         payload.put("newpassword",passwords[0]); //hashMap( "oldpassword", passwords[0] ).map( "newpassword", passwords[0] ); // fail

        //Makes sure we can't replace a password with itself ( as it is the only one in the history )
        try {
            management().users().user( clientSetup.getUsername() ).password().post(Entity.class ,payload );

            fail( "should fail with conflict" );
        }
        catch ( ClientErrorException e ) {
            assertEquals( 409, e.getResponse().getStatus() );
        }

        //Change the password
        payload.put( "newpassword", passwords[1] );
        management().users().user( clientSetup.getUsername() ).password().post( Entity.class,payload );

        refreshIndex();

        payload.put( "newpassword", passwords[0] );
        payload.put( "oldpassword", passwords[1] );

        //Make sure that we can't change the password with itself using a different entry in the history.
        try {
            management().users().user( clientSetup.getUsername() ).password().post( Entity.class,payload );

            fail( "should fail with conflict" );
        }
        catch ( ClientErrorException e ) {
            assertEquals( 409, e.getResponse().getStatus() );
        }
    }

    @Test
    public void checkPasswordChangeTime() throws Exception {

        // request password reset

        management().users().user( clientSetup.getUsername() ).resetpw().post(new Form());
        refreshIndex();

        // get resetpw token from email

        List<Message> inbox = Mailbox.get( clientSetup.getEmail() );
        assertFalse( inbox.isEmpty() );
        MockImapClient client = new MockImapClient( "mockserver.com", "test-user-46", "somepassword" );
        client.processMail();
        String token = null;
        Iterator<Message> msgIterator = inbox.iterator();
        while ( token == null && msgIterator.hasNext() ) {
            Message msg = msgIterator.next();
            if ( msg.getSubject().equals("Password Reset") ) {
                token = getTokenFromMessage( msg );
            }
        }
        assertNotNull( token );

        // reset the password to sesame

        Form formData = new Form();
        formData.param( "token", token );
        formData.param( "password1", "sesame" );
        formData.param( "password2", "sesame" );
        String html = management().users().user( clientSetup.getUsername() ).resetpw().getTarget().request()
            .post( javax.ws.rs.client.Entity.form(formData), String.class );
        assertTrue( html.contains( "password set" ) );
        refreshIndex();

        // login with new password and get token

        Map<String, Object> payload = new HashMap<String, Object>() {{
            put("grant_type", "password");
            put("username", clientSetup.getUsername() );
            put("password", "sesame");
        }};
        ApiResponse response = management().token().post( false, payload, null );

        // check password changed time

        Long changeTime = Long.parseLong( response.getProperties().get( "passwordChanged" ).toString() );
        assertTrue( System.currentTimeMillis() - changeTime < 2000 );

        // change password again by posting JSON

        payload = new HashMap<String, Object>() {{
            put("oldpassword", "sesame");
            put("newpassword", "test");
        }};
        management().users().user( clientSetup.getUsername() ).password().post( false, payload, null );
        refreshIndex();

        // get password and check password change time again

        payload = new HashMap<String, Object>() {{
            put("grant_type", "password");
            put("username", clientSetup.getUsername() );
            put("password", "test");
        }};
        response = management().token().post( false, payload, null );

        Long changeTime2 = Long.parseLong( response.getProperties().get( "passwordChanged" ).toString() );
        assertTrue( changeTime < changeTime2 );
        assertTrue( System.currentTimeMillis() - changeTime2 < 2000 );

        // login via /me end-point and check password change time again

        response = management().me().get( ApiResponse.class, new QueryParameters()
            .addParam( "grant_type", "password" )
            .addParam( "username", clientSetup.getUsername() )
            .addParam( "password", "test" ) );

        Long changeTime3 = Long.parseLong( response.getProperties().get( "passwordChanged" ).toString() );
        assertEquals( changeTime2, changeTime3 );
    }




    /**
     * Make sure we can list the org admin users by name.
      */
    @Test
    public void listOrgUsersByName() {

        Entity adminUserPayload = new Entity();
        String username = "listOrgUsersByName"+UUIDUtils.newTimeUUID();
        Credentials orgCredentials = clientSetup.getClientCredentials();
        adminUserPayload.put( "username", username );
        adminUserPayload.put( "name", username );
        adminUserPayload.put( "email", username+"@usergrid.com" );
        adminUserPayload.put( "password", username );

//        //If we comment this out it works, shouldn't using an organization Token for an endpoint
        //with organization access work?
        //TODO:investigate above comment
//        Token organizationToken =
//            management().token().post( Token.class,
//                new Token( "client_credentials", orgCredentials.getClientId(), orgCredentials.getClientSecret() ) );
//        management().token().setToken( organizationToken );

        //Create admin user
        management().orgs().org( clientSetup.getOrganizationName() ).users().post(ApiResponse.class ,adminUserPayload );

        refreshIndex();

        //Retrieves the admin users
        ApiResponse adminUsers = management().orgs().org( clientSetup.getOrganizationName() ).users().get(ApiResponse.class);

        assertEquals("There need to be 2 admin users",2,( ( ArrayList ) adminUsers.getData() ).size());

    }


    /**
     * Makes sure you can't create a already existing organization from a user connection.
     * @throws Exception
     */
    // TODO: figure out what is the expected behavior from this test.
    // While it fails it is not sure what it should return
    @Test
    public void createOrgFromUserConnectionFail() throws Exception {

        Token token = management().token()
            .post( Token.class, new Token( clientSetup.getUsername(), clientSetup.getPassword() ) );
        management().token().setToken( token );

        // try to create the same org again off the connection
        try {
            management().users().user( clientSetup.getUsername() ).organizations()
                .post( clientSetup.getOrganization(), token );

            fail( "Should have thrown unique exception on org name" );
        }
        catch ( InternalServerErrorException ise ) {
            assertEquals(500, ise.getResponse().getStatus());
        }
    }

    @Test
    public void testProperties(){
        ApiResponse originalTestPropertiesResponse = clientSetup.getRestClient().testPropertiesResource().get();
        Entity originalTestProperties = new Entity( originalTestPropertiesResponse );
        try {
            //Set runtime enviroment to the following settings
            Map<String, Object> testPropertiesMap = new HashMap<>();

            testPropertiesMap.put( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS, "false" );
            testPropertiesMap.put( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS, "false" );
            //Requires admins to do email confirmation before they can log in.
            testPropertiesMap.put( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "true" );
            testPropertiesMap.put( PROPERTIES_DEFAULT_SYSADMIN_EMAIL, "sysadmin-1@mockserver.com" );

            Entity testPropertiesPayload = new Entity( testPropertiesMap );

            //Send rest call to the /testProperties endpoint to persist property changes
            clientSetup.getRestClient().testPropertiesResource().post( testPropertiesPayload );

            refreshIndex();

            //Retrieve properties and ensure that they are set correctly.
            ApiResponse apiResponse = clientSetup.getRestClient().testPropertiesResource().get();

            assertEquals( "sysadmin-1@mockserver.com", apiResponse.getProperties().get( PROPERTIES_DEFAULT_SYSADMIN_EMAIL ) );
            assertEquals( "true", apiResponse.getProperties().get( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION ) );
            assertEquals( "false", apiResponse.getProperties().get( PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS ) );
            assertEquals( "false", apiResponse.getProperties().get( PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS ) );
        }finally{
            clientSetup.getRestClient().testPropertiesResource().post( originalTestProperties);
        }
    }

    /**
     * Create an organization payload with almost the same value for every field.
     * @param baseName
     * @param properties
     * @return
     */
    public Organization createOrgPayload(String baseName,Map properties){
        String orgName = baseName + org.apache.usergrid.persistence.index.utils.UUIDUtils.newTimeUUID();
        return new Organization( orgName,
            orgName,orgName+"@usergrid",orgName,orgName, properties);
    }


    /**
     * Extract token from mocked inbox message.
     * @param msg
     * @return
     * @throws IOException
     * @throws MessagingException
     */
    private String getTokenFromMessage( Message msg ) throws IOException, MessagingException {
        String body = ( ( MimeMultipart ) msg.getContent() ).getBodyPart( 0 ).getContent().toString();
        return StringUtils.substringAfterLast( body, "token=" );
    }

}
