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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.management.organizations.OrganizationsResource;

import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.representation.Form;
import java.io.IOException;

import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * @author tnine
 */
@Concurrent()
public class ManagementResourceIT extends AbstractRestIT {

    public ManagementResourceIT() throws Exception {

    }


    /**
     * Test if we can reset our password as an admin
     */
    @Test
    public void setSelfAdminPasswordAsAdmin() throws IOException {

        String newPassword = "foo";

        Map<String, String> data = new HashMap<String, String>();
        data.put( "newpassword", newPassword );
        data.put( "oldpassword", "test" );

        // change the password as admin. The old password isn't required
        JsonNode node = mapper.readTree( resource().path( "/management/users/test/password" ).accept( MediaType.APPLICATION_JSON )
                                  .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, data ));

        assertNull( getError( node ) );

        adminAccessToken = mgmtToken( "test", newPassword );

        data.put( "oldpassword", newPassword );
        data.put( "newpassword", "test" );

        node = mapper.readTree( resource().path( "/management/users/test/password" ).queryParam( "access_token", adminAccessToken )
                         .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                         .post( String.class, data ));

        assertNull( getError( node ) );
    }


    @Test
    public void passwordMismatchErrorAdmin() {
        String origPassword = "foo";
        String newPassword = "bar";

        Map<String, String> data = new HashMap<String, String>();
        data.put( "newpassword", origPassword );

        // now change the password, with an incorrect old password

        data.put( "oldpassword", origPassword );
        data.put( "newpassword", newPassword );

        Status responseStatus = null;

        try {
            resource().path( "/management/users/test/password" ).accept( MediaType.APPLICATION_JSON )
                      .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, data );
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertNotNull( responseStatus );

        assertEquals( Status.BAD_REQUEST, responseStatus );
    }


    @Test
    public void setAdminPasswordAsSysAdmin() throws IOException {

        String superToken = superAdminToken();

        String newPassword = "foo";

        Map<String, String> data = new HashMap<String, String>();
        data.put( "newpassword", newPassword );

        // change the password as admin. The old password isn't required
        JsonNode node = mapper.readTree( resource().path( "/management/users/test/password" ).queryParam( "access_token", superToken )
                                  .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                  .post( String.class, data ));

        assertNull( getError( node ) );

        // log in with the new password
        String token = mgmtToken( "test", newPassword );

        assertNotNull( token );

        data.put( "newpassword", "test" );

        // now change the password back
        node = mapper.readTree( resource().path( "/management/users/test/password" ).queryParam( "access_token", superToken )
                         .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                         .post( String.class, data ));

        assertNull( getError( node ) );
    }


    /**
     * Test that admins can't view organizations they're not authorized to view.
     */
    @Test
    public void crossOrgsNotViewable() throws Exception {

        OrganizationOwnerInfo orgInfo = setup.getMgmtSvc().createOwnerAndOrganization( "crossOrgsNotViewable",
                "crossOrgsNotViewable", "TestName", "crossOrgsNotViewable@usergrid.org", "password" );

        // check that the test admin cannot access the new org info

        Status status = null;

        try {
            resource().path( String.format( "/management/orgs/%s", orgInfo.getOrganization().getName() ) )
                      .queryParam( "access_token", adminAccessToken ).accept( MediaType.APPLICATION_JSON )
                      .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class );
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertNotNull( status );
        assertEquals( Status.UNAUTHORIZED, status );

        status = null;

        try {
            resource().path( String.format( "/management/orgs/%s", orgInfo.getOrganization().getUuid() ) )
                      .queryParam( "access_token", adminAccessToken ).accept( MediaType.APPLICATION_JSON )
                      .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class );
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertNotNull( status );
        assertEquals( Status.UNAUTHORIZED, status );

        // this admin should have access to test org
        status = null;
        try {
            resource().path( "/management/orgs/test-organization" ).queryParam( "access_token", adminAccessToken )
                      .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                      .get( String.class );
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertNull( status );

        OrganizationInfo org = setup.getMgmtSvc().getOrganizationByName( "test-organization" );

        status = null;
        try {
            resource().path( String.format( "/management/orgs/%s", org.getUuid() ) )
                      .queryParam( "access_token", adminAccessToken ).accept( MediaType.APPLICATION_JSON )
                      .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class );
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertNull( status );
    }


    @Test
    public void mgmtUserFeed() throws Exception {
        JsonNode userdata = mapper.readTree( resource().path( "/management/users/test@usergrid.com/feed" )
                                      .queryParam( "access_token", adminAccessToken )
                                      .accept( MediaType.APPLICATION_JSON ).get( String.class ));
        assertTrue( StringUtils.contains( this.getEntity( userdata, 0 ).get( "title" ).asText(),
                "<a href=\"mailto:test@usergrid.com\">" ) );
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
        String preFollowContent = leader + ": pre-something to look for " + UUID.randomUUID().toString();
        addActivity( leader, leader + " " + leader + "son", preFollowContent );
        String lastUser = followers.get( followers.size() - 1 );
        int i = 0;
        for ( String user : followers ) {
            createUser( user );
            follow( user, leader );
        }
        userFeed = getUserFeed( lastUser );
        assertTrue( userFeed.size() == 1 );

        //retrieve feed
        userFeed = getUserFeed( lastUser );
        assertTrue( userFeed.size() == 1 );
        String postFollowContent = leader + ": something to look for " + UUID.randomUUID().toString();
        addActivity( leader, leader + " " + leader + "son", postFollowContent );
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
        resource().path( "/test-organization/test-app/users" ).queryParam( "access_token", access_token )
                  .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                  .post( String.class, payload );
    }


    private JsonNode getUserFeed( String username ) throws IOException {
        JsonNode userFeed = mapper.readTree( resource().path( "/test-organization/test-app/users/" + username + "/feed" )
                                      .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                                      .get( String.class ));
        return userFeed.get( "entities" );
    }


    private void follow( String user, String followUser ) {
        //post follow
        resource().path( "/test-organization/test-app/users/" + user + "/following/users/" + followUser )
                  .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                  .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, new HashMap<String, String>() );
    }


    private void addActivity( String user, String name, String content ) {
        Map<String, Object> activityPayload = new HashMap<String, Object>();
        activityPayload.put( "content", content );
        activityPayload.put( "verb", "post" );
        Map<String, String> actorMap = new HashMap<String, String>();
        actorMap.put( "displayName", name );
        actorMap.put( "username", user );
        activityPayload.put( "actor", actorMap );
        resource().path( "/test-organization/test-app/users/" + user + "/activities" )
                  .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                  .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, activityPayload );
    }


    @Test
    public void mgmtCreateAndGetApplication() throws Exception {

        OrganizationInfo orgInfo = setup.getMgmtSvc().getOrganizationByName( "test-organization" );
        Map<String, String> data = new HashMap<String, String>();
        data.put( "name", "mgmt-org-app" );

        // POST /applications
        JsonNode appdata = mapper.readTree( resource().path( "/management/orgs/" + orgInfo.getUuid() + "/applications" )
                                     .queryParam( "access_token", adminToken() ).accept( MediaType.APPLICATION_JSON )
                                     .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, data ));
        logNode( appdata );
        appdata = getEntity( appdata, 0 );

        assertEquals( "test-organization/mgmt-org-app", appdata.get( "name" ).asText() );
        assertEquals( "Roles", appdata.get( "metadata" ).get( "collections" ).get( "roles" ).get( "title" ).asText() );
        assertEquals( 3, appdata.get( "metadata" ).get( "collections" ).get( "roles" ).get( "count" ).asInt() );

        // GET /applications/mgmt-org-app
        appdata = mapper.readTree( resource().path( "/management/orgs/" + orgInfo.getUuid() + "/applications/mgmt-org-app" )
                            .queryParam( "access_token", adminToken() ).accept( MediaType.APPLICATION_JSON )
                            .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        logNode( appdata );

        assertEquals( "test-organization", appdata.get( "organization" ).asText() );
        assertEquals( "mgmt-org-app", appdata.get( "applicationName" ).asText() );
        assertEquals( "http://sometestvalue/test-organization/mgmt-org-app", appdata.get( "uri" ).textValue() );
        appdata = getEntity( appdata, 0 );

        assertEquals( "test-organization/mgmt-org-app", appdata.get( "name" ).asText() );
        assertEquals( "Roles", appdata.get( "metadata" ).get( "collections" ).get( "roles" ).get( "title" ).asText() );
        assertEquals( 3, appdata.get( "metadata" ).get( "collections" ).get( "roles" ).get( "count" ).asInt() );
    }


    @Test
    public void tokenTtl() throws Exception {

        long ttl = 2000;

        JsonNode node = mapper.readTree( resource().path( "/management/token" ).queryParam( "grant_type", "password" )
                                  .queryParam( "username", "test@usergrid.com" ).queryParam( "password", "test" )
                                  .queryParam( "ttl", String.valueOf( ttl ) ).accept( MediaType.APPLICATION_JSON )
                                  .get( String.class ));

        long startTime = System.currentTimeMillis();

        String token = node.get( "access_token" ).textValue();

        assertNotNull( token );

        JsonNode userdata = mapper.readTree( resource().path( "/management/users/test@usergrid.com" ).queryParam( "access_token", token )
                                      .accept( MediaType.APPLICATION_JSON ).get( String.class ));

        assertEquals( "test@usergrid.com", userdata.get( "data" ).get( "email" ).asText() );

        // wait for the token to expire
        Thread.sleep( ttl - ( System.currentTimeMillis() - startTime ) + 1000 );

        Status responseStatus = null;
        try {
            userdata = mapper.readTree( resource().path( "/management/users/test@usergrid.com" ).accept( MediaType.APPLICATION_JSON )
                                 .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        }
        catch ( UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.UNAUTHORIZED, responseStatus );
    }


    @Test
    public void token() throws Exception {
        JsonNode node = mapper.readTree( resource().path( "/management/token" ).queryParam( "grant_type", "password" )
                                  .queryParam( "username", "test@usergrid.com" ).queryParam( "password", "test" )
                                  .accept( MediaType.APPLICATION_JSON ).get( String.class ));

        logNode( node );
        String token = node.get( "access_token" ).textValue();
        assertNotNull( token );

        // set an organization property
        HashMap<String, Object> payload = new HashMap<String, Object>();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "securityLevel", 5 );
        payload.put( OrganizationsResource.ORGANIZATION_PROPERTIES, properties );
        node = mapper.readTree( resource().path( "/management/organizations/test-organization" )
                         .queryParam( "access_token", superAdminToken() ).accept( MediaType.APPLICATION_JSON )
                         .type( MediaType.APPLICATION_JSON_TYPE ).put( String.class, payload ));

        // ensure the organization property is included
        node = mapper.readTree( resource().path( "/management/token" ).queryParam( "access_token", token )
                         .accept( MediaType.APPLICATION_JSON ).get( String.class ));
        logNode( node );

        JsonNode securityLevel = node.findValue( "securityLevel" );
        assertNotNull( securityLevel );
        assertEquals( 5L, securityLevel.asLong() );
    }


    @Test
    public void meToken() throws Exception {
        JsonNode node = mapper.readTree( resource().path( "/management/me" ).queryParam( "grant_type", "password" )
                                  .queryParam( "username", "test@usergrid.com" ).queryParam( "password", "test" )
                                  .accept( MediaType.APPLICATION_JSON ).get( String.class ));

        logNode( node );
        String token = node.get( "access_token" ).textValue();
        assertNotNull( token );

        node = mapper.readTree( resource().path( "/management/me" ).queryParam( "access_token", token )
                         .accept( MediaType.APPLICATION_JSON ).get( String.class ));
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

        JsonNode node = mapper.readTree( resource().path( "/management/me" ).accept( MediaType.APPLICATION_JSON )
                                  .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));

        logNode( node );
        String token = node.get( "access_token" ).textValue();

        assertNotNull( token );

        node = mapper.readTree( resource().path( "/management/me" ).queryParam( "access_token", token )
                         .accept( MediaType.APPLICATION_JSON ).get( String.class ));
        logNode( node );
    }


    @Test
    public void meTokenPostForm() throws IOException {

        Form form = new Form();
        form.add( "grant_type", "password" );
        form.add( "username", "test@usergrid.com" );
        form.add( "password", "test" );

        JsonNode node = mapper.readTree( resource().path( "/management/me" ).accept( MediaType.APPLICATION_JSON )
                                  .type( MediaType.APPLICATION_FORM_URLENCODED_TYPE )
                                  .entity( form, MediaType.APPLICATION_FORM_URLENCODED_TYPE ).post( String.class ));

        logNode( node );
        String token = node.get( "access_token" ).textValue();

        assertNotNull( token );

        node = mapper.readTree( resource().path( "/management/me" ).queryParam( "access_token", token )
                         .accept( MediaType.APPLICATION_JSON ).get( String.class ));
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
                      .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload );
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
                      .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload );
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

        JsonNode response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token1 )
                                      .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                      .get( String.class ));

        assertEquals( "test@usergrid.com", response.get( "data" ).get( "email" ).asText() );

        response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token2 )
                             .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                             .get( String.class ));

        assertEquals( "test@usergrid.com", response.get( "data" ).get( "email" ).asText() );

        // now revoke the tokens
        response = mapper.readTree( resource().path( "/management/users/test/revoketokens" ).queryParam( "access_token", superAdminToken() )
                          .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                          .post( String.class ));

        // the tokens shouldn't work

        Status status = null;

        try {
            response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token1 )
                                 .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                 .get( String.class ));
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.UNAUTHORIZED, status );

        status = null;

        try {
            response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token2 )
                                 .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                 .get( String.class ));
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.UNAUTHORIZED, status );

        String token3 = super.adminToken();
        String token4 = super.adminToken();

        response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token3 )
                             .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                             .get( String.class ));

        assertEquals( "test@usergrid.com", response.get( "data" ).get( "email" ).asText() );

        response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token4 )
                             .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                             .get( String.class ));

        assertEquals( "test@usergrid.com", response.get( "data" ).get( "email" ).asText() );

        // now revoke the token3
        response = mapper.readTree( resource().path( "/management/users/test/revoketoken" ).queryParam( "access_token", token3 )
                             .queryParam( "token", token3 ).accept( MediaType.APPLICATION_JSON )
                             .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class ));

        // the token3 shouldn't work

        status = null;

        try {
            response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token3 )
                                 .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                 .get( String.class ));
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.UNAUTHORIZED, status );

        status = null;

        try {
            response = mapper.readTree( resource().path( "/management/users/test" ).queryParam( "access_token", token4 )
                                 .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                                 .get( String.class ));

            status = Status.OK;
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertEquals( Status.OK, status );
    }

}
