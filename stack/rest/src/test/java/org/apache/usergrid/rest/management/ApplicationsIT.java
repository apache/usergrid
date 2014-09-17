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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;


/**
 *
 *
 */
public class ApplicationsIT extends AbstractRestIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test
    @Ignore("ignored because this test fails because it does not account for the default app "
            + "created by the TestContext and the sandbox app. "
            + "see also: https://issues.apache.org/jira/browse/USERGRID-210 ")
    public void test10AppLimit() throws IOException {

        int size = 11;

        Set<String> appNames = new HashSet<String>( size );

        for ( int i = 0; i < size; i++ ) {
            final String name = i + "";

            appNames.add( name );

            context.withApp( name ).createAppForOrg();
            refreshIndex(context.getOrgName(), name);
        }


        //now go through and ensure each entry is present

        final JsonNode apps = context.management().orgs().organization( context.getOrgName() ).apps().get();

        final JsonNode data = apps.get( "data" );

        final String orgName = context.getOrgName();


        final Set<String> copy = new HashSet<String> (appNames);

        for(String appName: copy){

            final String mapEntryName = String.format( "%s/%s", orgName.toLowerCase(),  appName.toLowerCase());

            JsonNode orgApp = data.get( mapEntryName);

            if(orgApp != null){
                appNames.remove( appName );
            }

        }

        assertEquals("All elements removed", 0, appNames.size());

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

        refreshIndex("test-organization", "test-app");

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
        refreshIndex("test-organization", "test-app");

        String preFollowContent = leader + ": pre-something to look for " + UUID.randomUUID().toString();

        addActivity( leader, leader + " " + leader + "son", preFollowContent );
        refreshIndex("test-organization", "test-app");

        String lastUser = followers.get( followers.size() - 1 );
        int i = 0;
        for ( String user : followers ) {
            createUser( user );
            refreshIndex("test-organization", "test-app");
            follow( user, leader );
            refreshIndex("test-organization", "test-app");
        }
        userFeed = getUserFeed( lastUser );
        assertTrue( userFeed.size() == 1 );

        //retrieve feed
        userFeed = getUserFeed( lastUser );
        assertTrue( userFeed.size() == 1 );
        String postFollowContent = leader + ": something to look for " + UUID.randomUUID().toString();
        addActivity( leader, leader + " " + leader + "son", postFollowContent );

        refreshIndex("test-organization", "test-app");

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

        refreshIndex("test-organization", "test-app");

        assertEquals( "test-organization/mgmt-org-app", appdata.get( "name" ).asText() );
        assertNotNull(appdata.get( "metadata" ));
        assertNotNull(appdata.get( "metadata" ).get( "collections" ));
        assertNotNull(appdata.get( "metadata" ).get( "collections" ).get( "roles" ));
        assertNotNull(appdata.get( "metadata" ).get( "collections" ).get( "roles" ).get( "title" ));
        assertEquals( "Roles", appdata.get( "metadata" ).get( "collections" ).get( "roles" ).get( "title" ).asText() );
        assertEquals( 3, appdata.get( "metadata" ).get( "collections" ).get( "roles" ).get( "count" ).asInt() );

        refreshIndex("test-organization", "test-app");

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
}


