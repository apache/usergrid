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


import com.fasterxml.jackson.databind.JsonNode;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ClientErrorException;
import java.io.IOException;

import static org.junit.Assert.*;


/**
 * @author zznate
 * @author tnine
 * @author rockerston
 *
 *  misc tests for collections
 */

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
        } catch (ClientErrorException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().readEntity( String.class ));
            assertEquals( "organization_application_not_found", node.get( "error" ).textValue() );
        }

        //try to do a POST on a bad path
        Entity payload = new Entity();
        payload.put("name", "Austin");
        payload.put("state", "TX");
        try {
            this.clientSetup.getRestClient().org(org).app(app).collection("cities").post(payload);
            fail("Call to bad path exists, but it should not");
        } catch (ClientErrorException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().readEntity( String.class ));
            assertEquals( "organization_application_not_found", node.get( "error" ).textValue() );
        }

        //try to do a PUT on a bad path
        try {
            this.clientSetup.getRestClient().org(org).app(app).collection("cities").entity(entity).put(payload);
            fail("Call to bad path exists, but it should not");
        } catch (ClientErrorException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().readEntity( String.class ));
            assertEquals( "organization_application_not_found", node.get( "error" ).textValue() );
        }

        //try to do a delete on a bad path
        try {
            this.clientSetup.getRestClient().org(org).app(app).collection("cities").entity(entity).delete();
            fail("Call to bad path exists, but it should not");
        } catch (ClientErrorException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().readEntity( String.class ));
            assertEquals( "organization_application_not_found", node.get( "error" ).textValue() );
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

    @Test
    public void postToReservedField() throws Exception {
        Entity payload = new Entity();
        payload.put( "term_date", "12/31/9999" );
        payload.put( "effective_date","2015-04-20T17:41:38.035Z" );
        payload.put("junk","TEST");

        this.app().collection( "testCollection" ).post( payload );
        refreshIndex();
        Thread.sleep( 1000 );

        Collection collection = this.app().collection( "testCollection" ).get();

        assertNotEquals(0, collection.getNumOfEntities() );

        payload = new Entity();
        payload.put( "term_date","1991-17-10" );
        payload.put( "effective_date","HELLO WORLD!" );
        payload.put("junk","TEST");

        this.app().collection( "testCollection" ).post( payload );
        refreshIndex();
        Thread.sleep( 1000 );

        collection = this.app().collection( "testCollection" ).get();

        assertEquals( 2, collection.getNumOfEntities() );

    }

    /**
     * Test posts with a user level token on a path with permissions
     */
    //TODO: App level permissions aren't functioning.
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

        String collectionName = "nestprofiles";
        //create a permission with the path "me" in it
        payload = new Entity();
        payload.put( "permission", "get,post,put,delete:/"+collectionName+"/**" );
        //POST to /users/sumeet.agarwal@usergrid.com/permissions
        Entity permission = this.app().collection("users").entity(user).collection("permissions").post(payload);
        assertEquals(permission.get("data"), "get,post,put,delete:/"+collectionName+"/**");

        //delete the default role, which would allow all authenticated requests
        this.app().collection("role").uniqueID("Default").delete();

        //log our new user in
        //TODO:App Level token is broken it seems. Test won't work with it.
        Token appToken = this.getAppUserToken(username, password);
        management().token().setToken( appToken );

        //now post data
        payload = new Entity();
        String profileName = "profile-sumeet";
        payload.put( "name", profileName );
        payload.put( "firstname", "sumeet" );
        payload.put( "lastname", "agarwal" );
        payload.put( "mobile", "122" );
        Entity nestProfile = this.app().collection(collectionName).post(payload);
        assertEquals(nestProfile.get("name"), profileName);

        this.refreshIndex();

        Entity nestprofileReturned = this.app().collection(collectionName).entity(nestProfile).get();
        assertEquals(nestprofileReturned.get("name"), profileName);

    }


    @Test
    public void stringWithSpaces() throws IOException {

        // create entity with a property with spaces
        String collection = "calendarlists";
        String summaryOverview = "My Summary";
        String calType = "personal";
        Entity payload = new Entity();
        payload.put("summaryOverview", summaryOverview);
        payload.put("caltype", calType);

        Entity calendarlistOne = this.app().collection(collection).post(payload );
        assertEquals( calendarlistOne.get( "summaryOverview" ), summaryOverview );
        assertEquals(calendarlistOne.get("caltype"), calType);

        this.refreshIndex();

        //post a second entity
        payload = new Entity();
        String summaryOverviewTwo = "Your Summary";
        String calTypeTwo = "personal";
        payload.put("summaryOverview", summaryOverviewTwo);
        payload.put("caltype", calTypeTwo);
        Entity calendarlistTwo = this.app().collection(collection).post(payload);
        assertEquals( calendarlistTwo.get( "summaryOverview" ), summaryOverviewTwo );
        assertEquals(calendarlistTwo.get("caltype"), calTypeTwo);


        //query for the first entity
        String query = "summaryOverview = 'My Summary'";
        QueryParameters queryParameters = new QueryParameters().setQuery(query);
        Collection calendarListCollection = this.app().collection(collection).get(queryParameters);
        assertEquals(calendarListCollection.hasNext(), true);

    }


    /**
     * Test to verify "name property returns twice in AppServices response" is fixed.
     */
    @Test
    public void testNoDuplicateFields() throws Exception {

        // create user
        String name = "fred";
        Entity payload = new Entity();
        payload.put("name", name);
        Entity user = this.app().collection("app_users").post(payload);
        assertEquals(user.get("name"), name);
        this.refreshIndex();

        Entity user2 = this.app().collection("app_users").entity(user).get();

/*
        // check REST API response for duplicate name property
        // have to look at raw response data, Jackson will remove dups
        String s = resource().path( "/test-organization/test-app/app_users/fred" )
                .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class );

        int firstFred = s.indexOf( "fred" );
        int secondFred = s.indexOf( "fred", firstFred + 4 );
        Assert.assertEquals( "Should not be more than one name property", -1, secondFred );
  */
    }
}
