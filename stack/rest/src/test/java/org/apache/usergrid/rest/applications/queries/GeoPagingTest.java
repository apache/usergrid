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
package org.apache.usergrid.rest.applications.queries;


import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.UniformInterfaceException;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.persistence.geo.model.Point;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.CustomCollection;

import static org.junit.Assert.assertEquals;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class GeoPagingTest extends AbstractRestIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test //("Test uses up to many resources to run reliably") // USERGRID-1403
    public void groupQueriesWithGeoPaging() throws IOException {

        CustomCollection groups = context.application().customCollection( "test1groups" );

        int maxRangeLimit = 2000;
        long[] index = new long[maxRangeLimit];
        Map actor = hashMap( "displayName", "Erin" );

        Map props = new HashMap();

        props.put( "actor", actor );
        Map location = hashMap( "latitude", 37 );
        location.put( "longitude", -75 );
        props.put( "location", location );
        props.put( "verb", "go" );
        props.put( "content", "bragh" );

        for ( int i = 0; i < 5; i++ ) {
            String newPath = String.format( "/kero" + i );
            props.put( "path", newPath );
            props.put( "ordinal", i );
            JsonNode activity = groups.create( props );
            index[i] = activity.findValue( "created" ).longValue();
        }

        refreshIndex(context.getOrgName(), context.getAppName());

        String query = "select * where location within 20000 of 37,-75 "
                + " and created > " + index[2]
                + " and created < " + index[4] + "";

        JsonNode node = groups.withQuery( query ).get();
        assertEquals( 1, node.get( "entities" ).size() );

        assertEquals( index[3], node.get( "entities" ).get( 0 ).get( "created" ).longValue() );
    }


    @Test // USERGRID-1401
    public void groupQueriesWithConsistentResults() throws IOException {

        CustomCollection groups = context.application().customCollection( "test2groups" );

        int maxRangeLimit = 20;
        JsonNode[] saved = new JsonNode[maxRangeLimit];

        Map<String, String> actor = hashMap( "displayName", "Erin" );
        Map<String, Object> props = new HashMap<String, Object>();

        props.put( "actor", actor );
        Map<String, Integer> location = hashMap( "latitude", 37 );
        location.put( "longitude", -75 );
        props.put( "location", location );
        props.put( "verb", "go" );
        props.put( "content", "bragh" );

        for ( int i = 0; i < 20; i++ ) {
            String newPath = String.format( "/kero" + i );
            props.put( "path", newPath );
            props.put( "ordinal", i );
            JsonNode activity = groups.create( props ).get( "entities" ).get( 0 );
            saved[i] = activity;
        }

        refreshIndex(context.getOrgName(), context.getAppName());
        JsonNode node = null;
        for ( int consistent = 0; consistent < 20; consistent++ ) {

            String query = String.format(
                "select * where location within 100 of 37, -75 and ordinal >= %d and ordinal < %d",
                saved[7].get( "ordinal" ).asLong(), saved[10].get( "ordinal" ).asLong() );

            node = groups.withQuery( query ).get(); //groups.query(query);

            JsonNode entities = node.get( "entities" );

            assertEquals( 3, entities.size() );

            for ( int i = 0; i < 3; i++ ) {
                // shouldn't start at 10 since you're excluding it above in the query, it should return 9,8,7
                assertEquals( saved[7 + i], entities.get( i ) );
            }
        }
    }


    /**
     * Creates a store then queries to check ability to find different store from up to 40 mil meters away
     * @throws IOException
     */
    @Test
    public void testFarAwayLocationFromCenter() throws IOException {

        JsonNode node = null;
        String collectionName = "testFarAwayLocation";
        Map store1 = entityMapLocationCreator( "usergrid",-33.746369 ,150.952183 );
        Map store2 = entityMapLocationCreator( "usergrid2",-33.889058, 151.124024  );
        Point center = new Point( 37.776753, -122.407846 );
        //TODO: make query builder for this
        Query queryClose = locationQuery( 10000 ,center );
        Query queryFar = locationQuery( 40000000, center );

        /*Create */
        try {
            node = context.collection( collectionName ).post( store1);
        }
        catch ( UniformInterfaceException e ) {
            JsonNode nodeError = mapper.readTree( e.getResponse().getEntity( String.class ) );
            fail( node.get( "error" ).textValue() );
        }
        assertNotNull( node );
        assertEquals( "usergrid", node.get( "name" ).asText() );
//TODO: check to see if context.application.collection is broken?
        try {
            node = context.collection( collectionName ).post( store2 );
        }
        catch ( UniformInterfaceException e ) {
            JsonNode nodeError = mapper.readTree( e.getResponse().getEntity( String.class ) );
            fail( node.get( "error" ).textValue() );
        }

        refreshIndex( context.getOrgName(),context.getAppName() );

        /* run queries */
        //context.collection( collectionName ).query(  )



        // assertEquals(node.get(  ))
    }

    private Map entityMapLocationCreator(String name,Double lat, Double lon){
        Map<String, Double> latLon = hashMap( "latitude", lat );
        latLon.put( "longitude", lon );
        Map<String, Object> entityData = new HashMap<String, Object>();
        entityData.put( "name", name );
        entityData.put( "location", latLon );

        return entityData;
    }

    private Query locationQuery(int  metersAway, Point startingPoint){
        return Query.fromQL( "select * where location within " + String.valueOf( metersAway ) + " of "
                + startingPoint.getLat() + "," + startingPoint.getLon());
    }


}
