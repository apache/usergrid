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


import java.util.HashMap;
import java.util.Map;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * Checks about different aspects of using a cursor combined with a query.
 */
@Concurrent()
public class PagingResourceIT extends AbstractRestIT {

    private static final Logger logger = LoggerFactory.getLogger( PagingResourceIT.class );

    /**
     * Creates 40 objects and then retrieves all 40 by paging through sets of 10 with a cursor.
     * @throws Exception
     */
    @Test
    public void collectionPaging() throws Exception {

        String collectionName = "testCollectionPaging" + UUIDUtils.newTimeUUID();

        int size = 40;

        //Creates 40 entities by posting to collection
        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entityPayload = new HashMap<String, Object>();
            entityPayload.put( "name", String.valueOf( i ) );
            Entity entity = new Entity( entityPayload );

            this.app().collection( collectionName ).post( entity );
        }

        this.refreshIndex();

        Collection testCollections = this.app().collection( collectionName ).get();

        //checks to make sure we can page through all entities in order.
        int index = 0;
        for ( int i = 0; i < 4; i++ ) {
            while ( testCollections.hasNext() ) {
                Entity returnedEntity = testCollections.next();
                assertEquals( String.valueOf( index++ ), returnedEntity.get( "name" ) );
            }
            testCollections = this.app().collection( collectionName ).getNextPage( testCollections, true );
        }
        //make sure the cursor is null after we have no more entities to page through.
        assertNull( testCollections.getCursor() );
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


        String collectionName = "testCollectionBatchDeleting" + UUIDUtils.newTimeUUID();

        //Creates 40 entities by posting to collection
        int size = 40;
        for ( int i = 0; i < size; i++ ) {
            Map<String, Object> entityPayload = new HashMap<String, Object>();
            entityPayload.put( "name", String.valueOf( i ) );
            Entity entity = new Entity( entityPayload );

            this.app().collection( collectionName ).post( entity );
        }

        this.refreshIndex();

        //sets the number of entities we want to delete per call.
        int deletePageSize = 10;
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setLimit( deletePageSize );

        //deletes the entities using the above set value. Then verifies that those entities were deleted.
        for ( int i = 0; i < size / deletePageSize; i++ ) {

            ApiResponse response = this.app().collection( collectionName ).delete( queryParameters );

            this.refreshIndex();

            assertEquals( "Only 10 entities should have been deleted", 10, response.getEntityCount() );
        }

        //verifies that we can't get anymore entities from the collection
        Collection getCollection = this.app().collection( collectionName ).get();

        assertEquals( "All entities should have been removed", 0, getCollection.getResponse().getEntityCount() );

        //now do 1 more delete, we shouldn't get any results
        ApiResponse response = this.app().collection( collectionName ).delete( queryParameters );
        assertEquals( "No more entities deleted", 0, response.getEntityCount() );
    }


    /**
     * Checks to make sure we can get an entity despite having a empty query, and limit parameter
     * @throws Exception
     */
    @Test
    public void emptyQlandLimitIgnored() throws Exception {

        String collectionName = "testEmptyQAndLimitIgnored" + UUIDUtils.newTimeUUID();


        Map<String, Object> entityPayload = new HashMap<String, Object>();
        entityPayload.put( "name", "thing1" );
        Entity entity = new Entity( entityPayload );

        this.app().collection( collectionName ).post( entity );

        this.refreshIndex();

        //passes in empty parameters
        QueryParameters parameters = new QueryParameters();
        parameters.setKeyValue( "ql", "" );
        parameters.setKeyValue( "limit", "" );

        //make get using empty parameters
        Entity testEntity = this.app().collection( collectionName ).uniqueID( ( String ) entity.get( "name" ) )
                                .get( parameters, true );

        assertEquals( entity.get( "name" ), testEntity.get( "name" ) );
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
}
