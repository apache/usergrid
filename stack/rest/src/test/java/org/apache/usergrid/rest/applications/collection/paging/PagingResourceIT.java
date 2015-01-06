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

import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

        assertFalse( "All entities should have been removed", getCollection.hasNext());

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

        createEntities( collectionName, 1 );

        //passes in empty parameters
        QueryParameters parameters = new QueryParameters();
        parameters.setKeyValue( "ql", "" );
        parameters.setKeyValue( "limit", "" );

        //sends GET call using empty parameters
        pageAndVerifyEntities( collectionName,parameters, 1 );

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
        String collectionName = "testCursor" ;

        createEntities( collectionName, widgetsSize );

        //checks to make sure we have a cursor
        pageAndVerifyEntities( collectionName,null,2 );

        //Create new collection of only 5 entities
        String trinketCollectionName = "trinkets" ;
        int trinketsSize = 5;
        createEntities( trinketCollectionName,trinketsSize );

        //checks to make sure we don't get a cursor for just 5 entities.
        pageAndVerifyEntities( trinketCollectionName,null,1 );

    }

    /**
     * Tests that we can create 100 entities and then get them back in the order that they were created 10 at a time and
     * retrieving the next 10 with a cursor.
     */
    @Test
    public void pagingEntities() throws IOException {
        int maxSize = 100;
        int totalPagesExpected = 10;
        String collectionName = "testPagingEntities" ;

        //creates entities
        createEntities( collectionName,maxSize );

        //pages through entities and verifies that they are correct.
        pageAndVerifyEntities( collectionName,null,totalPagesExpected );
    }


    /**
     * Pages through entities that are connected to each other
     * @throws IOException
     */
    @Ignore("This does not return a page for any entities. It just keeps returning them in bulk."
            + " Not sure about intended functionality")
    @Test
    public void pageThroughConnectedEntities() throws IOException {


        long created = 0;
        int maxSize = 100;
        int totalPagesExpected = 10;
        Entity connectedEntity = null;
        Map<String, Object> entityPayload = new HashMap<String, Object>();
        String collectionName = "pageThroughConnectedEntities" ;

        for ( created = 0; created < maxSize; created++ ) {

            entityPayload.put( "name", "value" + created );
            Entity entity = new Entity( entityPayload );
            entity = this.app().collection( collectionName ).post( entity );
            if(created == 0){
                connectedEntity = entity;
            }
            else if (created > 0){
                this.app().collection( collectionName ).entity( connectedEntity ).connection( "likes" ).entity( entity ).post();
            }
        }

        Collection colConnection =  this.app().collection( collectionName ).entity( connectedEntity ).connection( "likes" ).get();
        assertNotNull( colConnection );
        assertNotNull( colConnection.getCursor() );
        pageAndVerifyEntities( collectionName,null,totalPagesExpected );

        /**
         * the below checks to make sure we get a cursor back and go through each one to page the entities.
         */


    }


    /**
     * Checks to make sure the query gives us the correct result set.
     * Creates entities with different verbs and does a query to make sure the exact same entities are returned
     * when queried for. This is accomplished by saving the created entities in a list.
     * @throws Exception
     */
    @Test
    public void pagingQueryReturnCorrectResults() throws Exception {

        long created = 0;
        int indexForChangedEntities = 15;
        int maxSize = 20;
        int numOfChangedEntities = maxSize - indexForChangedEntities;
        Map<String, Object> entityPayload = new HashMap<String, Object>();

        String collectionName = "merp";

        //Creates Entities
        for ( created = 0; created < maxSize; created++ ) {

            //Creates all entities between 15 and 20 with the verb stop
            if ( created >= indexForChangedEntities && created < maxSize ) {

                entityPayload.put( "verb", "stop" );
            }
            //all other entities are tagged with the verb go
            else {
                entityPayload.put( "verb", "go" );
            }

            entityPayload.put( "name", "value" + created );
            Entity entity = new Entity( entityPayload );

            this.app().collection( collectionName ).post( entity );
        }

        this.refreshIndex();

        //Creates query looking for entities with the very stop.
        String query = "select * where verb = 'stop'";
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setQuery( query );

        //Get the collection with the query applied to it
        Collection queryCollection = this.app().collection( collectionName ).get( queryParameters );
        assertNotNull( queryCollection );
        assertNull( queryCollection.getCursor() );
        assertEquals( numOfChangedEntities, queryCollection.getNumOfEntities() );

        //Gets the supposed number of changed entities and checks they have the correct verb.
        for(int i = 0; i<numOfChangedEntities; i++){
            assertEquals( "stop", queryCollection.next().get( "verb" ) );
        }
        //makes sure there are no entities left in the collection.
        assertFalse( queryCollection.hasNext() );
    }

    /**
     * Not a test
     * Helper method that takes in the <collectionName> collection applies the queryParameters and pages through
     * results for the given number of pages.
     * @param collectionName
     * @param numOfPages
     * @return
     */
    //TODO: add in a +1 to numOfPAges so that if we want to loop through a collection with no pages we would put in 0 instead of 1 page.
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
            if(testCollections.getCursor()!=null) {
                testCollections =
                        this.app().collection( collectionName ).getNextPage( testCollections, queryParameters, true );
            }
        }

        //make sure the cursor is null after we have no more entities to page through.
        assertNull( testCollections.getCursor() );
        return testCollections;
    }

}
