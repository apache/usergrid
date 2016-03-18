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

import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.entities.Application;

import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import static org.junit.Assert.*;


/**
 * @author zznate
 * @author tnine
 * @author rockerston
 *
 *  misc tests for collections
 */

public class CollectionsResourceIT extends AbstractRestIT {

    private static final Logger log = LoggerFactory.getLogger( CollectionsResourceIT.class );


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


    /**
     * Create test collection
     * Give collection an indexing schema
     * Give collection a new entity and ensure it only indexes wht is in the schema
     * Reindex and make sure old entity with full text indexing is reindexed with the schema.
     *
     * @throws Exception
     */
    @Ignore("The reindexing isn't currently supported yet.")
    public void postToCollectionSchemaUpdateExistingCollection() throws Exception {

        //Create test collection with test entity that is full text indexed.
        Entity testEntity = new Entity();
        testEntity.put( "one", "12/31/9999" );
        //this field shouldn't persist after reindexing.
        testEntity.put( "two","2015-04-20T17:41:38.035Z" );

        //TODO: add arrays to the indexing test
        //testEntity.put("array","array stuff here");

        Entity returnedEntity = this.app().collection( "testCollection" ).post( testEntity );

        //Creating schema.
        //this could be changed to a hashmap.
        ArrayList<String> indexingArray = new ArrayList<>(  );
        indexingArray.add( "one" );

        //TODO: add indexing array to the backend/test once you finish the regular selective indexing.
        //indexingArray.add( "field.three.index.array" );

        //field "fields" is required.
        Entity payload = new Entity();
        payload.put( "fields", indexingArray);

        //Post index to the collection metadata
        Entity thing = this.app().collection( "testCollection" ).collection( "_indexes" ).post( payload );
        refreshIndex();

        //Below is what needs to be implemented along with the index call above

        //Get the collection schema and verify that it contains the same schema as posted above.
        Collection collection = this.app().collection( "testCollection" ).collection( "_index" ).get();

        LinkedHashMap testCollectionSchema = (LinkedHashMap)collection.getResponse().getData();
        //TODO: the below will have to be replaced by the values that I deem correct.
        assertEquals( ( thing ).get( "lastUpdated" ), testCollectionSchema.get( "lastUpdated" ));
        assertEquals( ( thing ).get( "lastUpdateBy" ),testCollectionSchema.get( "lastUpdateBy" ) );
        assertEquals( ( thing ).get( "lastReindexed" ),testCollectionSchema.get( "lastReindexed" ) );

        //TODO: this test doesn't check to see if create checks the schema. Only that the reindex removes whats already there.
        ArrayList<String> schema = ( ArrayList<String> ) testCollectionSchema.get( "fields" );
        assertEquals( "one",schema.get( 0 ) );

        //Reindex and verify that the entity only has field one index.
        this.app().collection( "testCollection" ).collection( "_reindex" ).post();

        refreshIndex();

        Entity reindexedEntity = this.app().collection( "testCollection" ).entity( returnedEntity.getUuid() ).get();
        assertEquals( "12/31/9999",reindexedEntity.get( "one" ) );
        assertNull( reindexedEntity.get( "two" ) );
        //not sure if this should have some kind of sleep here because this reindex will be heavily throttled.

    }

    /**
     * Create test collection
     * Give collection an indexing schema
     * Give collection a new entity and ensure it only indexes wht is in the schema
     * Reindex and make sure old entity with full text indexing is reindexed with the schema.
     *
     * @throws Exception
     */
    @Test
    public void postToCollectionSchemaWithSchemaFirst() throws Exception {

        //Include the property labeled two to be index.
        ArrayList<String> indexingArray = new ArrayList<>(  );
        indexingArray.add( "two" );

        //field "fields" is required.
        Entity payload = new Entity();
        payload.put( "fields", indexingArray);

        //Post index to the collection metadata
        this.app().collection( "testCollection" ).collection( "_indexes" ).post( payload );
        refreshIndex();

        //Create test collection with a test entity that is partially indexed.
        Entity testEntity = new Entity();
        testEntity.put( "one", "helper" );
        testEntity.put( "two","query" );

        //Post entity.
        this.app().collection( "testCollection" ).post( testEntity );
        refreshIndex();

        //Do a query to see if you can find the indexed query.
        String query = "two ='query'";
        QueryParameters queryParameters = new QueryParameters().setQuery(query);

        //having a name breaks it. Need to get rid of the stack trace and also
        Collection tempEntity = this.app().collection( "testCollection" ).get(queryParameters,true);
        Entity reindexedEntity = tempEntity.getResponse().getEntity();
        assertEquals( "helper",reindexedEntity.get( "one" ) );

        //Verify if you can query on an entity that was not indexed and that no entities are returned.
        query = "one = 'helper'";
        queryParameters = new QueryParameters().setQuery(query);
        tempEntity = this.app().collection( "testCollection" ).get(queryParameters,true);
        assertEquals(0,tempEntity.getResponse().getEntities().size());

    }

    @Test
    public void postToCollectionArraySchemaWithSchemaFirst() throws Exception {

        //Include the property labeled two to be index.
        ArrayList<String> indexingArray = new ArrayList<>(  );
        indexingArray.add( "two" );
        indexingArray.add( "one.key" );

        //field "fields" is required.
        Entity payload = new Entity();
        payload.put( "fields", indexingArray);

        //Post index to the collection metadata
        this.app().collection( "testCollection" ).collection( "_indexes" ).post( payload );
        refreshIndex();

        Map<String,Object> arrayFieldsForTesting = new HashMap<>();

        arrayFieldsForTesting.put( "key","value" );
        arrayFieldsForTesting.put( "anotherKey","value2");

        //Create test collection with a test entity that is partially indexed.
        Entity testEntity = new Entity();
        testEntity.put( "one", arrayFieldsForTesting );
        testEntity.put( "two","query" );

        //Post entity.
        this.app().collection( "testCollection" ).post( testEntity );
        refreshIndex();

        //Do a query to see if you can find the indexed query.
        String query = "one.key = 'value'";
        QueryParameters queryParameters = new QueryParameters().setQuery(query);

        //having a name breaks it. Need to get rid of the stack trace and also
        Collection tempEntity = this.app().collection( "testCollection" ).get(queryParameters,true);
        Entity reindexedEntity = tempEntity.getResponse().getEntity();
        assertEquals( "value2",((Map)reindexedEntity.get( "one" )).get( "anotherKey" ) );

        //Verify if you can query on an entity that was not indexed and that no entities are returned.
        query = "one.anotherKey = 'value2'";
        queryParameters = new QueryParameters().setQuery(query);
        tempEntity = this.app().collection( "testCollection" ).get(queryParameters,true);
        assertEquals(0,tempEntity.getResponse().getEntities().size());

    }

    //test to reflect if one would index whatever is prefixed with that. Will
    //need to do extensive testing to ensure that it will work with different instances.
    @Test
    public void postToCollectionSchemaArrayWithTopLevelIndexing() throws Exception {

        //Include the property labeled two to be index.
        ArrayList<String> indexingArray = new ArrayList<>(  );
        indexingArray.add( "two" );
        //this should work such that one.key and one.anotherKey should work.
        indexingArray.add( "one" );

        //field "fields" is required.
        Entity payload = new Entity();
        payload.put( "fields", indexingArray);

        //Post index to the collection metadata
        this.app().collection( "testCollection" ).collection( "_indexes" ).post( payload );
        refreshIndex();

        Map<String,Object> arrayFieldsForTesting = new HashMap<>();

        arrayFieldsForTesting.put( "key","value" );
        arrayFieldsForTesting.put( "anotherKey","value2");

        //Create test collection with a test entity that is partially indexed.
        Entity testEntity = new Entity();
        testEntity.put( "one", arrayFieldsForTesting );
        testEntity.put( "two","query" );

        //Post entity.
        this.app().collection( "testCollection" ).post( testEntity );
        refreshIndex();

        //Do a query to see if you can find the indexed query.
        String query = "one.key = 'value'";
        QueryParameters queryParameters = new QueryParameters().setQuery(query);

        //having a name breaks it. Need to get rid of the stack trace and also
        Collection tempEntity = this.app().collection( "testCollection" ).get(queryParameters,true);
        Entity reindexedEntity = tempEntity.getResponse().getEntity();
        assertEquals( "value2",((Map)reindexedEntity.get( "one" )).get( "anotherKey" ) );

        //Verify if you can query on an entity that was not indexed and that no entities are returned.
        //TODO: check that the below gets indexed as well. although the above should prove that at least one thing is getting indexed.
//        query = "one.anotherKey = 'value2'";
//        queryParameters = new QueryParameters().setQuery(query);
//        tempEntity = this.app().collection( "testCollection" ).get(queryParameters,true);
//        assertEquals(0,tempEntity.getResponse().getEntities().size());

    }

    @Test
    public void postToCollectionSchemaArrayWithSelectiveTopLevelIndexing() throws Exception {

        //Include the property labeled two to be index.
        ArrayList<String> indexingArray = new ArrayList<>(  );
        indexingArray.add( "two" );
        //this should work such that one.key and one.anotherKey should work.
        indexingArray.add( "one.key" );

        //field "fields" is required.
        Entity payload = new Entity();
        payload.put( "fields", indexingArray);

        //Post index to the collection metadata
        this.app().collection( "testCollection" ).collection( "_indexes" ).post( payload );
        refreshIndex();

        Map<String,Object> arrayFieldsForTestingSelectiveIndexing = new HashMap<>();

        arrayFieldsForTestingSelectiveIndexing.put( "wowMoreKeys","value" );
        arrayFieldsForTestingSelectiveIndexing.put( "thisShouldBeQueryableToo","value2");

        Map<String,Object> arrayFieldsForTesting = new HashMap<>();

        arrayFieldsForTesting.put( "key",arrayFieldsForTestingSelectiveIndexing );
        arrayFieldsForTesting.put( "anotherKey","value2");

        //Create test collection with a test entity that is partially indexed.
        Entity testEntity = new Entity();
        testEntity.put( "one", arrayFieldsForTesting );
        testEntity.put( "two","query" );

        //Post entity.
        this.app().collection( "testCollection" ).post( testEntity );
        refreshIndex();

        //Do a query to see if you can find the indexed query.
        String query = "one.key.wowMoreKeys = 'value'";
        QueryParameters queryParameters = new QueryParameters().setQuery(query);

        //having a name breaks it. Need to get rid of the stack trace and also
        Collection tempEntity = this.app().collection( "testCollection" ).get(queryParameters,true);
        Entity reindexedEntity = tempEntity.getResponse().getEntity();
        assertEquals( "value2",((Map)reindexedEntity.get( "one" )).get( "anotherKey" ) );

        //Verify if you can query on an entity that was not indexed and that no entities are returned.
        //TODO: check that the below gets indexed as well. although the above should prove that at least one thing is getting indexed.
        query = "one.anotherKey = 'value2'";
        queryParameters = new QueryParameters().setQuery(query);
        tempEntity = this.app().collection( "testCollection" ).get(queryParameters,true);
        assertEquals(0,tempEntity.getResponse().getEntities().size());

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

    @Test
    public void testDefaultCollectionReturning() throws IOException {

        ApiResponse usersDefaultCollection = this.app().get();

        LinkedHashMap collectionHashMap = ( LinkedHashMap ) usersDefaultCollection.getEntity().get( "metadata" );

        //make sure you have all the other default collections once you have users in place.
        Set<String> system_collections = Schema.getDefaultSchema().getCollectionNames( Application.ENTITY_TYPE );
        for(String collectionName : system_collections){
            assertNotSame( null,((LinkedHashMap)(collectionHashMap.get( "collections" ))).get( collectionName ));
        }
    }

    @Ignore("Ignored because we no longer retain custom collections after deleting the last entity in a collection"
        + "This test can be used to verify that works when we implement it")
    @Test
    public void testNewlyCreatedCollectionReturnsWhenEmpty(){
        String collectionName =  "testDefaultCollectionReturnings";

        Map<String,Object> payload = new HashMap(  );
        payload.put( "hello","test" );
        ApiResponse testEntity = this.app().collection( collectionName ).post( payload );

        //Verify that the below collection actually does exist.
        ApiResponse usersDefaultCollection = this.app().get();

        LinkedHashMap collectionHashMap = ( LinkedHashMap ) usersDefaultCollection.getEntity().get( "metadata" );

        assertNotSame( null,((LinkedHashMap)(collectionHashMap.get( "collections" ))).get( collectionName.toLowerCase() ));

        this.refreshIndex();
        this.app().collection( collectionName ).entity( testEntity.getEntity().getUuid() ).delete();
        this.refreshIndex();


        //Verify that the collection still exists despite deleting its only entity.)
        usersDefaultCollection = this.app().get();

        collectionHashMap = ( LinkedHashMap ) usersDefaultCollection.getEntity().get( "metadata" );

        assertNotSame( null,((LinkedHashMap)(collectionHashMap.get( "collections" ))).get( collectionName.toLowerCase() ));

        Collection createdCollectionResponse = this.app().collection( collectionName ).get();

        assertEquals( 0,createdCollectionResponse.getNumOfEntities() );
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
