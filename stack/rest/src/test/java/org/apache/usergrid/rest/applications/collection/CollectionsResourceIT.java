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
package org.apache.usergrid.rest.applications.collection;


import java.io.IOException;
import java.util.NoSuchElementException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.cassandra.Concurrent;

import static org.junit.Assert.*;


/**
 * @author zznate
 * @author tnine
 * @author rockerston
 *
 *  misc tests for collections
 */
@Concurrent()
public class CollectionsResourceIT extends AbstractRestIT {

    private static Logger log = LoggerFactory.getLogger( CollectionsResourceIT.class );


    /***
     *
     * Test to make sure we get a 400 back when posting to a bad path
     *
     */
    @Test
    public void postToBadPath() throws IOException {

        String app = "fakeapp";
        String org = this.clientSetup.getOrganizationName();
        String entity = "fakeentity";
        //try to do a GET on a bad path
        try {
            this.clientSetup.getRestClient().org(org).app(app).collection("cities").get();
            fail("Call to bad path exists, but it should not");
        } catch (UniformInterfaceException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().getEntity( String.class ));
            assertEquals( "service_resource_not_found", node.get( "error" ).textValue() );
        }

        //try to do a POST on a bad path
        Entity payload = new Entity();
        payload.put("name", "Austin");
        payload.put("state", "TX");
        try {
            this.clientSetup.getRestClient().org(org).app(app).collection("cities").post(payload);
            fail("Call to bad path exists, but it should not");
        } catch (UniformInterfaceException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().getEntity( String.class ));
            assertEquals( "service_resource_not_found", node.get( "error" ).textValue() );
        }

        //try to do a PUT on a bad path
        try {
            this.clientSetup.getRestClient().org(org).app(app).collection("cities").entity(entity).put(payload);
            fail("Call to bad path exists, but it should not");
        } catch (UniformInterfaceException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().getEntity( String.class ));
            assertEquals( "service_resource_not_found", node.get( "error" ).textValue() );
        }

        //try to do a delete on a bad path
        try {
            this.clientSetup.getRestClient().org(org).app(app).collection("cities").entity(entity).delete();
            fail("Call to bad path exists, but it should not");
        } catch (UniformInterfaceException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().getEntity( String.class ));
            assertEquals( "service_resource_not_found", node.get( "error" ).textValue() );
        }

    }

    @Ignore("Not sure that this test makes any sense")
    @Test
    public void postToEmptyCollection() throws IOException {
/*
        Entity payload = new Entity();
        Entity entity = this.app().collection("cities").post(payload);
        assertNull(entity.get("name"));


        Map<String, String> payload = new HashMap<String, String>();

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/cities" ).queryParam( "access_token", access_token )
                        .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                        .post( String.class, payload ));
        assertNull( getEntity( node, 0 ) );
        assertNull( node.get( "count" ) );
*/
    }


    /**
     * Test posts with a user level token on a path with permissions
     */
    @Test
    public void permissionWithMeInString() throws Exception {

        // create user
        String username = "sumeet.agarwal@usergrid.com";
        String email = "sumeet.agarwal@usergrid.com";
        String password = "secret";
        String name = "Sumeet Agarwal";
        Entity payload = new Entity();
        payload.put("username", username);
        payload.put("email", email);
        payload.put("password", password);
        payload.put("name", name);
        Entity user = this.app().collection("users").post(payload);
        assertEquals(user.get("username"), username);
        assertEquals(user.get("email"), email);
        this.refreshIndex();

        //create a permission with the path "me" in it
        payload = new Entity();
        payload.put( "permission", "get,post,put,delete:/users/sumeet.agarwal@usergrid.com/**" );
        //POST to /users/sumeet.agarwal@usergrid.com/permissions
        Entity permission = this.app().collection("users").entity(user).collection("permissions").post(payload);
        assertEquals(permission.get("data"), "get,post,put,delete:/users/sumeet.agarwal@usergrid.com/**");

        //delete the default role, which would allow all authenticated requests
        this.app().collection("role").uniqueID("Default").delete();

        //log our new user in
        this.getAppUserToken(username, password);

        //now post data
        payload = new Entity();
        String profileName = "profile-sumeet";
        payload.put( "name", profileName );
        payload.put( "firstname", "sumeet" );
        payload.put( "lastname", "agarwal" );
        payload.put( "mobile", "122" );
        Entity nestProfile = this.app().collection("nestprofiles").post(payload);
        assertEquals(nestProfile.get("name"), profileName);

        this.refreshIndex();

        Entity nestprofileReturned = this.app().collection("nestprofiles").entity(nestProfile).get();
        assertEquals(nestprofileReturned.get("name"), name);

    }


    @Test
    public void stringWithSpaces() throws IOException {

        // create user
        String username = "sumeet.agarwal@usergrid.com";
        String email = "sumeet.agarwal@usergrid.com";
        String password = "secret";
        String name = "Sumeet Agarwal";
        Entity payload = new Entity();
        payload.put("username", username);
        payload.put("email", email);
        payload.put("password", password);
        payload.put("name", name);
        Entity user = this.app().collection("users").post(payload);
        assertEquals(user.get("username"), username);
        assertEquals(user.get("email"), email);
        this.refreshIndex();

        Map<String, String> payload = hashMap( "summaryOverview", "My Summary" ).map( "caltype", "personal" );

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/calendarlists" )
                .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));


        UUID id = getEntityId( node, 0 );

        //post a second entity


        payload = hashMap( "summaryOverview", "Your Summary" ).map( "caltype", "personal" );

        node = mapper.readTree( resource().path( "/test-organization/test-app/calendarlists" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                .post( String.class, payload ));


        refreshIndex("test-organization", "test-app");

        //query for the first entity

        String query = "summaryOverview = 'My Summary'";


        JsonNode queryResponse = mapper.readTree( resource().path( "/test-organization/test-app/calendarlists" )
                .queryParam( "access_token", access_token ).queryParam( "ql", query )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));


        UUID returnedId = getEntityId( queryResponse, 0 );

        assertEquals( id, returnedId );

        assertEquals( 1, queryResponse.get( "entities" ).size() );
    }


    /**
     * Test to verify "name property returns twice in AppServices response" is fixed.
     * https://apigeesc.atlassian.net/browse/USERGRID-2318
     */
    @Test
    public void testNoDuplicateFields() throws Exception {

        {
            // create an "app_user" object with name fred
            Map<String, String> payload = hashMap( "type", "app_user" ).map( "name", "fred" );

            JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/app_users" )
                    .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));

            String uuidString = node.get( "entities" ).get( 0 ).get( "uuid" ).asText();
            UUID entityId = UUIDUtils.tryGetUUID( uuidString );
            Assert.assertNotNull( entityId );
        }

        refreshIndex("test-organization", "test-app");

        {
            // check REST API response for duplicate name property
            // have to look at raw response data, Jackson will remove dups
            String s = resource().path( "/test-organization/test-app/app_users/fred" )
                    .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class );

            int firstFred = s.indexOf( "fred" );
            int secondFred = s.indexOf( "fred", firstFred + 4 );
            Assert.assertEquals( "Should not be more than one name property", -1, secondFred );
        }
    }
}
