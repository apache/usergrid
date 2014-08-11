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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.java.client.entities.Entity;
import org.apache.usergrid.java.client.response.ApiResponse;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.CustomCollection;
import org.apache.usergrid.rest.test.resource.EntityResource;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Simple tests to test querying at the REST tier */
@Concurrent()
public class PagingResourceIT extends AbstractRestIT {

    private static final Logger logger = LoggerFactory.getLogger( PagingResourceIT.class );

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test
    public void collectionPaging() throws Exception {

        CustomCollection things = context.application().collection( "test1things" );

        int size = 40;

        List<Map<String, String>> created = new ArrayList<Map<String, String>>( size );

        for ( int i = 0; i < size; i++ ) {
            Map<String, String> entity = hashMap( "name", String.valueOf( i ) );
            things.create( entity );

            created.add( entity );
        }

        refreshIndex(context.getOrgName(), context.getAppName());

        // now page them all
        ApiResponse response = null;
        Iterator<Map<String, String>> entityItr = created.iterator();

        do {

            response = parse( things.get() );

            for ( Entity e : response.getEntities() ) {
                assertTrue( entityItr.hasNext() );
                assertEquals( entityItr.next().get( "name" ), e.getProperties().get( "name" ).asText() );
                logger.debug("Got item value {}", e.getProperties().get( "name" ).asText());
            }

            logger.debug("response cursor: " + response.getCursor() );
            
            things = things.withCursor( response.getCursor() );
        }
        while ( response != null && response.getCursor() != null );

        assertFalse("Should have paged them all", entityItr.hasNext() );
    }


    @Test
    public void startPaging() throws Exception {

        CustomCollection things = context.application().collection( "test2things" );

        int size = 40;

        List<Map<String, String>> created = new ArrayList<Map<String, String>>( size );

        for ( int i = 0; i < size; i++ ) {
            Map<String, String> entity = hashMap( "name", String.valueOf( i ) );
            things.create( entity );

            created.add( entity );
        }

        refreshIndex(context.getOrgName(), context.getAppName());

        // now page them all
        ApiResponse response = null;

        UUID start = null;
        int index = 0;

        do {

            response = parse( things.get() );

            for ( Entity e : response.getEntities() ) {
                logger.debug("Getting item {} value {}", index, e.getProperties().get( "name" ).asText());
                assertEquals( created.get( index ).get( "name" ), e.getProperties().get( "name" ).asText() );
                index++;
            }

            // decrement since we'll get this one again
            index--;

            start = response.getEntities().get( response.getEntities().size() - 1 ).getUuid();

            things = things.withStart( start );
        }
        while ( response != null && response.getEntities().size() > 1 );

        // we paged them all
        assertEquals( created.size() - 1, index );
    }


    @Test
    public void collectionBatchDeleting() throws Exception {

        CustomCollection things = context.application().collection( "test3things" );

        int size = 40;

        List<Map<String, String>> created = new ArrayList<Map<String, String>>( size );

        for ( int i = 0; i < size; i++ ) {
            Map<String, String> entity = hashMap( "name", String.valueOf( i ) );
            things.create( entity );

            created.add( entity );
        }

        refreshIndex(context.getOrgName(), context.getAppName());

        ApiResponse response;
        int deletePageSize = 10;

        things = things.withLimit( deletePageSize );

        for ( int i = 0; i < size / deletePageSize; i++ ) {
            response = parse( things.delete() );

            refreshIndex(context.getOrgName(), context.getAppName());

            assertEquals( "Only 10 entities should have been deleted", 10, response.getEntityCount() );
        }

        response = parse( things.get() );

        assertEquals( "All entities should have been removed", 0, response.getEntityCount() );

        //now do 1 more delete, we should get any results

        response = parse( things.delete() );

        assertEquals( "No more entities deleted", 0, response.getEntityCount() );
    }


    @Test
    public void emptyQlandLimitIgnored() throws Exception {

        CustomCollection things = context.application().collection( "test4things" );

        Map<String, String> data = hashMap( "name", "thing1" );
        JsonNode response = things.create( data );

        refreshIndex(context.getOrgName(), context.getAppName());

        JsonNode entity = getEntity( response, 0 );

        String uuid = entity.get( "uuid" ).asText();

        EntityResource entityRequest = things.entity( "thing1" ).withParam( "ql", "" ).withParam( "limit", "" );

        JsonNode returnedEntity = getEntity( entityRequest.get(), 0 );

        assertEquals( entity, returnedEntity );

        entityRequest = things.entity( uuid ).withParam( "ql", "" ).withParam( "limit", "" );

        returnedEntity = getEntity( entityRequest.get(), 0 );

        assertEquals( entity, returnedEntity );

        // now do a delete
        returnedEntity = getEntity( entityRequest.delete(), 0 );

        assertEquals( entity, returnedEntity );

        refreshIndex(context.getOrgName(), context.getAppName());

        // verify it's gone
        returnedEntity = getEntity( things.entity( uuid ).get(), 0 );

        assertNull( returnedEntity );
    }


    private static ObjectMapper mapper = new ObjectMapper();


    private static final ApiResponse parse( JsonNode response ) throws Exception {
        String jsonResponseString = mapper.writeValueAsString( response );
        return mapper.readValue( jsonResponseString, ApiResponse.class );
    }
}
