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


import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.UniformInterfaceException;

import java.io.IOException;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Results;

import org.jclouds.json.Json;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.persistence.geo.model.Point;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
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

        CustomCollection groups = context.application().customCollection("test1groups");

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

        CustomCollection groups = context.application().customCollection("test2groups");

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
     */
    @Test
    public void testFarAwayLocationFromCenter() throws IOException {

        JsonNode node = null;
        String collectionName = "testFarAwayLocation" + UUIDUtils.newTimeUUID();
        Point center = new Point( 37.776753, -122.407846 );

        String queryClose = locationQuery( 10000, center );
        String queryFar = locationQuery( 40000000, center );

        //TODO: move test setup out of the test.
        /*Create */
        createGeoUser( "usergrid", collectionName, -33.746369, 150.952183 );

        createGeoUser( "usergrid2", collectionName, -33.889058, 151.124024 );

        /* run queries */

        node = queryCollection( collectionName, queryClose );

        assertEquals( "Results from nearby, should return nothing", 0, node.get( "entities" ).size() );

        node = queryCollection( collectionName, queryFar );

        assertEquals( "Results from center point to ridiculously far", 2, node.get( "entities" ).size() );
    }


    /**
     * Creates a store right on top of the center store and checks to see if we can find that store, then find both
     * stores.
     */
    @Test
    public void testFarAwayLocationWithOneResultCloser() throws IOException {
        JsonNode node = null;
        String collectionName = "testFarAwayLocation" + UUIDUtils.newTimeUUID();
        Point center = new Point( -33.746369, 150.952183 );

        String queryClose = locationQuery( 10000, center );
        String queryFar = locationQuery( 40000000, center );

        /*Create */
        createGeoUser( "usergrid", collectionName, -33.746369, 150.952183 );

        createGeoUser( "usergrid2", collectionName, -33.889058, 151.124024 );

        /* run queries */

        node = queryCollection( collectionName, queryClose );

        assertEquals( "Results from nearby, should return 1 store", 1, node.get( "entities" ).size() );

        node = queryCollection( collectionName, queryFar );

        assertEquals( "Results from center point to ridiculously far", 2, node.get( "entities" ).size() );
    }


    /**
     * Creates two users, then a huge matrix of coordinates, then checks to see if any of the coordinates are near our users
     * @throws IOException
     */
    @Test
    public void createHugeMatrixOfCoordinates() throws IOException {


        JsonNode node = null;

        Map user1Coordinates = entityMapLocationCreator( -33.746369 ,150.952183 );
        Map user2Coordinates = entityMapLocationCreator( -33.889058, 151.124024 );

        /*Create */
        try {
            node = context.users().post("norwest", "norwest@usergrid.com", "norwest", user1Coordinates);
        }
        catch ( UniformInterfaceException e ) {
            JsonNode nodeError = mapper.readTree( e.getResponse().getEntity( String.class ) );
            fail( node.get( "error" ).textValue() );
        }

        /*Create */
        try {
            node = context.users().post("ashfield", "ashfield@usergrid.com", "ashfield", user2Coordinates);
        }
        catch ( UniformInterfaceException e ) {
            JsonNode nodeError = mapper.readTree( e.getResponse().getEntity( String.class ) );
            fail( node.get( "error" ).textValue() );
        }

        refreshIndex( context.getOrgName(),context.getAppName() );

        List<Point> points = new ArrayList<Point>();
        points.add(new Point( 33.746369,-89 ));//Woodland, MS
        points.add(new Point( 33.746369,-91 ));//Beulah, MS
        points.add(new Point( -1.000000, 102.000000 ));//Somewhere in Indonesia
        points.add(new Point( -90.000000, 90.000000 ));//Antarctica
        points.add(new Point( 90, 90 ));//Santa's house
        //and the cartesian product...
        for(int i= -90;i<=90;i++){
            for(int j= -180;j<=180;j++){
                points.add(new Point( i, j ));
            }
        }
        Iterator<Point> pointIterator = points.iterator();
        for(Point p=pointIterator.next();pointIterator.hasNext();p=pointIterator.next()){

            Point center = new Point( p.getLat(),  p.getLon() );
            String query = locationQuery( 10000 ,center );

            try {
                //node = context.users( ).withQuery(query).get( );
            }
            catch ( UniformInterfaceException e ) {
                JsonNode nodeError = mapper.readTree( e.getResponse().getEntity( String.class ) );
                fail( node.get( "error" ).textValue() );
            }

/*
            Query query = Query.fromQL( "select * where location within 10000 of "
                    + p.getLat() + "," + p.getLon());
            Results listResults = em.searchCollection( em.getApplicationRef(), "stores", query );

            this.dump( listResults );
            assertEquals("Results less than 10000m away from center", 0, listResults.size() );

            query = Query.fromQL( "select * where location within 40000000 of "
                    + p.getLat() + "," + p.getLon());
            listResults = em.searchCollection( em.getApplicationRef(), "stores", query );

            assertEquals("Results from center point to ridiculously far", 2, listResults.size() );
*/
        }
    }


    private JsonNode queryCollection( String collectionName, String query ) throws IOException {
        JsonNode node = null;
        try {
            node = context.collection( collectionName ).withQuery( query ).get();
        }
        catch ( UniformInterfaceException e ) {
            JsonNode nodeError = mapper.readTree( e.getResponse().getEntity( String.class ) );
            fail( node.get( "error" ).textValue() );
        }

        assertNotNull( node );
        return node;
    }


    private void createGeoUser( String username, String collectionName, Double lat, Double lon ) throws IOException {

        JsonNode node = null;


        Map<String, Object> user = entityMapLocationCreator( lat, lon );
        user.put( "name", username );

        try {
            node = context.collection( collectionName ).post( user );
        }
        catch ( UniformInterfaceException e ) {
            JsonNode nodeError = mapper.readTree( e.getResponse().getEntity( String.class ) );
            fail( node.get( "error" ).textValue() );
        }

        assertNotNull( node );
        assertEquals( username, node.get( "name" ).asText() );

        context.refreshIndex();
    }


    private Map<String, Object> entityMapLocationCreator( Double lat, Double lon ) {
        Map<String, Double> latLon = hashMap( "latitude", lat );
        latLon.put( "longitude", lon );
        Map<String, Object> entityData = new HashMap<String, Object>();
        entityData.put( "location", latLon );

        return entityData;
    }


    private String locationQuery( int metersAway, Point startingPoint ) {
        return "select * where location within " + String.valueOf( metersAway ) + " of " + startingPoint.getLat() + ","
                + startingPoint.getLon();
    }


}
