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
package org.apache.usergrid.rest.applications.collection.paging;


import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.services.ServiceParameter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

//TODO: combine with PagingEntitiesTest
/**
 * Tests paging with respect to entities. Also tests cursors and queries with respect to paging.
 */
@Concurrent()
public class PagingResourceIT extends AbstractRestIT {

    /**
     * Creates 40 objects and then retrieves all 40 by paging through sets of 10 with a cursor.
     * @throws Exception
     */
    @Test
    public void collectionPaging() throws Exception {

        String collectionName = "testCollectionPaging" ;

        int size = 40;
        int numOfPages = 4;

        //Creates 40 entities by posting entities with names 0 - size in order.
        createEntities( collectionName, size );

        //Pages through 4 pages of entities and verifies that they have the posted names
        pageAndVerifyEntities( collectionName, null,numOfPages );

    }

    /**
     * Creates a number of entities with sequential names going up to the numOfEntities and posts them to the
     * collection specified with CollectionName.
     * @param collectionName
     * @param numOfEntities
     */
    public List<Entity> createEntities(String collectionName ,int numOfEntities ){
        List<Entity> entities = new LinkedList<>(  );

        for ( int i = 0; i < numOfEntities; i++ ) {
            Map<String, Object> entityPayload = new HashMap<String, Object>();
            entityPayload.put( "name", String.valueOf( i ) );
            Entity entity = new Entity( entityPayload );

            entities.add( entity );

            this.app().collection( collectionName ).post( entity );
        }

        this.refreshIndex();

        return entities;
    }

    //
    //    @Test
    //    @Ignore("ignored because currently startPaging is only be supported for queues and not for  "
    //            + "generic collections as this test assumes. "
    //            + "see also: https://issues.apache.org/jira/browse/USERGRID-211 ")
    //    public void startPaging() throws Exception {
    //
    //        CustomCollection things = context.application().customCollection( "test2things" );
    //
    //        int size = 40;
    //
    //        List<Map<String, String>> created = new ArrayList<Map<String, String>>( size );
    //
    //        for ( int i = 0; i < size; i++ ) {
    //            Map<String, String> entity = hashMap( "name", String.valueOf( i ) );
    //            things.create( entity );
    //
    //            created.add( entity );
    //        }
    //
    //        refreshIndex(context.getOrgName(), context.getAppName());
    //
    //        // now page them all
    //        ApiResponse response = null;
    //
    //        UUID start = null;
    //        int index = 0;
    //
    //        do {
    //
    //            response = parse( things.get() );
    //
    //            for ( Entity e : response.getEntities() ) {
    //                logger.debug("Getting item {} value {}", index, e.getProperties().get( "name" ).asText());
    //                assertEquals( created.get( index ).get( "name" ), e.getProperties().get( "name" ).asText() );
    //                index++;
    //            }
    //
    //            // decrement since we'll get this one again
    //            index--;
    //
    //            start = response.getEntities().get( response.getEntities().size() - 1 ).getUuid();
    //
    //            things = things.withStart( start );
    //        }
    //        while ( response != null && response.getEntities().size() > 1 );
    //
    //        // we paged them all
    //        assertEquals( created.size() - 1, index );
    //    }


    /**
     * Creates 40 objects and then creates a query to delete sets of 10 entities per call. Checks at the end
     * to make sure there are no entities remaining.
     * @throws Exception
     */
    @Test
    public void collectionBatchDeleting() throws Exception {


        String collectionName = "testCollectionBatchDeleting";

        int numOfEntities = 40;

        //Creates 40 entities by posting to collection
        createEntities( collectionName, numOfEntities );

        //sets the number of entities we want to delete per call.
        int deletePageSize = 10;
        int totalNumOfPages = numOfEntities/deletePageSize;
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setLimit( deletePageSize );

        //deletes the entities using the above set value. Then verifies that those entities were deleted.
        deleteByPage(deletePageSize,totalNumOfPages,collectionName,queryParameters);


        //verifies that we can't get anymore entities from the collection
        Collection getCollection = this.app().collection( collectionName ).get();

        assertEquals( "All entities should have been removed", 0, getCollection.getResponse().getEntityCount() );

        //now do 1 more delete, we shouldn't get any results
        ApiResponse response = this.app().collection( collectionName ).delete( queryParameters );
        assertEquals( "No more entities deleted", 0, response.getEntityCount() );
    }


    /**
     * Deletes entities from collectionName collection by deleting the number of entities specified in
     * deletePageSize. You can specific how many pages to delete by chaing the totalPages and what query you
     * want to attach by adding in QueryParameters
     *
     * @param deletePageSize
     * @param totalPages
     * @param collectionName
     * @param queryParameters
     */
    public void deleteByPage(int deletePageSize,int totalPages,String collectionName, QueryParameters queryParameters){
        for ( int i = 0; i < totalPages; i++ ) {

            ApiResponse response = this.app().collection( collectionName ).delete( queryParameters );

            this.refreshIndex();

            assertEquals( "Only 10 entities should have been deleted", deletePageSize, response.getEntityCount() );
        }
    }






    /**
     * Checks to make sure we can get an entity despite having a empty query, and limit parameter
     * @throws Exception
     */
    @Test
    public void emptyQlandLimitIgnored() throws Exception {

        String collectionName = "testEmptyQAndLimitIgnored";

        List<Entity> entities = createEntities( collectionName, 1 );


//        Map<String, Object> entityPayload = new HashMap<String, Object>();
//        entityPayload.put( "name", "thing1" );
//        Entity entity = new Entity( entityPayload );
//
//        this.app().collection( collectionName ).post( entity );
//
//        this.refreshIndex();

        //passes in empty parameters
        QueryParameters parameters = new QueryParameters();
        parameters.setKeyValue( "ql", "" );
        parameters.setKeyValue( "limit", "" );

        //make get using empty parameters
        pageAndVerifyEntities( collectionName,parameters, 1 );
//        Entity testEntity = this.app().collection( collectionName ).uniqueID( ( String ) entity.get( "name" ) )
//                                .get( parameters, true );
//
//        assertEquals( entities.get( 0 ), testEntity.get( "name" ) );
    }

    /**
     * Pages the specificed collectionName
     * @param collectionName
     * @param numOfPages
     * @return
     */
    public Collection pageAndVerifyEntities(String collectionName,QueryParameters queryParameters, int numOfPages ){
        //Get the entities that exist in the collection
        Collection testCollections = this.app().collection( collectionName ).get(queryParameters);

        //checks to make sure we can page through all entities in order.

        //Used as an index to see what value we're on.
        int index = 0;
        //Tells us how many pages we want to sort through
        for ( int i = 0; i < numOfPages; i++ ) {
            //page through returned entities.
            while ( testCollections.hasNext() ) {
                Entity returnedEntity = testCollections.next();
                //verifies that the names are in order
                assertEquals( String.valueOf( index++ ), returnedEntity.get( "name" ) );
            }
            testCollections = this.app().collection( collectionName ).getNextPage( testCollections,queryParameters, true );
        }

        //make sure the cursor is null after we have no more entities to page through.
        assertNull( testCollections.getCursor() );
        return testCollections;
    }

    /**
     * Checks to make sure we get a cursor when we should ( by creating 50 entities ) and then checks to make sure
     * we do not get a cursor when we create a collection of only 5 entities.
     * @throws Exception
     */
    @Test
    public void testCursor() throws Exception {

        // test that we do get cursor when we need one
        // create enough widgets to make sure we need a cursor
        int widgetsSize = 11;
        Map<String, Object> entityPayload = new HashMap<String, Object>();
        String collectionName = "testCursor" + UUIDUtils.newTimeUUID();

        for ( int i = 0; i < widgetsSize; i++ ) {

            entityPayload.put( "name", "value" + i );
            Entity entity = new Entity( entityPayload );
            this.app().collection( collectionName ).post( entity );
        }

        this.refreshIndex();

        //checks to make sure we have a cursor
        Collection testCollection = this.app().collection( collectionName ).get();
        assertNotNull( testCollection.getCursor() );
        assertEquals( 10, testCollection.getResponse().getEntityCount() );

        //Create new collection of only 5 entities
        String trinketCollectionName = "trinkets" + UUIDUtils.newTimeUUID();
        int trinketsSize = 5;
        for ( int i = 0; i < trinketsSize; i++ ) {

            entityPayload.put( "name", "value" + i );
            Entity entity = new Entity( entityPayload );
            this.app().collection( trinketCollectionName ).post( entity );
        }

        this.refreshIndex();

        //checks to make sure we don't get a cursor for just 5 entities.
        testCollection = this.app().collection( trinketCollectionName ).get();
        assertNull( testCollection.getCursor() );
        assertEquals( 5, testCollection.getResponse().getEntityCount() );
    }



    /**
     * Tests that we can create 100 entities and then get them back in the order that they were created 10 at a time and
     * retrieving the next 10 with a cursor.
     */
    @Test //TODO: add comments to test.
    public void pagingEntities() throws IOException {
        long created = 0;
        int maxSize = 100;
        int totalPagesExpected = 10;
        Map<String, Object> entityPayload = new HashMap<String, Object>();
        //TODO: set below in a helper method , don't need uuid since org+app unique for each test
        String collectionName = "testPagingEntities" + UUIDUtils.newTimeUUID();

        for ( created = 0; created < maxSize; created++ ) {

            entityPayload.put( "name", "value" + created );
            Entity entity = new Entity( entityPayload );
            this.app().collection( collectionName ).post( entity );
        }

        this.refreshIndex();

        Collection sandboxCollection = this.app().collection( collectionName ).get();
        assertNotNull( sandboxCollection );

        //TODO: put in helper method.
        created = 0;
        for ( int i = 0; i < totalPagesExpected; i++ ) {
            while ( sandboxCollection.hasNext() ) {
                Entity returnedEntity = sandboxCollection.next();
                assertEquals( "value" + created++, returnedEntity.get( "name" ) );
            }
            sandboxCollection = this.app().collection( collectionName ).getNextPage( sandboxCollection, null, true );
        }
    }


    @Ignore( "This test only checks activities and if we can retrieve them using a limit"
            + "Doesn't check to make sure we can page through entities that are connected using connections." )
    @Test
    public void pageThroughConnectedEntities() throws IOException {

        //        CustomCollection activities = context.collection( "activities" );
        //
        //        long created = 0;
        //        int maxSize = 100;
        //        long[] verifyCreated = new long[maxSize];
        //        Map actor = hashMap( "displayName", "Erin" );
        //        Map props = new HashMap();
        //
        //
        //        props.put( "actor", actor );
        //        props.put( "verb", "go" );
        //
        //        for ( int i = 0; i < maxSize; i++ ) {
        //
        //            props.put( "ordinal", i );
        //            JsonNode activity = activities.create( props );
        //            verifyCreated[i] = activity.findValue( "created" ).longValue();
        //            if ( i == 0 ) {
        //                created = activity.findValue( "created" ).longValue();
        //            }
        //        }
        //        ArrayUtils.reverse( verifyCreated );
        //
        //        refreshIndex( context.getOrgName(), context.getAppName() );
        //
        //        String query = "select * where created >= " + created;
        //
        //
        //        JsonNode node = activities.query( query, "limit", "2" ); //activities.query(query,"");
        //        int index = 0;
        //        while ( node.get( "entities" ).get( "created" ) != null ) {
        //            assertEquals( 2, node.get( "entities" ).size() );
        //
        //            if ( node.get( "cursor" ) != null ) {
        //                node = activities.query( query, "cursor", node.get( "cursor" ).toString() );
        //            }
        //
        //            else {
        //                break;
        //            }
        //        }
    }


    /**
     * Checks to make sure the query gives us the correct result set.
     * @throws Exception
     */
    //TODO: rework test
    @Test
    public void pagingQueryReturnCorrectResults() throws Exception {

        long created = 0;
        int maxSize = 20;
        Map<String, Object> entityPayload = new HashMap<String, Object>();
        List<Entity> entityPayloadVerifier = new LinkedList<>(  );
        long createdTimestamp = 0;
        String collectionName = "merp" + createdTimestamp;

        for ( created = 0; created < maxSize; created++ ) {

            if ( created >= 15 && created < 20 ) {

                entityPayload.put( "verb", "stop" );
            }
            else {
                entityPayload.put( "verb", "go" );
            }

            entityPayload.put( "name", "value" + created );
            Entity entity = new Entity( entityPayload );
            entityPayloadVerifier.add( entity );
            if(created == 15) {
                createdTimestamp = System.currentTimeMillis();
            }
            this.app().collection( collectionName ).post( entity );
        }

        this.refreshIndex();

        String query = "select * where created >= " + createdTimestamp + " or verb = 'stop'";

        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setQuery( query );
        Collection queryCollection = this.app().collection( collectionName ).get( queryParameters );

        assertNotNull( queryCollection );
        assertNull( queryCollection.getCursor() );
        assertEquals( 5, queryCollection.getResponse().getEntities().size() );

        for(int i = 15;i<maxSize;i++){
            Entity correctEntity = entityPayloadVerifier.get( i );
            Entity returnedEntity = queryCollection.next();
            assertEquals( correctEntity.get( "name" ), returnedEntity.get( "name" ) );
            assertEquals( correctEntity.get( "verb" ), returnedEntity.get( "verb" ) );

        }
    }


}
