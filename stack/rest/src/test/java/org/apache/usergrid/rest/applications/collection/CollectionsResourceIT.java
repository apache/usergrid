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
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.corepersistence.util.CpCollectionUtils;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.*;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import java.io.IOException;
import java.util.*;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;


/**
 * @author zznate
 * @author tnine
 * @author rockerston
 *
 *  misc tests for collections
 */

@NotThreadSafe
public class CollectionsResourceIT extends AbstractRestIT {

    private final String REGION_SETTING = "authoritativeRegion";

    private static final Logger log = LoggerFactory.getLogger( CollectionsResourceIT.class );

    @Test
    public void testPostValidSettings() throws IOException {

        String randomizer = RandomStringUtils.randomAlphanumeric(10);
        String collectionName = "collection_" + randomizer;

        //Create test collection with test entity that is full text indexed.
        Entity testEntity = new Entity();

        testEntity.put( "one","value" );
        app().collection( collectionName ).post( testEntity );

        //Creating schema.
        //this could be changed to a hashmap.
        ArrayList<String> indexingArray = new ArrayList<>(  );
        indexingArray.add( "one" );

        //field "fields" is required.
        Entity payload = new Entity();
        payload.put( "fields", indexingArray);
        payload.put( "queueIndex", "direct");

        //Post index to the collection metadata
        Entity settings = app().collection( collectionName ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();

        Object queueIndexValue = settings.get("queueIndex");
        assertEquals( "direct", queueIndexValue);

        Object feildsValue = settings.get("fields");
        assertTrue(feildsValue  instanceof  List);
        List list = (List) feildsValue;
        assertEquals(1, list.size()  );
        assertEquals( "one", list.get(0) );
    }


    /**
     * test that invalid settings are ignored
     * @throws IOException
     */
    @Test
    public void testPostInvalidSettings() throws IOException {

        String randomizer = RandomStringUtils.randomAlphanumeric(10);
        String collectionName = "collection" + randomizer;

        //Create test collection with test entity that is full text indexed.
        Entity testEntity = new Entity();

        testEntity.put( "one","value" );
        app().collection( collectionName ).post( testEntity );

        //Creating schema.
        //this could be changed to a hashmap.
        ArrayList<String> indexingArray = new ArrayList<>(  );
        indexingArray.add( "one" );

        //field "fields" is required.
        Entity payload = new Entity();
        payload.put( "queueIndex", "BADVALUE");

        //Post index to the collection metadata
        Entity settings = app().collection( collectionName ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();

        Object queueIndexValue = settings.get("queueIndex");
        // BAD value should be rejected and we get the default.
        assertEquals( "config", queueIndexValue);
    }



    /**
     * test that we can add new properties and modify existing ones.
     * @throws IOException
     */
    @Test
    public void testAddNewSettings() throws IOException {

        String randomizer = RandomStringUtils.randomAlphanumeric(10);
        String collectionName = "collection_" + randomizer;

        //Create test collection with test entity that is full text indexed.
        Entity testEntity = new Entity();

        testEntity.put( "one","value" );
        testEntity.put( "two","value" );
        app().collection( collectionName ).post( testEntity );

        //field "fields" is required.
        Entity payload = new Entity();
        payload.put( "fields", Collections.singletonList("one"));

        //Post index to the collection metadata
        app().collection( collectionName ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();

        // Now add a new setting 'queueIndex'

        payload = new Entity();
        payload.put( "queueIndex", "direct");

        Map<String, String> queryParams = Collections.singletonMap("replace_all","false");
        Entity settings = app().collection( collectionName ).collection( "_settings" ).post( payload , queryParams );
        waitForQueueDrainAndRefreshIndex();

        // assert that both old and new are in the setting.

        Object queueIndexValue = settings.get("queueIndex");
        assertEquals( "direct", queueIndexValue);

        Object feildsValue = settings.get("fields");
        assertNotNull(feildsValue);
        assertTrue(feildsValue  instanceof  List);
        List list = (List) feildsValue;
        assertEquals(1, list.size()  );
        assertEquals( "one", list.get(0) );

        // now replace the settings

        payload = new Entity();
        payload.put( "queueIndex", "async");
        payload.put( "fields", Collections.singletonList("two"));

        settings = app().collection( collectionName ).collection( "_settings" ).post( payload , queryParams );
        waitForQueueDrainAndRefreshIndex();

        // assert that they have new values

        queueIndexValue = settings.get("queueIndex");
        assertEquals( "async", queueIndexValue);

        feildsValue = settings.get("fields");
        assertNotNull(feildsValue);
        assertTrue(feildsValue  instanceof  List);
        list = (List) feildsValue;
        assertEquals(1, list.size()  );
        assertEquals( "two", list.get(0) );

    }



    @Test
    public void testStaleEntries() throws IOException {

        String randomizer = RandomStringUtils.randomAlphanumeric(10);
        String collectionName = "cars_" + randomizer;

        //Create test collection with test entity that is full text indexed.
        Entity testEntity1 = new Entity();
        testEntity1.put( "make","tesla" );
        testEntity1.put( "color","red" );
        testEntity1 = app().collection( collectionName ).post( testEntity1 );

        Entity testEntity2 = new Entity();
        testEntity2.put( "make","honda" );
        testEntity2.put( "color","red" );
        testEntity2 = app().collection( collectionName ).post( testEntity2 );

        ArrayList<String> indexingArray = new ArrayList<>(  );
        indexingArray.add( "make" );
        indexingArray.add( "color" );

        Entity payload = new Entity();
        payload.put( "fields", indexingArray);
        payload.put( "queueIndex", "direct");

        Entity settings = app().collection( collectionName ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();

        String query = "color ='red'";
        QueryParameters queryParameters = new QueryParameters().setQuery( query );

        Collection resultEntity = app().collection( collectionName ).get( queryParameters, true );

        assertNotNull(resultEntity);
        assertEquals(resultEntity.getResponse().getEntities().size(), 2);

        Object color0 = resultEntity.getResponse().getEntities().get(0).get("color");
        Object color1 = resultEntity.getResponse().getEntities().get(1).get("color");

        assertEquals("red", color0);
        assertEquals("red", color1);

        // Allow illegal settings
        CpCollectionUtils.setDebugMode(true);
        payload.put( "queueIndex", "debug_noindex");
        payload.put( "indexConsistency", "latest");
        settings = app().collection( collectionName ).collection( "_settings" ).post( payload );

        testEntity2.put("color", "blue");
        Entity response = app().collection( collectionName ).entity(testEntity2.getUuid()).put(testEntity2);

        queryParameters = new QueryParameters().setQuery( "color ='red'" );

        resultEntity = app().collection( collectionName ).get( queryParameters, true );

        waitForQueueDrainAndRefreshIndex();

        assertEquals(resultEntity.getResponse().getEntities().size(), 2);

        Entity entity0 = resultEntity.getResponse().getEntities().get(0);
        Entity entity1 = resultEntity.getResponse().getEntities().get(1);

        color0 = entity0.get("color");
        color1 = entity1.get("color");

        Object make0 = entity0.get("make");
        Object make1 = entity1.get("make");

        if (make0.equals("honda")) {
            assertEquals("blue", color0);
            assertEquals("red", color1);
        } else {
            assertEquals("red", color0);
            assertEquals("blue", color1);
        }

    }

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

    @Test
    public void postToCollectionSchemaUsingOrgAppCreds(){
        //Creating schema.
        //this could be changed to a hashmap.
        ArrayList<String> indexingArray = new ArrayList<>(  );


        //field "fields" is required.
        Entity payload = new Entity();
        payload.put( "fields", indexingArray);


        Credentials appCredentials = clientSetup.getAppCredentials();


        try {

            this.pathResource( getOrgAppPath( "testcollections/_settings" ) ).post( false, payload,
                new QueryParameters().addParam( "grant_type", "client_credentials" ).addParam( "client_id",
                    String.valueOf( ( ( Map ) appCredentials.get( "credentials" ) ).get( "client_id" ) ) )
                                     .addParam( "client_secret", String.valueOf(
                                         ( ( Map ) appCredentials.get( "credentials" ) ).get( "client_secret" ) ) ) );
        }catch(Exception e){
            fail("This should return a success.");
        }

        waitForQueueDrainAndRefreshIndex();


        Collection collection = this.app().collection( "testCollections" ).collection( "_settings" ).get();

        LinkedHashMap testCollectionSchema = (LinkedHashMap)collection.getResponse().getData();
        assertEquals( "app credentials",testCollectionSchema.get( "lastUpdateBy" ) );
        assertEquals( 0,testCollectionSchema.get( "lastReindexed" ) );
    }

    @Test
    public void deleteCollectionSchema() throws Exception {
        //Creating schema.
        //this could be changed to a hashmap.
        ArrayList<String> indexingArray = new ArrayList<>(  );


        //field "fields" is required.
        Entity payload = new Entity();
        payload.put( "fields", indexingArray);

        //Post index to the collection metadata
        Entity thing = this.app().collection( "testCollections" ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();


        //The above verifies the test case.


        //Create test collection with a test entity that is partially indexed.
        Entity testEntity = new Entity();
        testEntity.put( "one", "helper" );
        testEntity.put( "two","query" );

        //Post entity.
        Entity postedEntity = this.app().collection( "testCollections" ).post( testEntity );
        waitForQueueDrainAndRefreshIndex();

        //Do a query to see if you can find the indexed query.
        String query = "two ='query'";
        QueryParameters queryParameters = new QueryParameters().setQuery(query);

        //having a name breaks it. Need to get rid of the stack trace and also
        Collection tempEntity = this.app().collection( "testCollections" ).get(queryParameters,true);
        assertEquals( 0,tempEntity.getResponse().getEntities().size() );

        //Verify if you can query on an entity that was not indexed and that no entities are returned.
        query = "one = 'helper'";
        queryParameters = new QueryParameters().setQuery(query);
        tempEntity = this.app().collection( "testCollections" ).get(queryParameters,true);
        assertEquals(0,tempEntity.getResponse().getEntities().size());

        query = "uuid = "+postedEntity.getUuid().toString();
        queryParameters = new QueryParameters().setQuery(query);
        tempEntity = this.app().collection( "testCollections" ).get(queryParameters,true);
        assertEquals(1,tempEntity.getResponse().getEntities().size());

        //since the collection schema doesn't index anything it shouldn't show up except for the default properties
        //to prove that the entity exists

        //next part is to delete the schema then reindex it and it should work.
        this.app().collection( "testCollections" ).collection( "_settings" ).delete();
        waitForQueueDrainAndRefreshIndex();

        this.app().collection( "testCollections" ).collection( "_reindex" )
            .post(true,clientSetup.getSuperuserToken(),ApiResponse.class,null,null,false);
        waitForQueueDrainAndRefreshIndex();


        //Do a query to see if you can find the indexed query.
        query = "two ='query'";
        queryParameters = new QueryParameters().setQuery(query);

        //having a name breaks it. Need to get rid of the stack trace and also
        tempEntity = this.app().collection( "testCollections" ).get(queryParameters,true);
        assertEquals( 1,tempEntity.getResponse().getEntities().size() );

        //Verify if you can query on an entity that was not indexed and that no entities are returned.
        query = "one = 'helper'";
        queryParameters = new QueryParameters().setQuery(query);
        tempEntity = this.app().collection( "testCollections" ).get(queryParameters,true);
        assertEquals(1,tempEntity.getResponse().getEntities().size());

        query = "uuid = "+postedEntity.getUuid().toString();
        queryParameters = new QueryParameters().setQuery(query);
        tempEntity = this.app().collection( "testCollections" ).get(queryParameters,true);
        assertEquals(1,tempEntity.getResponse().getEntities().size());

    }

    @Test
    public void postCollectionSchemaWithWildcardIndexAll() throws Exception {

        // setup collection with index all
        Entity payload = new Entity();
        payload.put( "fields", "all");
        app().collection( "testCollection" ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();

        // post entity with two fields
        Entity testEntity = new Entity();
        testEntity.put( "one", "helper" );
        testEntity.put( "two","query" );
        app().collection( "testCollection" ).post( testEntity );
        waitForQueueDrainAndRefreshIndex();

        // verify it can be queried on both fields

        String query = "two ='query'";
        QueryParameters queryParameters = new QueryParameters().setQuery(query);
        Collection tempEntity = app().collection( "testCollection" ).get(queryParameters,true);
        Entity reindexedEntity = tempEntity.getResponse().getEntity();
        assertEquals( "helper",reindexedEntity.get( "one" ) );

        query = "one = 'helper'";
        queryParameters = new QueryParameters().setQuery(query);
        tempEntity = app().collection( "testCollection" ).get(queryParameters,true);
        assertEquals(1,tempEntity.getResponse().getEntities().size());
    }


    /**
     * Create test collection
     * Give collection an indexing schema
     * Give collection a new entity and ensure it only indexes wht is in the schema
     * Reindex and make sure old entity with full text indexing is reindexed with the schema.
     */
    @Test
    public void postToCollectionSchemaUpdateExistingCollection() throws Exception {

        //Create test collection with test entity that is full text indexed.
        Entity testEntity = new Entity();


        for(int i = 0; i < 10; i++){
            testEntity.put( "one","value"+i );
            testEntity.put( "two","valuetwo"+i );
            this.app().collection( "testCollection" ).post( testEntity );
        }

        //Creating schema.
        //this could be changed to a hashmap.
        ArrayList<String> indexingArray = new ArrayList<>(  );
        indexingArray.add( "one" );


        //field "fields" is required.
        Entity payload = new Entity();
        payload.put( "fields", indexingArray);
        payload.put( "queueIndex", "direct");

        //Post index to the collection metadata
        Entity thing = this.app().collection( "testCollection" ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();


        //Reindex and verify that the entity only has field one index.
        this.app().collection( "testCollection" ).collection( "_reindex" )
            .post(true,clientSetup.getSuperuserToken(),ApiResponse.class,null,null,false);

        for(int i = 0; i < 10; i++) {
            String query = "one ='value"+ i + "'";
            QueryParameters queryParameters = new QueryParameters().setQuery( query );

            //having a name breaks it. Need to get rid of the stack trace and also
            Collection tempEntity = this.app().collection( "testCollection" ).get( queryParameters, true );
            Entity reindexedEntity = tempEntity.getResponse().getEntity();
            assertEquals( "value"+i, reindexedEntity.get( "one" ) );

            //Verify if you can query on an entity that was not indexed and that no entities are returned.
            query = "two = 'valuetwo1"+ i + "'";
            queryParameters = new QueryParameters().setQuery( query );
            tempEntity = this.app().collection( "testCollection" ).get( queryParameters, true );
            assertEquals( 0, tempEntity.getResponse().getEntities().size() );
        }
    }


    @Test
    public void postToCollectionSchemaAndVerifyFieldsAreUpdated() throws Exception {

        //Create test collection with test entity that is full text indexed.
        Entity testEntity = new Entity();


        for(int i = 0; i < 10; i++){
            testEntity.put( "one","value"+i );
            testEntity.put( "two","valuetwo"+i );
            this.app().collection( "testCollection" ).post( testEntity );
        }


        //Creating schema.
        //this could be changed to a hashmap.
        ArrayList<String> indexingArray = new ArrayList<>(  );
        indexingArray.add( "one" );


        //field "fields" is required.
        Entity payload = new Entity();
        payload.put( "fields", indexingArray);

        //Post index to the collection metadata
        Entity thing = this.app().collection( "testCollection" ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();

        Collection collection = this.app().collection( "testCollection" ).collection( "_settings" ).get();

        LinkedHashMap testCollectionSchema = (LinkedHashMap)collection.getResponse().getData();
        assertEquals( ( thing ).get( "lastUpdated" ), testCollectionSchema.get( "lastUpdated" ));
        assertEquals( ( thing ).get( "lastUpdateBy" ),testCollectionSchema.get( "lastUpdateBy" ) );
        assertEquals( 0,testCollectionSchema.get( "lastReindexed" ) );

        ArrayList<String> schema = ( ArrayList<String> ) testCollectionSchema.get( "fields" );
        assertEquals( "one",schema.get( 0 ) );


        //Reindex and verify that the entity only has field one index.
        this.app().collection( "testCollection" ).collection( "_reindex" )
            .post(true,clientSetup.getSuperuserToken(),ApiResponse.class,null,null,false);


        indexingArray.add( "one" );
        indexingArray.add( "two" );


        //field "fields" is required.
        payload = new Entity();
        payload.put( "fields", indexingArray);

        //Post index to the collection metadata
        this.app().collection( "testCollection" ).collection( "_settings" ).post( payload );

        collection = this.app().collection( "testCollection" ).collection( "_settings" ).get();



        testCollectionSchema = (LinkedHashMap)collection.getResponse().getData();
        assertNotEquals( ( thing ).get( "lastUpdated" ), testCollectionSchema.get( "lastUpdated" ));
        assertEquals( ( thing ).get( "lastUpdateBy" ),testCollectionSchema.get( "lastUpdateBy" ) );
        assertNotEquals( 0,testCollectionSchema.get( "lastReindexed" ) );

        schema = ( ArrayList<String> ) testCollectionSchema.get( "fields" );
        assertEquals( "one",schema.get( 0 ) );


        for(int i = 0; i < 10; i++) {
            String query = "one ='value"+ i + "'";
            QueryParameters queryParameters = new QueryParameters().setQuery( query );

            //having a name breaks it. Need to get rid of the stack trace and also
            Collection tempEntity = this.app().collection( "testCollection" ).get( queryParameters, true );
            Entity reindexedEntity = tempEntity.getResponse().getEntity();
            assertEquals( "value"+i, reindexedEntity.get( "one" ) );

            //Verify if you can query on an entity that was not indexed and that no entities are returned.
            query = "two = 'valuetwo1"+ i + "'";
            queryParameters = new QueryParameters().setQuery( query );
            tempEntity = this.app().collection( "testCollection" ).get( queryParameters, true );
            assertEquals( 0, tempEntity.getResponse().getEntities().size() );
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
    @Test
    public void postToCollectionSchemaWithSchemaFirst() throws Exception {

        //Include the property labeled two to be index.
        ArrayList<String> indexingArray = new ArrayList<>(  );
        indexingArray.add( "two" );

        //field "fields" is required.
        Entity payload = new Entity();
        payload.put( "fields", indexingArray);

        //Post index to the collection metadata
        this.app().collection( "testCollection" ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();

        //Create test collection with a test entity that is partially indexed.
        Entity testEntity = new Entity();
        testEntity.put( "one", "helper" );
        testEntity.put( "two","query" );

        //Post entity.
        this.app().collection( "testCollection" ).post( testEntity );
        waitForQueueDrainAndRefreshIndex();

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
        this.app().collection( "testCollection" ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();

        Map<String,Object> arrayFieldsForTesting = new HashMap<>();

        arrayFieldsForTesting.put( "key","value" );
        arrayFieldsForTesting.put( "anotherKey","value2");

        //Create test collection with a test entity that is partially indexed.
        Entity testEntity = new Entity();
        testEntity.put( "one", arrayFieldsForTesting );
        testEntity.put( "two","query" );

        //Post entity.
        this.app().collection( "testCollection" ).post( testEntity );
        waitForQueueDrainAndRefreshIndex();

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
        this.app().collection( "testCollection" ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();

        Map<String,Object> arrayFieldsForTesting = new HashMap<>();

        arrayFieldsForTesting.put( "key","value" );
        arrayFieldsForTesting.put( "anotherKey","value2");

        //Create test collection with a test entity that is partially indexed.
        Entity testEntity = new Entity();
        testEntity.put( "one", arrayFieldsForTesting );
        testEntity.put( "two","query" );

        //Post entity.
        this.app().collection( "testCollection" ).post( testEntity );
        waitForQueueDrainAndRefreshIndex();

        //Do a query to see if you can find the indexed query.
        String query = "one.key = 'value'";
        QueryParameters queryParameters = new QueryParameters().setQuery(query);

        //having a name breaks it. Need to get rid of the stack trace and also
        Collection tempEntity = this.app().collection( "testCollection" ).get(queryParameters,true);
        Entity reindexedEntity = tempEntity.getResponse().getEntity();
        assertEquals( "value2",((Map)reindexedEntity.get( "one" )).get( "anotherKey" ) );


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
        this.app().collection( "testCollection" ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();

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
        waitForQueueDrainAndRefreshIndex();

        //Do a query to see if you can find the indexed query.
        String query = "one.key.wowMoreKeys = 'value'";
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


    @Test
    public void postToCollectionSchemaArrayWithSelectiveTopLevelIndexingAddingDefaultPropertyNames() throws Exception {

        //Include the property labeled two to be index.
        ArrayList<String> indexingArray = new ArrayList<>(  );
        indexingArray.add( "two" );
        //this should work such that one.key and one.anotherKey should work.
        indexingArray.add( "one.key" );
        indexingArray.add( "name" );

        //field "fields" is required.
        Entity payload = new Entity();
        payload.put( "fields", indexingArray);

        //Post index to the collection metadata
        this.app().collection( "testCollection" ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();

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
        testEntity.put( "name","howdy");

        //Post entity.
        this.app().collection( "testCollection" ).post( testEntity );
        waitForQueueDrainAndRefreshIndex();

        //Do a query to see if you can find the indexed query.
        String query = "name = 'howdy'";
        QueryParameters queryParameters = new QueryParameters().setQuery(query);

        //having a name breaks it. Need to get rid of the stack trace and also
        Collection tempEntity = this.app().collection( "testCollection" ).get(queryParameters,true);
        Entity reindexedEntity = tempEntity.getResponse().getEntity();
        assertEquals( "howdy",(reindexedEntity.get( "name" )) );

    }


    /**
     * Verify that doing a put goes through and still applies the existing schema.
     * @throws Exception
     */
    @Test
    public void putToCollectionSchema() throws Exception {

        //Include the property labeled two to be index.
        ArrayList<String> indexingArray = new ArrayList<>(  );
        indexingArray.add( "one" );

        //field "fields" is required.
        Entity payload = new Entity();
        payload.put( "fields", indexingArray);

        //Post index to the collection metadata
        this.app().collection( "testCollection" ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();

        //Create test collection with a test entity that is partially indexed.
        Entity testEntity = new Entity();
        testEntity.put( "one", "two");
        testEntity.put( "test", "anotherTest");

        //Post entity.
        Entity postedEntity = this.app().collection( "testCollection" ).post( testEntity );
        waitForQueueDrainAndRefreshIndex();

        testEntity.put( "one","three" );
        this.app().collection( "testCollection" ).entity( postedEntity.getUuid() ).put( testEntity );
        waitForQueueDrainAndRefreshIndex();

        //Do a query to see if you can find the indexed query.
        String query = "one = 'three'";
        QueryParameters queryParameters = new QueryParameters().setQuery(query);

        //having a name breaks it. Need to get rid of the stack trace and also
        Collection tempEntity = this.app().collection( "testCollection" ).get(queryParameters,true);
        Entity reindexedEntity = tempEntity.getResponse().getEntity();
        assertEquals( "three",(reindexedEntity.get( "one" )) );

        //check that the old value no longer exists in elasticsearch
        query = "one = 'two'";
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
        this.waitForQueueDrainAndRefreshIndex();

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

        this.waitForQueueDrainAndRefreshIndex();

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

        this.waitForQueueDrainAndRefreshIndex();

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

        assertNotSame( null,
            ((LinkedHashMap)(collectionHashMap.get( "collections" ))).get( collectionName.toLowerCase() ));

        this.waitForQueueDrainAndRefreshIndex();
        this.app().collection( collectionName ).entity( testEntity.getEntity().getUuid() ).delete();
        this.waitForQueueDrainAndRefreshIndex();


        //Verify that the collection still exists despite deleting its only entity.)
        usersDefaultCollection = this.app().get();

        collectionHashMap = ( LinkedHashMap ) usersDefaultCollection.getEntity().get( "metadata" );

        assertNotSame( null,
            ((LinkedHashMap)(collectionHashMap.get( "collections" ))).get( collectionName.toLowerCase() ));

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
        this.waitForQueueDrainAndRefreshIndex();

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


    /**
     * Test that when schema is "none" entity gets saved but does not get indexed
     */
    @Test
    public void postCollectionSchemaWithWildcardIndexNone() throws Exception {

        Entity payload = new Entity();
        payload.put( "fields", "none");

        String randomizer = RandomStringUtils.randomAlphanumeric(10);
        String collectionName = "col_" + randomizer;
        app().collection( collectionName ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();

        // was the no-index wildcard saved and others ignored?
        Collection collection = app().collection( collectionName ).collection( "_settings" ).get();
        LinkedHashMap testCollectionSchema = (LinkedHashMap)collection.getResponse().getData();
        String schema = (String)testCollectionSchema.get( "fields" );
        assertEquals( "none", schema );

        // post an entity with a name and a color
        String entityName = "name_" + randomizer;
        Entity postedEntity = this.app().collection( collectionName )
            .post( new Entity().withProp("name", entityName).withProp( "color", "magenta" ) );

        // should be able to get entity by ID
        Entity getByIdEntity = app().collection( collectionName ).entity( postedEntity.getUuid() ).get();
        assertNotNull( getByIdEntity );
        assertEquals( postedEntity.getUuid(), getByIdEntity.getUuid() );

        // should be able to get entity by name
        Entity getByNameEntity = app().collection( collectionName ).entity( entityName ).get();
        assertNotNull( getByNameEntity );
        assertEquals( postedEntity.getUuid(), getByNameEntity.getUuid() );

        // should NOT be able to get entity by query
        Iterator<Entity> getByQuery = app().collection( collectionName )
            .get( new QueryParameters().setQuery( "select * where color='magenta'" ) ).iterator();
        assertFalse( getByQuery.hasNext() );
    }

    /**
     * Test that indexed entities can be connected to un-indexed Entities and connections still work.
     */
    @Test
    public void testIndexedEntityToUnindexedEntityConnections() {

        // create entities in an un-indexed collection

        Entity payload = new Entity();
        payload.put( "fields", "none");

        String randomizer = RandomStringUtils.randomAlphanumeric(10);
        String unIndexedCollectionName = "col_" + randomizer;
        app().collection( unIndexedCollectionName ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();

        String entityName1 = "unindexed1";
        Entity unindexed1 = this.app().collection( unIndexedCollectionName )
            .post( new Entity().withProp("name", entityName1).withProp( "color", "violet" ) );

        String entityName2 = "unindexed2";
        Entity unindexed2 = this.app().collection( unIndexedCollectionName )
            .post( new Entity().withProp("name", entityName2).withProp( "color", "violet" ) );

        // create an indexed entity

        String indexedCollection = "col_" + randomizer;
        String indexedEntityName = "indexedEntity";
        Entity indexedEntity = this.app().collection( indexedCollection )
            .post( new Entity().withProp("name", indexedEntityName).withProp( "color", "orange" ) );

        // create connections from indexed entity to un-indexed entities

        app().collection(indexedCollection).entity(indexedEntity).connection("likes").entity(unindexed1).post();
        app().collection(indexedCollection).entity(indexedEntity).connection("likes").entity(unindexed2).post();

        Collection connectionsByGraph = app().collection( indexedCollection )
            .entity(indexedEntity).connection( "likes" ).get();
        assertEquals( 2, connectionsByGraph.getNumOfEntities() );

        Collection connectionsByQuery = app().collection( indexedCollection )
            .entity(indexedEntity).connection( "likes" )
            .get( new QueryParameters().setQuery( "select * where color='violet'" ));
        assertEquals( 0, connectionsByQuery.getNumOfEntities() );

    }


    /**
     * Test that index entities can be connected to un-indexed Entities and connections still work.
     */
    @Test
    public void testUnindexedEntityToIndexedEntityConnections() {

        // create two entities in an indexed collection

        String randomizer = RandomStringUtils.randomAlphanumeric(10);
        String indexedCollection = "col_" + randomizer;
        String indexedEntityName = "indexedEntity";

        Entity indexedEntity1 = this.app().collection( indexedCollection )
            .post( new Entity().withProp("name", indexedEntityName + "_1").withProp( "color", "orange" ) );

        Entity indexedEntity2 = this.app().collection( indexedCollection )
            .post( new Entity().withProp("name", indexedEntityName + "_2").withProp( "color", "orange" ) );

        // create an un-indexed entity

        Entity payload = new Entity();
        payload.put( "fields", "none");

        String unIndexedCollectionName = "col_" + randomizer;
        app().collection( unIndexedCollectionName ).collection( "_settings" ).post( payload );
        waitForQueueDrainAndRefreshIndex();

        String entityName1 = "unindexed1";
        Entity unindexed1 = this.app().collection( unIndexedCollectionName )
            .post( new Entity().withProp("name", entityName1).withProp( "color", "violet" ) );

        // create connections from un-indexed entity to indexed entities

        app().collection(unIndexedCollectionName).entity(unindexed1).connection("likes").entity(indexedEntity1).post();
        app().collection(unIndexedCollectionName).entity(unindexed1).connection("likes").entity(indexedEntity2).post();

        // should be able to get connections via graph from un-indexed to index

        Collection connectionsByGraph = app().collection( indexedCollection )
            .entity(unindexed1).connection( "likes" ).get();
        assertEquals( 2, connectionsByGraph.getNumOfEntities() );

        // should not be able to get connections via query from unindexed to indexed

        Collection connectionsByQuery = app().collection( indexedCollection )
            .entity(unindexed1).connection( "likes" )
            .get( new QueryParameters().setQuery( "select * where color='orange'" ));
        assertEquals( 0, connectionsByQuery.getNumOfEntities() );
    }


    @Test
    public void testCollectionAuthoritativeRegion() {

        // create collection with settings for index all

        String randomizer = RandomStringUtils.randomAlphanumeric(10);
        String collectionName = "col_" + randomizer;

        app().collection( collectionName ).collection( "_settings" )
            .post( new Entity().chainPut( "fields", "all" ) );
        waitForQueueDrainAndRefreshIndex();

        // get collection settings, should see no region

        Collection collection = app().collection( collectionName ).collection( "_settings" ).get();
        Map<String, Object> settings = (Map<String, Object>)collection.getResponse().getData();
        assertNull( settings.get( REGION_SETTING ));

        // set collection region with bad region, expect error

        try {
            app().collection( collectionName ).collection( "_settings" )
                .post( new Entity().chainPut(REGION_SETTING, "us-moon" ) );
            fail( "post should have failed");

        } catch ( BadRequestException expected ) {}

        // set collection region with good region

        app().collection( collectionName ).collection( "_settings" )
            .post( new Entity().chainPut( REGION_SETTING, "us-east" ) );

        // get collection settings see that we have a region

        collection = app().collection( collectionName ).collection( "_settings" ).get();
        settings = (Map<String, Object>)collection.getResponse().getData();
        assertNotNull( settings.get( REGION_SETTING ));
        assertEquals( "us-east", settings.get( REGION_SETTING ));

        // unset the collection region

        app().collection( collectionName ).collection( "_settings" )
            .post( new Entity().chainPut( REGION_SETTING, "" ) );
        waitForQueueDrainAndRefreshIndex();

        // get collection settings, should see no region

        collection = app().collection( collectionName ).collection( "_settings" ).get();
        settings = (Map<String, Object>)collection.getResponse().getData();
        assertNull( settings.get( REGION_SETTING ));


    }


    @Test
    public void testBeingAbleToRetreiveMigratedValues() throws Exception {


        Entity notifier = new Entity().chainPut("name", "mynotifier").chainPut("provider", "noop");

        ApiResponse notifierNode = this.pathResource(getOrgAppPath("notifier")).post(ApiResponse.class,notifier);

        // create user

        Map payloads = new HashMap<>(  );
        payloads.put( "mynotifier","hello world" );

        Map statistics = new HashMap<>(  );
        statistics.put( "sent",1 );
        statistics.put( "errors",2 );

        Entity payload = new Entity();
        payload.put("debug", false);
        payload.put( "expectedCount",0 );
        payload.put( "finished",1438279671229L);
        payload.put( "payloads",payloads);
        payload.put( "priority","normal");
        payload.put( "state","FINISHED");
        payload.put( "statistics",statistics);


        this.app().collection("notifications/"+ UUIDUtils.newTimeUUID()).post(payload );
        this.waitForQueueDrainAndRefreshIndex();

        Collection user2 = this.app().collection("notifications").get();

        assertEquals(1,user2.getNumOfEntities());

        this.app().collection("notifications/"+ UUIDUtils.newTimeUUID()).put(null,payload );
        this.waitForQueueDrainAndRefreshIndex();

        user2 = this.app().collection("notifications").get();

        assertEquals(2,user2.getNumOfEntities());

    }

}
