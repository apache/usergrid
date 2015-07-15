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


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.rest.TestContextSetup;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.UniformInterfaceException;

import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


/**
 * Basic Geo Tests - CRUD entities with geo points, exceptions for malformed calls
 *
 * @author rockerston
 */
public class BasicGeoTests extends AbstractRestIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );

    public final String latitude = "latitude";

    /**
     * Create a entity with a geo location point in it
     * 1. Create entity
     * 2. Verify that the entity was created
     */
    @Test
    public void createEntityWithGeoLocationPoint() throws IOException {

        String collectionType = "stores";
        JsonNode node = null;
        Double lat = 37.776753;
        Double lon = -122.407846;
        //1. Create entity
        Map<String, Double> latLon = hashMap("latitude", lat);
        latLon.put( "longitude", lon );
        Map<String, Object> entityData = new HashMap<String, Object>();
        entityData.put( "location", latLon );

        try {
            node = context.collection( collectionType ).post( entityData );
        }
        catch ( UniformInterfaceException e ) {
            JsonNode nodeError = mapper.readTree( e.getResponse().getEntity( String.class ) );
            fail( nodeError.get( "error" ).textValue() );
        }

        //2. Verify that the entity was created
        assertNotNull( node );
        assertEquals( lat.toString(), node.get("location").get("latitude").asText() );
        assertEquals( lon.toString(), node.get( "location" ).get("longitude").asText() );

    }

    /**
     * Update an entity with a geo location point in it
     * 1. create an entity with a geo point
     * 2. read back that entity make sure it is accurate
     * 3. update the geo point to a new value
     * 4. read back the updated entity, make sure it is accurate
     */
    @Test
    public void updateEntityWithGeoLocationPoint() throws IOException {

        String collectionType = "stores";
        String entityName = "cornerStore";
        JsonNode entity = null;
        Double lat = 37.776753;
        Double lon = -122.407846;

        //1. create an entity with a geo point
        Map<String, Double> latLon = hashMap("latitude", lat);
        latLon.put( "longitude", lon );
        Map<String, Object> entityData = new HashMap<String, Object>();
        entityData.put( "location", latLon );
        entityData.put( "name", entityName );

        try {
            entity = context.collection( collectionType ).post( entityData );
        }
        catch ( UniformInterfaceException e ) {
            JsonNode nodeError = mapper.readTree( e.getResponse().getEntity( String.class ) );
            fail( nodeError.get( "error" ).textValue() );
        }

        assertNotNull(entity);
        assertEquals( lat.toString(), entity.get("location").get("latitude").asText() );
        assertEquals( lon.toString(), entity.get( "location" ).get("longitude").asText() );

        context.refreshIndex();

        //2. read back that entity make sure it is accurate
        /*
        try {
            node = context.collection( collectionType ).get(entityName);
        }
        catch ( UniformInterfaceException e ) {
            JsonNode nodeError = mapper.readTree( e.getResponse().getEntity( String.class ) );
            fail( nodeError.get( "error" ).textValue() );
        }

        //3. update the geo point to a new value
        Double newLat = 35.776753;
        Double newLon = -119.407846;
        latLon.put( "latitude", newLat );
        latLon.put( "longitude", newLon );
        entityData.put( "location", latLon );

        //TODO PUT should take name property and append it to URL - e.g. PUT /cats/fluffy  not PUT /cats {"name":"fluffy"}
        try {
            //node = context.collection( collectionType ).put(entityData);
            //entity.put(entityData);

        }
        catch ( UniformInterfaceException e ) {
            JsonNode nodeError = mapper.readTree( e.getResponse().getEntity( String.class ) );
            fail( nodeError.get( "error" ).textValue() );
        }

        assertNotNull(entity);
        assertEquals( newLat.toString(), entity.get("location").get("latitude").asText() );
        assertEquals( newLon.toString(), entity.get( "location" ).get("longitude").asText() );
  */

        context.refreshIndex();

        //4. read back the updated entity, make sure it is accurate





    }

    /**
     * Test exceptions for entities with poorly created geo points
     * 1. misspell latitude
     * 2. misspell longitude
     */
    @Test
    public void createEntitiesWithBadSpelling() throws IOException {

        String collectionType = "stores";
        JsonNode node = null;
        Double lat = 37.776753;
        Double lon = -122.407846;

        // 1. misspell latitude
        Map<String, Double> misspelledLatitude = hashMap("latitudee", lat);
        misspelledLatitude.put( "longitude", lon );
        Map<String, Object> misspelledLatitudeEntityData = new HashMap<String, Object>();
        misspelledLatitudeEntityData.put( "location", misspelledLatitude );

        try {
            node = context.collection( collectionType ).post( misspelledLatitudeEntityData );
            fail("System allowed misspelled location property - latitudee, which it should not");
        }
        catch ( UniformInterfaceException e ) {
            //verify the correct error was returned
            JsonNode nodeError = mapper.readTree( e.getResponse().getEntity( String.class ));
            assertEquals( "illegal_argument", nodeError.get( "error" ).textValue() );
        }

        // 2. misspell longitude
        Map<String, Double> misspelledLongitude = hashMap("latitude", lat);
        misspelledLongitude.put( "longitudee", lon );
        Map<String, Object> misspelledLongitudeEntityData = new HashMap<String, Object>();
        misspelledLongitudeEntityData.put( "location", misspelledLongitude );

        try {
            node = context.collection( collectionType ).post( misspelledLongitudeEntityData );
            fail("System allowed misspelled location property - longitudee, which it should not");
        }
        catch ( UniformInterfaceException e ) {
            //verify the correct error was returned
            JsonNode nodeError = mapper.readTree( e.getResponse().getEntity( String.class ));
            assertEquals( "illegal_argument", nodeError.get( "error" ).textValue() );
        }

    }


    /**
     * Test exceptions for entities with poorly created geo points
     * 1. pass only one point instead of two
     * 2. pass a values that are not doubles
     */
    @Test
    public void createEntitiesWithBadPoints() throws IOException {

        String collectionType = "stores";
        JsonNode node = null;
        Double lat = 37.776753;
        Double lon = -122.407846;

        // 1. pass only one point instead of two
        Map<String, Double> latitudeOnly = hashMap("latitude", lat);
        Map<String, Object> latitudeOnlyEntityData = new HashMap<String, Object>();
        latitudeOnlyEntityData.put( "location", latitudeOnly );

        try {
            node = context.collection( collectionType ).post( latitudeOnlyEntityData );
            fail("System allowed location with only one point, latitude, which it should not");
        }
        catch ( UniformInterfaceException e ) {
            //verify the correct error was returned
            JsonNode nodeError = mapper.readTree( e.getResponse().getEntity( String.class ));
            assertEquals( "illegal_argument", nodeError.get( "error" ).textValue() );
        }

        // 2. pass a values that are not doubles
        Map<String, String> notDoubleLatLon = hashMap("latitude", "fred");
        notDoubleLatLon.put( "longitude", "barney" );
        Map<String, Object> notDoubleLatLonEntityData = new HashMap<String, Object>();
        notDoubleLatLonEntityData.put( "location", notDoubleLatLon );

        try {
            node = context.collection( collectionType ).post( notDoubleLatLonEntityData );
            fail("System allowed misspelled location values that are not doubles for latitude and longitude, which it should not");
        }
        catch ( UniformInterfaceException e ) {
            //verify the correct error was returned
            JsonNode nodeError = mapper.readTree( e.getResponse().getEntity( String.class ));
            assertEquals( "illegal_argument", nodeError.get( "error" ).textValue() );
        }


    }

}
