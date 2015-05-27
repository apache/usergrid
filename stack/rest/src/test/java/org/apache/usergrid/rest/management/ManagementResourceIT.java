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


import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.representation.Form;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.management.organizations.OrganizationsResource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;

import static org.apache.usergrid.rest.management.ManagementResource.USERGRID_CENTRAL_URL;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.*;


/**
 * @author tnine
 */

public class ManagementResourceIT extends AbstractRestIT {

    private static final Logger logger = LoggerFactory.getLogger(ManagementResourceIT.class);


    public ManagementResourceIT() throws Exception {

    }


    /**
     * Test if we can reset our password as an admin
     */
    @Test
    public void setSelfAdminPasswordAsAdmin() {

        String newPassword = "foo";

        Map<String, String> data = new HashMap<String, String>();
        data.put( "newpassword", newPassword );
        data.put( "oldpassword", "test" );

        // change the password as admin. The old password isn't required
        JsonNode node = resource().path( "/management/users/test/password" ).accept( MediaType.APPLICATION_JSON )
                                  .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, data );

        assertNull( getError( node ) );

        adminAccessToken = mgmtToken( "test", newPassword );

        data.put( "oldpassword", newPassword );
        data.put( "newpassword", "test" );

        node = resource().path( "/management/users/test/password" ).queryParam( "access_token", adminAccessToken )
                         .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                         .post( JsonNode.class, data );

        assertNull( getError( node ) );
    }



    /**
     * Test that admins can't view organizations they're not authorized to view.
     */
    @Test
    public void crossOrgsNotViewable() throws Exception {

        String username = "test" + UUIDUtils.newTimeUUID();
        String name = username;
        String email = username + "@usergrid.com";
        String password = "password";
        String orgName = username;

        Map payload =
                hashMap( "email", email ).map( "username", username ).map( "name", name ).map( "password", password )
                                         .map( "organization", orgName ).map( "company", "Apigee" );

        JsonNode node = mapper.readTree(
                resource().path( "/management/organizations" ).accept( MediaType.APPLICATION_JSON )
                          .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ) );

        // check that the test admin cannot access the new org info

        Status status = null;

        try {
            this.
            resource().path( String.format( "/management/orgs/%s", orgName ) )
                      .queryParam( "access_token", this.adminToken() )
                      .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( String.class );
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertNotNull( status );
        assertEquals( Status.UNAUTHORIZED, status );

        // this admin should have access to test org
        status = null;
        try {
            resource().path( "/management/orgs/" + this.orgInfo.getName())
                      .queryParam( "access_token", this.adminToken() )
                      .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( String.class );
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertNull( status );

        //test getting the organization by org

        status = null;
        try {
            resource().path( String.format( "/management/orgs/%s", this.orgInfo.getUuid() ) )
                      .queryParam( "access_token", this.adminToken() )
                      .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( String.class );
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertNull( status );
    }


    /**
     * Test that we can support over 10 items in feed.
     */
    @Test
    public void mgmtFollowsUserFeed() throws Exception {
        List<String> users1 = new ArrayList<String>();
        int i;
        //try with 10 users
        for ( i = 0; i < 10; i++ ) {
            users1.add( "follower" + Integer.toString( i ) );
        }

        refreshIndex( this.orgInfo.getName(), this.appInfo.getName() );

        checkFeed( "leader1", users1 );
        //try with 11
        List<String> users2 = new ArrayList<String>();
        for ( i = 20; i < 31; i++ ) {
            users2.add( "follower" + Integer.toString( i ) );
        }
        checkFeed( "leader2", users2 );
    }


    private void checkFeed( String leader, List<String> followers ) throws IOException {
        JsonNode userFeed;

        //create user
        createUser( leader );
        refreshIndex( this.orgInfo.getName(), this.appInfo.getName() );

        String preFollowContent = leader + ": pre-something to look for " + UUID.randomUUID().toString();

        addActivity( leader, leader + " " + leader + "son", preFollowContent );
        refreshIndex( this.orgInfo.getName(), this.appInfo.getName() );

        String lastUser = followers.get( followers.size() - 1 );
        int i = 0;
        for ( String user : followers ) {
            createUser( user );
            refreshIndex( this.orgInfo.getName(), this.appInfo.getName() );
            follow( user, leader );
            refreshIndex( this.orgInfo.getName(), this.appInfo.getName() );
        }
        userFeed = getUserFeed( lastUser );
        assertTrue( userFeed.size() == 1 );

        //retrieve feed
        userFeed = getUserFeed( lastUser );
        assertTrue( userFeed.size() == 1 );
        String postFollowContent = leader + ": something to look for " + UUID.randomUUID().toString();
        addActivity( leader, leader + " " + leader + "son", postFollowContent );

        refreshIndex( this.orgInfo.getName(), this.appInfo.getName() );

        //check feed
        userFeed = getUserFeed( lastUser );
        assertNotNull( userFeed );
        assertTrue( userFeed.size() > 1 );
        String serialized = userFeed.toString();
        assertTrue( serialized.indexOf( postFollowContent ) > 0 );
        assertTrue( serialized.indexOf( preFollowContent ) > 0 );
    }


    private void createUser( String username ) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put( "username", username );
        resource().path( "" + orgInfo.getName() + "/" + appInfo.getName() + "/users" )
                  .queryParam( "access_token", this.adminToken() ).accept( MediaType.APPLICATION_JSON )
                  .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload );
    }


    private JsonNode getUserFeed( String username ) throws IOException {
        JsonNode userFeed = mapper.readTree( resource()
                .path( "/" + orgInfo.getName() + "/" + appInfo.getName() + "/users/" + username + "/feed" )
                .queryParam( "access_token", this.adminToken() ).accept( MediaType.APPLICATION_JSON )
                .get( String.class ) );
        return userFeed.get( "entities" );
    }


    private void follow( String user, String followUser ) {
        //post follow
        resource()
                .path( "/" + orgInfo.getName() + "/" + appInfo.getName() + "/users/" + user + "/following/users/"
                        + followUser ).queryParam( "access_token", this.adminToken() )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                .post( String.class, new HashMap<String, String>() );
    }


    private void addActivity( String user, String name, String content ) {
        Map<String, Object> activityPayload = new HashMap<String, Object>();
        activityPayload.put( "content", content );
        activityPayload.put( "verb", "post" );
        Map<String, String> actorMap = new HashMap<String, String>();
        actorMap.put( "displayName", name );
        actorMap.put( "username", user );
        activityPayload.put( "actor", actorMap );
        resource().path( "/" + orgInfo.getName() + "/" + appInfo.getName() + "/users/" + user + "/activities" )
                  .queryParam( "access_token", this.adminToken() ).accept( MediaType.APPLICATION_JSON )
                  .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, activityPayload );
    }


    @Test
    public void mgmtCreateAndGetApplication() throws Exception {

        Map<String, String> data = new HashMap<String, String>();
        data.put( "name", "mgmt-org-app" );

        String orgName = orgInfo.getName();

        // POST /applications
        JsonNode appdata = mapper.readTree( resource().path( "/management/orgs/" + orgName + "/applications" )
                                                      .queryParam( "access_token", this.adminToken() )
                                                      .accept( MediaType.APPLICATION_JSON )
                                                      .type( MediaType.APPLICATION_JSON_TYPE )
                                                      .post( String.class, data ) );
        logNode( appdata );
        appdata = getEntity( appdata, 0 );

        refreshIndex( this.orgInfo.getName(), this.appInfo.getName() );

        assertEquals( orgName.toLowerCase() + "/mgmt-org-app", appdata.get( "name" ).asText() );
        assertNotNull( appdata.get( "metadata" ) );
        assertNotNull( appdata.get( "metadata" ).get( "collections" ) );
        assertNotNull( appdata.get( "metadata" ).get( "collections" ).get( "roles" ) );
        assertNotNull( appdata.get( "metadata" ).get( "collections" ).get( "roles" ).get( "title" ) );
        assertEquals( "Roles", appdata.get( "metadata" ).get( "collections" ).get( "roles" ).get( "title" ).asText() );
        assertEquals( 3, appdata.get( "metadata" ).get( "collections" ).get( "roles" ).get( "count" ).asInt() );

        refreshIndex( this.orgInfo.getName(), this.appInfo.getName() );

        // GET /applications/mgmt-org-app
        appdata = mapper.readTree(
                resource().path( "/management/orgs/" + orgInfo.getUuid() + "/applications/mgmt-org-app" )
                          .queryParam( "access_token", this.adminToken() )
                          .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                          .get( String.class ) );
        logNode( appdata );

        assertEquals( orgName.toLowerCase(), appdata.get( "organization" ).asText() );
        assertEquals( "mgmt-org-app", appdata.get( "applicationName" ).asText() );
        assertEquals( "http://sometestvalue/" + orgName.toLowerCase() + "/mgmt-org-app",
                appdata.get( "uri" ).textValue() );
        appdata = getEntity( appdata, 0 );

        assertEquals( orgName.toLowerCase() + "/mgmt-org-app", appdata.get( "name" ).asText() );
        assertEquals( "Roles", appdata.get( "metadata" ).get( "collections" ).get( "roles" ).get( "title" ).asText() );
        assertEquals( 3, appdata.get( "metadata" ).get( "collections" ).get( "roles" ).get( "count" ).asInt() );
    }

    @Test
    public void tokenTtl() throws Exception {

        long ttl = 2000;

        JsonNode node = resource().path( "/management/token" ).queryParam( "grant_type", "password" )
                                  .queryParam( "username", "test@usergrid.com" ).queryParam( "password", "test" )
                                  .queryParam( "ttl", String.valueOf( ttl ) ).accept( MediaType.APPLICATION_JSON )
                                  .get( JsonNode.class );

        long startTime = System.currentTimeMillis();

        String token = node.get( "access_token" ).textValue();

        assertNotNull( token );

        JsonNode userdata = resource().path( "/management/users/test@usergrid.com" ).queryParam( "access_token", token )
                                      .accept( MediaType.APPLICATION_JSON ).get( JsonNode.class );

        assertEquals( "test@usergrid.com", userdata.get( "data" ).get( "email" ).asText() );

        // wait for the token to expire
        Thread.sleep( ttl - (System.currentTimeMillis() - startTime) + 1000 );

        Status responseStatus = null;
        try {
            userdata = resource().path( "/management/users/test@usergrid.com" ).accept( MediaType.APPLICATION_JSON )
                                 .type( MediaType.APPLICATION_JSON_TYPE ).get( JsonNode.class );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.UNAUTHORIZED, responseStatus );
    }


    @Test
    public void token() throws Exception {
        JsonNode node = resource().path( "/management/token" ).queryParam( "grant_type", "password" )
                                  .queryParam( "username", "test@usergrid.com" ).queryParam( "password", "test" )
                                  .accept( MediaType.APPLICATION_JSON ).get( JsonNode.class );

        logNode( node );
        String token = node.get( "access_token" ).textValue();
        assertNotNull( token );

        // set an organization property
        HashMap<String, Object> payload = new HashMap<String, Object>();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "securityLevel", 5 );
        payload.put( OrganizationsResource.ORGANIZATION_PROPERTIES, properties );
        node = resource().path( "/management/organizations/test-organization" )
                         .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                         .type( MediaType.APPLICATION_JSON_TYPE ).put( JsonNode.class, payload );

        // ensure the organization property is included
        node = resource().path( "/management/token" ).queryParam( "access_token", token )
                         .accept( MediaType.APPLICATION_JSON ).get( JsonNode.class );
        logNode( node );

        JsonNode securityLevel = node.findValue( "securityLevel" );
        assertNotNull( securityLevel );
        assertEquals( 5L, securityLevel.asLong() );
    }


    @Test
    public void meToken() throws Exception {
        JsonNode node = resource().path( "/management/me" ).queryParam( "grant_type", "password" )
                                  .queryParam( "username", "test@usergrid.com" ).queryParam( "password", "test" )
                                  .accept( MediaType.APPLICATION_JSON ).get( JsonNode.class );

        logNode( node );
        String token = node.get( "access_token" ).textValue();
        assertNotNull( token );

        node = resource().path( "/management/me" ).queryParam( "access_token", token )
                         .accept( MediaType.APPLICATION_JSON ).get( JsonNode.class );
        logNode( node );

        assertNotNull( node.get( "passwordChanged" ) );
        assertNotNull( node.get( "access_token" ) );
        assertNotNull( node.get( "expires_in" ) );
        JsonNode userNode = node.get( "user" );
        assertNotNull( userNode );
        assertNotNull( userNode.get( "uuid" ) );
        assertNotNull( userNode.get( "username" ) );
        assertNotNull( userNode.get( "email" ) );
        assertNotNull( userNode.get( "name" ) );
        assertNotNull( userNode.get( "properties" ) );
        JsonNode orgsNode = userNode.get( "organizations" );
        assertNotNull( orgsNode );
        JsonNode orgNode = orgsNode.get( "test-organization" );
        assertNotNull( orgNode );
        assertNotNull( orgNode.get( "name" ) );
        assertNotNull( orgNode.get( "properties" ) );
    }


    @Test
    public void meTokenPost() throws Exception {
        Map<String, String> payload =
                hashMap( "grant_type", "password" ).map( "username", "test@usergrid.com" ).map( "password", "test" );

        JsonNode node = resource().path( "/management/me" ).accept( MediaType.APPLICATION_JSON )
                                  .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );

        logNode( node );
        String token = node.get( "access_token" ).textValue();

        assertNotNull( token );

        node = resource().path( "/management/me" ).queryParam( "access_token", token )
                         .accept( MediaType.APPLICATION_JSON ).get( JsonNode.class );
        logNode( node );
    }


    @Test
    public void meTokenPostForm() {

        Form form = new Form();
        form.add( "grant_type", "password" );
        form.add( "username", "test@usergrid.com" );
        form.add( "password", "test" );

        JsonNode node = resource().path( "/management/me" ).accept( MediaType.APPLICATION_JSON )
                                  .type( MediaType.APPLICATION_FORM_URLENCODED_TYPE )
                                  .entity( form, MediaType.APPLICATION_FORM_URLENCODED_TYPE ).post( JsonNode.class );

        logNode( node );
        String token = node.get( "access_token" ).textValue();

        assertNotNull( token );

        node = resource().path( "/management/me" ).queryParam( "access_token", token )
                         .accept( MediaType.APPLICATION_JSON ).get( JsonNode.class );
        logNode( node );
    }


    @Test
    public void ttlNan() throws Exception {

        Map<String, String> payload =
                hashMap( "grant_type", "password" ).map( "username", "test@usergrid.com" ).map( "password", "test" )
                                                   .map( "ttl", "derp" );

        Status responseStatus = null;
        try {
            resource().path( "/management/token" ).accept( MediaType.APPLICATION_JSON )
                      .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void ttlOverMax() throws Exception {

        Map<String, String> payload =
                hashMap( "grant_type", "password" ).map( "username", "test@usergrid.com" ).map( "password", "test" )
                                                   .map( "ttl", Long.MAX_VALUE + "" );

        Status responseStatus = null;

        try {
            resource().path( "/management/token" ).accept( MediaType.APPLICATION_JSON )
                      .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void revokeToken() throws Exception {
        String token1 = super.adminToken();
        String token2 = super.adminToken();

        JsonNode response = resource().path( "/management/users/test" ).queryParam( "access_token", token1 )
                                      .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                      .get( JsonNode.class );

        assertEquals( "test@usergrid.com", response.get( "data" ).get( "email" ).asText() );

        response = resource().path( "/management/users/test" ).queryParam( "access_token", token2 )
                             .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                             .get( JsonNode.class );

        assertEquals( "test@usergrid.com", response.get( "data" ).get( "email" ).asText() );

        // now revoke the tokens
        response =
                resource().path( "/management/users/test/revoketokens" ).queryParam( "access_token", superAdminToken() )
                          .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                          .post( JsonNode.class );

        // the tokens shouldn't work

        Status status = null;

        try {
            response = resource().path( "/management/users/test" ).queryParam( "access_token", token1 )
                                 .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                 .get( JsonNode.class );
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.UNAUTHORIZED, status );

        status = null;

        try {
            response = resource().path( "/management/users/test" ).queryParam( "access_token", token2 )
                                 .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                 .get( JsonNode.class );
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.UNAUTHORIZED, status );

        String token3 = super.adminToken();
        String token4 = super.adminToken();

        response = resource().path( "/management/users/test" ).queryParam( "access_token", token3 )
                             .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                             .get( JsonNode.class );

        assertEquals( "test@usergrid.com", response.get( "data" ).get( "email" ).asText() );

        response = resource().path( "/management/users/test" ).queryParam( "access_token", token4 )
                             .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                             .get( JsonNode.class );

        assertEquals( "test@usergrid.com", response.get( "data" ).get( "email" ).asText() );

        // now revoke the token3
        response = resource().path( "/management/users/test/revoketoken" ).queryParam( "access_token", token3 )
                             .queryParam( "token", token3 ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class );

        // the token3 shouldn't work

        status = null;

        try {
            response = resource().path( "/management/users/test" ).queryParam( "access_token", token3 )
                                 .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                 .get( JsonNode.class );
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.UNAUTHORIZED, status );

        status = null;

        try {
            response = resource().path( "/management/users/test" ).queryParam( "access_token", token4 )
                                 .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                 .get( JsonNode.class );

            status = Status.OK;
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.OK, status );
    }


    @Test
    public void testValidateExternalToken() throws Exception {

        // create a new admin user, get access token

        String rand = RandomStringUtils.randomAlphanumeric(10);
        final String username = "user_" + rand;
        OrganizationOwnerInfo orgInfo = setup.getMgmtSvc().createOwnerAndOrganization(
                username, username, "Test User", username + "@example.com", "password" );

        Map<String, Object> loginInfo = new HashMap<String, Object>() {{
            put("username", username );
            put("password", "password");
            put("grant_type", "password");
        }};
        JsonNode accessInfoNode = resource().path("/management/token")
            .type( MediaType.APPLICATION_JSON_TYPE )
            .post( JsonNode.class, loginInfo );
        String accessToken = accessInfoNode.get( "access_token" ).textValue();

        // set the Usergrid Central SSO URL because Tomcat port is dynamically assigned

        String suToken = superAdminToken();
        Map<String, String> props = new HashMap<String, String>();
        props.put( USERGRID_CENTRAL_URL, getBaseURI().toURL().toExternalForm() );
        resource().path( "/testproperties" )
                .queryParam( "access_token", suToken)
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .post( props );

        // attempt to validate the token, must be valid

        JsonNode validatedNode = resource().path( "/management/externaltoken" )
            .queryParam( "access_token", suToken ) // as superuser
            .queryParam( "ext_access_token", accessToken )
            .queryParam( "ttl", "1000" )
            .get( JsonNode.class );
        String validatedAccessToken = validatedNode.get( "access_token" ).textValue();
        assertEquals( accessToken, validatedAccessToken );

        // attempt to validate an invalid token, must fail

        try {
            resource().path( "/management/externaltoken" )
                .queryParam( "access_token", suToken ) // as superuser
                .queryParam( "ext_access_token", "rubbish_token")
                .queryParam( "ttl", "1000" )
                .get( JsonNode.class );
            fail("Validation should have failed");
        } catch ( UniformInterfaceException actual ) {
            assertEquals( 404, actual.getResponse().getStatus() );
            String errorMsg = actual.getResponse().getEntity( JsonNode.class ).get( "error_description" ).toString();
            logger.error( "ERROR: " + errorMsg );
            assertTrue( errorMsg.contains( "Cannot find Admin User" ) );
        }



        // TODO: how do we test the create new user and organization case?



        // unset the Usergrid Central SSO URL so it does not interfere with other tests

        props.put( USERGRID_CENTRAL_URL, "" );
        resource().path( "/testproperties" )
                .queryParam( "access_token", suToken)
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .post( props );

    }


    @Test
    public void testSuperuserOnlyWhenValidateExternalTokensEnabled() throws Exception {

        // create an org and an admin user

        String rand = RandomStringUtils.randomAlphanumeric( 10 );
        final String username = "user_" + rand;
        OrganizationOwnerInfo orgInfo = setup.getMgmtSvc().createOwnerAndOrganization(
                username, username, "Test User", username + "@example.com", "password" );

        // turn on validate external tokens by setting the usergrid.central.url

        String suToken = superAdminToken();
        Map<String, String> props = new HashMap<String, String>();
        props.put( USERGRID_CENTRAL_URL, getBaseURI().toURL().toExternalForm());
        resource().path( "/testproperties" )
                .queryParam( "access_token", suToken)
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .post( props );

        // calls to login as an Admin User must now fail

        try {

            Map<String, Object> loginInfo = new HashMap<String, Object>() {{
                put("username", username );
                put("password", "password");
                put("grant_type", "password");
            }};
            JsonNode accessInfoNode = resource().path("/management/token")
                    .type( MediaType.APPLICATION_JSON_TYPE )
                    .post( JsonNode.class, loginInfo );
            fail("Login as Admin User must fail when validate external tokens is enabled");

        } catch ( UniformInterfaceException actual ) {
            assertEquals( 400, actual.getResponse().getStatus() );
            String errorMsg = actual.getResponse().getEntity( JsonNode.class ).get( "error_description" ).toString();
            logger.error( "ERROR: " + errorMsg );
            assertTrue( errorMsg.contains( "Admin Users must login via" ));

        } catch ( Exception e ) {
            fail( "We expected a UniformInterfaceException" );
        }

        // login as superuser must succeed

        Map<String, Object> loginInfo = new HashMap<String, Object>() {{
            put("username", "superuser");
            put("password", "superpassword");
            put("grant_type", "password");
        }};
        JsonNode accessInfoNode = resource().path("/management/token")
                .type( MediaType.APPLICATION_JSON_TYPE )
                .post( JsonNode.class, loginInfo );
        String accessToken = accessInfoNode.get( "access_token" ).textValue();
        assertNotNull( accessToken );

        // turn off validate external tokens by un-setting the usergrid.central.url

        props.put( USERGRID_CENTRAL_URL, "" );
        resource().path( "/testproperties" )
                .queryParam( "access_token", suToken)
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .post( props );
    }

}
