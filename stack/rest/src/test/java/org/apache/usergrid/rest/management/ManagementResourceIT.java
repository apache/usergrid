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

import org.junit.Rule;
import org.junit.Test;

import org.apache.commons.lang.StringUtils;


import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
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

public class ManagementResourceIT extends AbstractRestIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    public ManagementResourceIT() throws Exception {

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
            resource().path( String.format( "/management/orgs/%s", orgName ) )
                      .queryParam( "access_token", context.getActiveUser().getToken() )
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
            resource().path( "/management/orgs/" + context.getOrgName() )
                      .queryParam( "access_token", context.getActiveUser().getToken() )
                      .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( String.class );
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertNull( status );

        //test getting the organization by org

        status = null;
        try {
            resource().path( String.format( "/management/orgs/%s", context.getOrgUuid() ) )
                      .queryParam( "access_token", context.getActiveUser().getToken() )
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

        refreshIndex( context.getOrgName(), context.getAppName() );

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
        refreshIndex( context.getOrgName(), context.getAppName() );

        String preFollowContent = leader + ": pre-something to look for " + UUID.randomUUID().toString();

        addActivity( leader, leader + " " + leader + "son", preFollowContent );
        refreshIndex( context.getOrgName(), context.getAppName() );

        String lastUser = followers.get( followers.size() - 1 );
        int i = 0;
        for ( String user : followers ) {
            createUser( user );
            refreshIndex( context.getOrgName(), context.getAppName() );
            follow( user, leader );
            refreshIndex( context.getOrgName(), context.getAppName() );
        }
        userFeed = getUserFeed( lastUser );
        assertTrue( userFeed.size() == 1 );

        //retrieve feed
        userFeed = getUserFeed( lastUser );
        assertTrue( userFeed.size() == 1 );
        String postFollowContent = leader + ": something to look for " + UUID.randomUUID().toString();
        addActivity( leader, leader + " " + leader + "son", postFollowContent );

        refreshIndex( context.getOrgName(), context.getAppName() );

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
        resource().path( "" + context.getOrgName() + "/" + context.getAppName() + "/users" )
                  .queryParam( "access_token", context.getActiveUser().getToken() ).accept( MediaType.APPLICATION_JSON )
                  .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload );
    }


    private JsonNode getUserFeed( String username ) throws IOException {
        JsonNode userFeed = mapper.readTree( resource()
                .path( "/" + context.getOrgName() + "/" + context.getAppName() + "/users/" + username + "/feed" )
                .queryParam( "access_token", context.getActiveUser().getToken() ).accept( MediaType.APPLICATION_JSON )
                .get( String.class ) );
        return userFeed.get( "entities" );
    }


    private void follow( String user, String followUser ) {
        //post follow
        resource()
                .path( "/" + context.getOrgName() + "/" + context.getAppName() + "/users/" + user + "/following/users/"
                        + followUser ).queryParam( "access_token", context.getActiveUser().getToken() )
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
        resource().path( "/" + context.getOrgName() + "/" + context.getAppName() + "/users/" + user + "/activities" )
                  .queryParam( "access_token", context.getActiveUser().getToken() ).accept( MediaType.APPLICATION_JSON )
                  .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, activityPayload );
    }


    @Test
    public void mgmtCreateAndGetApplication() throws Exception {

        Map<String, String> data = new HashMap<String, String>();
        data.put( "name", "mgmt-org-app" );

        String orgName = context.getOrgName();

        // POST /applications
        JsonNode appdata = mapper.readTree( resource().path( "/management/orgs/" + orgName + "/applications" )
                                                      .queryParam( "access_token", context.getActiveUser().getToken() )
                                                      .accept( MediaType.APPLICATION_JSON )
                                                      .type( MediaType.APPLICATION_JSON_TYPE )
                                                      .post( String.class, data ) );
        logNode( appdata );
        appdata = getEntity( appdata, 0 );

        refreshIndex( orgName, context.getAppName() );

        assertEquals( orgName.toLowerCase() + "/mgmt-org-app", appdata.get( "name" ).asText() );
        assertNotNull( appdata.get( "metadata" ) );
        assertNotNull( appdata.get( "metadata" ).get( "collections" ) );
        assertNotNull( appdata.get( "metadata" ).get( "collections" ).get( "roles" ) );
        assertNotNull( appdata.get( "metadata" ).get( "collections" ).get( "roles" ).get( "title" ) );
        assertEquals( "Roles", appdata.get( "metadata" ).get( "collections" ).get( "roles" ).get( "title" ).asText() );
        assertEquals( 3, appdata.get( "metadata" ).get( "collections" ).get( "roles" ).get( "count" ).asInt() );

        refreshIndex( orgName, context.getAppName() );

        // GET /applications/mgmt-org-app
        appdata = mapper.readTree(
                resource().path( "/management/orgs/" + context.getOrgUuid() + "/applications/mgmt-org-app" )
                          .queryParam( "access_token", context.getActiveUser().getToken() )
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
}
