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
import java.util.*;

import org.junit.Ignore;
import org.junit.Test;


import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;

import static org.junit.Assert.*;

/**
 * Tests paging with respect to entities. Also tests cursors and queries with respect to paging.
 */

public class PagingResourceIT extends AbstractRestIT {

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
        QueryParameters queryParameters = new QueryParameters().setLimit( deletePageSize );

        //deletes the entities using the above set value. Then verifies that those entities were deleted.
        deleteByPage(deletePageSize,totalNumOfPages,collectionName,queryParameters);


        Thread.sleep(2000);
        //verifies that we can't get anymore entities from the collection
        Collection getCollection = this.app().collection(collectionName).get();

        assertFalse("All entities should have been removed", getCollection.hasNext());

        //now do 1 more delete, we shouldn't get any results
        ApiResponse response = this.app().collection( collectionName ).delete( queryParameters );
        assertEquals("No more entities deleted", 0, response.getEntityCount());
    }

    @Test
    public void collectionBatchDeletingWithQuery() throws Exception {


        String collectionName = "testCollectionBatchDeleting";

        int numOfEntities = 40;

        //Creates 40 entities by posting to collection
        createEntities(collectionName, numOfEntities);

        //sets the number of entities we want to delete per call.
        int deletePageSize = 10;
        int totalNumOfPages = numOfEntities/deletePageSize;
        QueryParameters queryParameters = new QueryParameters().setLimit(deletePageSize);
        queryParameters.addParam("ql", "select * where city='Denver'");

        //deletes the entities using the above set value. Then verifies that those entities were deleted.
        deleteByPage(deletePageSize, totalNumOfPages, collectionName, queryParameters, false);


        Thread.sleep(2000);
        //verifies that we can't get anymore entities from the collection
        Collection getCollection = this.app().collection( collectionName ).get(queryParameters);

        assertFalse( "All entities should have been removed", getCollection.hasNext());

        //now do 1 more delete, we shouldn't get any results
        Collection response = this.app().collection( collectionName ).get(queryParameters);
        assertEquals("No more entities deleted", 0, response.getNumOfEntities());
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
    public void deleteByPage(int deletePageSize,int totalPages,String collectionName, QueryParameters queryParameters) {
        deleteByPage(deletePageSize, totalPages, collectionName, queryParameters, true);
    }

    public void deleteByPage(int deletePageSize,int totalPages,String collectionName, QueryParameters queryParameters, boolean validate){
        for ( int i = 0; i < totalPages; i++ ) {

            ApiResponse response = this.app().collection( collectionName ).delete( queryParameters );

            this.refreshIndex();

            if(validate)
                assertEquals("Entities should have been deleted", deletePageSize,response.getEntityCount() );
            try{Thread.sleep(100);}catch (InterruptedException ie){

            }
        }
    }

    /**
     * Checks to make sure we can get an entity despite having a empty query, and limit parameter
     * @throws Exception
     */
    @Test
    public void emptyQlandLimitIgnored() throws Exception {

        String collectionName = "testEmptyQAndLimitIgnored";

        int numOfEntities = 1;
        int numOfPages = 1;

        createEntities( collectionName, numOfEntities );

        //passes in empty parameters
        QueryParameters parameters = new QueryParameters();
        parameters.setKeyValue( "ql", "" );
        parameters.setKeyValue( "limit", "" );

        //sends GET call using empty parameters
        pageAndVerifyEntities( collectionName,parameters, numOfPages, numOfEntities );

    }



    /**
     * Checks to make sure we get a cursor when we should ( by creating 11 entities ) and then checks to make sure
     * we do not get a cursor when we create a collection of only 5 entities.
     * @throws Exception
     */
    @Test
    public void testCursor() throws Exception {

        // test that we do get cursor when we need one
        // create enough widgets to make sure we need a cursor
        int numOfEntities = 11;
        int numOfPages = 2;
        Map<String, Object> entityPayload = new HashMap<String, Object>();
        String collectionName = "testCursor" ;

        createEntities( collectionName, numOfEntities );

        //checks to make sure we have a cursor
        //pages through entities and verifies that they are correct.
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setQuery( "select * ORDER BY created" );
        pageAndVerifyEntities( collectionName,queryParameters,numOfPages, numOfEntities );

        //Create new collection of only 5 entities
        String trinketCollectionName = "trinkets" ;
        numOfEntities = 5;
        numOfPages = 1;
        createEntities( trinketCollectionName, numOfEntities );

        //checks to make sure we don't get a cursor for just 5 entities.
        //Created a new query parameter because when generated it store the cursor token back into it.
        queryParameters = new QueryParameters();
        queryParameters.setQuery( "select * ORDER BY created" );
        pageAndVerifyEntities( trinketCollectionName,queryParameters,numOfPages, numOfEntities );

    }

    /**
     * Tests that we can create 100 entities and then get them back in the order that they were created 10 at a time and
     * retrieving the next 10 with a cursor.
     */
    @Test
    public void pagingEntities() throws IOException {

        int numOfEntities = 100;
        int numOfPages = 10;
        String collectionName = "testPagingEntities" ;

        //creates entities
        createEntities(collectionName, numOfEntities);

        //pages through entities and verifies that they are correct.
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setQuery( "select * ORDER BY created" );
        pageAndVerifyEntities(collectionName, queryParameters, numOfPages, numOfEntities);
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
        int numOfEntities = 100;
        int numOfPages = 10;
        Entity connectedEntity = null;
        Map<String, Object> entityPayload = new HashMap<String, Object>();
        String collectionName = "pageThroughConnectedEntities" ;

        for ( created = 1; created <= numOfEntities; created++ ) {

            entityPayload.put( "name", "value" + created );
            Entity entity = new Entity( entityPayload );
            entity = this.app().collection( collectionName ).post( entity );
            refreshIndex();
            if(created == 1){
                connectedEntity = entity;
            }
            else if (created > 0){
                this.app().collection( collectionName ).entity( connectedEntity ).connection( "likes" ).entity( entity ).post();
            }
        }

        refreshIndex();

        Collection colConnection =  this.app().collection( collectionName ).entity(connectedEntity).connection("likes").get();
        assertNotNull(colConnection);
        assertNotNull( colConnection.getCursor() );
        pageAndVerifyEntities(collectionName, null, numOfPages, numOfEntities);

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
        int numOfEntities = 20;
        int numOfChangedEntities = numOfEntities - indexForChangedEntities;
        Map<String, Object> entityPayload = new HashMap<String, Object>();

        String collectionName = "merp";

        //Creates Entities
        for ( created = 0; created < numOfEntities; created++ ) {

            //Creates all entities between 15 and 20 with the verb stop
            if ( created >= indexForChangedEntities && created < numOfEntities ) {

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

        refreshIndex();

        //Creates query looking for entities with the very stop.
        String query = "select * where verb = 'stop'";
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setQuery( query );

        //Get the collection with the query applied to it
        Collection queryCollection = this.app().collection( collectionName ).get( queryParameters );
        assertNotNull( queryCollection );
        //assert that there is no cursor because there is <10 entities.
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
     * A method that calls the cursor a numOfPages number of times and verifies that entities are stored in order of
     * creation from the createEntities method.
     * @param collectionName
     * @param numOfPages
     * @return
     */
    public Collection pageAndVerifyEntities(String collectionName,QueryParameters queryParameters, int numOfPages, int numOfEntities ){
        //Get the entities that exist in the collection
        Collection testCollections = this.app().collection( collectionName ).get(queryParameters);

        //checks to make sure we can page through all entities in order.

        //Used as an index to see what value we're on and also used to keep track of the current index of the entity.
        int entityIndex = 1;
        int pageIndex = 0;


        //Counts all the entities in pages with cursors
        while(testCollections.getCursor()!=null){
            //page through returned entities.
            while ( testCollections.hasNext() ) {
                Entity returnedEntity = testCollections.next();
                //verifies that the names are in order, named string values will always +1 of the current index
                assertEquals( String.valueOf( entityIndex ), returnedEntity.get( "name" ) );
                entityIndex++;
            }
            testCollections =
                        this.app().collection( collectionName ).getNextPage( testCollections, queryParameters, true );
            //increment the page count because we have just loops through a page of entities
            pageIndex++;

        }

        //if the testCollection does have entities then increment the page
        if(testCollections.hasNext()) {
         pageIndex++;
        }

        //handles left over entities at the end of the page when the cursor is null.
        while ( testCollections.hasNext() ) {
            //increment the page count because having entities ( while no cursor ) counts as having a page.
            Entity returnedEntity = testCollections.next();
            //verifies that the names are in order, named string values will always +1 of the current index
            assertEquals( String.valueOf( entityIndex ), returnedEntity.get( "name" ) );
            entityIndex++;
        }


        //added in a minus one to account for the adding the additional 1 above.
        assertEquals( numOfEntities, entityIndex-1 );
        assertEquals( numOfPages, pageIndex );
        return testCollections;
    }

    /**
     * Creates a number of entities with sequential names going up to the numOfEntities and posts them to the
     * collection specified with CollectionName.
     * @param collectionName
     * @param numOfEntities
     */
    public List<Entity> createEntities(String collectionName ,int numOfEntities ){
        List<Entity> entities = new LinkedList<>(  );
        Random random = new Random();
        List<String> cities = new ArrayList<String>();
        cities.add("Denver");
        cities.add("New York");
        cities.add("Los Angeles");
        cities.add("Los");
        cities.add("Boulder");


        for ( int i = 1; i <= numOfEntities; i++ ) {
            Map<String, Object> entityPayload = new HashMap<String, Object>();
            entityPayload.put( "name", String.valueOf( i ) );
            entityPayload.put( "city", cities.get(random.nextInt(5)) );

            Entity entity = new Entity( entityPayload );

            entities.add( entity );

            this.app().collection( collectionName ).post( entity );
        }

        this.refreshIndex();

        return entities;
    }

}
