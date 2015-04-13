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


import java.util.*;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.usergrid.rest.test.resource2point0.model.*;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
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

public class ManagementResourceIT extends org.apache.usergrid.rest.test.resource2point0.AbstractRestIT {

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

        Entity payload =
                new Entity().chainPut("company", "Apigee" );

        Organization organization = new Organization(orgName,username,email,name,password,payload);

        Organization node = management().orgs().post(  organization );

        // check that the test admin cannot access the new org info

        Status status = null;

        try {
            this.management().orgs().organization(this.clientSetup.getOrganizationName()).get(String.class);
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertNotNull( status );
        assertEquals( Status.UNAUTHORIZED, status );

        // this admin should have access to test org
        status = null;
        try {
            this.management().orgs().organization(this.clientSetup.getOrganizationName()).get( String.class );
        }
        catch ( UniformInterfaceException uie ) {
            status = uie.getResponse().getClientResponseStatus();
        }

        assertNull( status );

        //test getting the organization by org

        status = null;
        try {
            this.management().orgs().organization(this.clientSetup.getOrganizationName()).get( String.class );
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

        refreshIndex(  );

        checkFeed( "leader1", users1 );
        //try with 11
        List<String> users2 = new ArrayList<String>();
        for ( i = 20; i < 31; i++ ) {
            users2.add( "follower" + Integer.toString( i ) );
        }
        checkFeed( "leader2", users2 );
    }


    private void checkFeed( String leader, List<String> followers ) throws IOException {
        List<Entity> userFeed;

        //create user
        createUser( leader );
        refreshIndex(   );

        String preFollowContent = leader + ": pre-something to look for " + UUID.randomUUID().toString();

        addActivity( leader, leader + " " + leader + "son", preFollowContent );
        refreshIndex(  );

        String lastUser = followers.get( followers.size() - 1 );
        int i = 0;
        for ( String user : followers ) {
            createUser( user );
            refreshIndex( );
            follow( user, leader );
            refreshIndex(  );
        }
        userFeed = getUserFeed( lastUser );
        assertTrue( userFeed.size() == 1 );

        //retrieve feed
        userFeed = getUserFeed( lastUser );
        assertTrue( userFeed.size() == 1 );
        String postFollowContent = leader + ": something to look for " + UUID.randomUUID().toString();
        addActivity( leader, leader + " " + leader + "son", postFollowContent );

        refreshIndex(  );

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
       this.app().collection("users").post(String.class, payload);
    }


    private List<Entity> getUserFeed( String username ) throws IOException {
        Collection collection = this.app().collection("users").entity(username).collection("feed").get();
        return collection.getResponse().getEntities();
    }


    private void follow( String user, String followUser ) {
        //post follow
        Entity entity = this.app().collection("users").entity(user).collection("following").collection("users").entity(followUser).post();
    }


    private void addActivity( String user, String name, String content ) {
        Map<String, Object> activityPayload = new HashMap<String, Object>();
        activityPayload.put( "content", content );
        activityPayload.put( "verb", "post" );
        Map<String, String> actorMap = new HashMap<String, String>();
        actorMap.put( "displayName", name );
        actorMap.put( "username", user );
        activityPayload.put("actor", actorMap);
        Entity entity = this.app().collection("users").entity(user).collection("activities").post(new Entity(activityPayload));

    }


    @Test
    public void mgmtCreateAndGetApplication() throws Exception {



        // POST /applications
        ApiResponse apiResponse = management().orgs().organization(clientSetup.getOrganizationName()).app().post(new Application("mgmt-org-app"));


        refreshIndex();

        Entity appdata = apiResponse.getEntities().get(0);
        assertEquals((clientSetup.getOrganizationName() + "/mgmt-org-app").toLowerCase(), appdata.get("name").toString().toLowerCase());
        assertNotNull(appdata.get("metadata"));
        Map metadata =(Map) appdata.get( "metadata" );
        assertNotNull(metadata.get("collections"));
        Map collections =  ((Map)metadata.get("collections"));
        assertNotNull(collections.get("roles"));
        Map roles =(Map) collections.get("roles");
        assertNotNull(roles.get("title"));
        assertEquals("Roles", roles.get("title").toString());
        assertEquals(3, roles.size());

        refreshIndex(   );

        // GET /applications/mgmt-org-app


        Entity app = management().orgs().organization(clientSetup.getOrganizationName()).app().addToPath("mgmt-org-app").get();


        assertEquals(this.clientSetup.getOrganizationName().toLowerCase(), app.get("organization").toString());
        assertEquals( "mgmt-org-app", app.get( "applicationName" ).toString() );
        assertEquals( "http://sometestvalue/" + this.clientSetup.getOrganizationName().toLowerCase() + "/mgmt-org-app",
            app.get( "uri" ).toString() );

        assertEquals( clientSetup.getOrganizationName().toLowerCase() + "/mgmt-org-app", app.get( "name" ).toString() );
        metadata =(Map) appdata.get( "metadata" );
        collections =  ((Map)metadata.get("collections"));
        roles =(Map) collections.get("roles");

        assertEquals( "Roles", roles.get("title").toString() );
        assertEquals(3, roles.size());
    }
}
