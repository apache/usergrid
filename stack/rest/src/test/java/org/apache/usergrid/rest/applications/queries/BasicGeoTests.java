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


import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.utils.MapUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;


/**
 * Basic Geo Tests - CRUD entities with geo points, exceptions for malformed calls
 *
 * @author rockerston
 */
public class BasicGeoTests extends AbstractRestIT {


  /**
   * Create a entity with a geo location point in it
   * 1. Create entity
   * 2. Verify that the entity was created
   */
  @Test
  public void createEntityWithGeoLocationPoint() throws IOException {

    Double lat = 37.776753;
    Double lon = -122.407846;

    //1. Create entity
    Entity entity = new Entity();
    entity.put("location", new MapUtils.HashMapBuilder<String, Double>()
        .map("latitude", lat)
        .map("longitude", lon));

    Entity testEntity = this.app().collection("stores").post(entity);

    //2. Verify that the entity was created
    assertNotNull(testEntity);
    assertEquals(lat.toString(), ((HashMap<String, Object>) testEntity.get("location")).get("latitude").toString());
    assertEquals(lon.toString(), ((HashMap<String, Object>) testEntity.get("location")).get("longitude").toString());

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
    Double lat = 37.776753;
    Double lon = -122.407846;

    //1. create an entity with a geo point
    Entity entity = new Entity();
    entity.put("name", entityName);
    entity.put("location", new MapUtils.HashMapBuilder<String, Double>()
        .map("latitude", lat)
        .map("longitude", lon));

    Entity testEntity = this.app().collection(collectionType).post(entity);

    assertNotNull(testEntity);
    assertEquals(lat.toString(), ((HashMap<String, Object>) testEntity.get("location")).get("latitude").toString());
    assertEquals(lon.toString(), ((HashMap<String, Object>) testEntity.get("location")).get("longitude").toString());

//        context.refreshIndex();

    //2. read back that entity make sure it is accurate
    testEntity = this.app().collection(collectionType).entity(testEntity).get();

    assertNotNull(testEntity);
    assertEquals(lat.toString(), ((HashMap<String, Object>) testEntity.get("location")).get("latitude").toString());
    assertEquals(lon.toString(), ((HashMap<String, Object>) testEntity.get("location")).get("longitude").toString());

    //3. update the geo point to a new value
    Double newLat = 35.776753;
    Double newLon = -119.407846;
    testEntity.put("location", new MapUtils.HashMapBuilder<String, Double>()
        .map("latitude", newLat)
        .map("longitude", newLon));

    testEntity = this.app().collection(collectionType).entity(testEntity).put(testEntity);

    assertNotNull(testEntity);
    assertEquals(newLat.toString(), ((HashMap<String, Object>) testEntity.get("location")).get("latitude").toString());
    assertEquals(newLon.toString(), ((HashMap<String, Object>) testEntity.get("location")).get("longitude").toString());

    //4. read back the updated entity, make sure it is accurate
    testEntity = this.app().collection(collectionType).entity(testEntity).get();

    assertNotNull(testEntity);
    assertEquals(newLat.toString(), ((HashMap<String, Object>) testEntity.get("location")).get("latitude").toString());
    assertEquals(newLon.toString(), ((HashMap<String, Object>) testEntity.get("location")).get("longitude").toString());


  }

  /**
   * Test exceptions for entities with poorly created geo points
   * 1. misspell latitude
   * 2. misspell longitude
   */
  @Test
  public void createEntitiesWithBadSpelling() throws IOException {

    String collectionType = "stores";
    Double lat = 37.776753;
    Double lon = -122.407846;

    // 1. misspell latitude
    Entity entity = new Entity();
    entity.put("location", new MapUtils.HashMapBuilder<String, Double>()
        .map("latitudee", lat) //intentionally misspelled
        .map("longitude", lon));
    try {
      this.app().collection(collectionType).post(entity);
      fail("System allowed misspelled location property - latitudee, which it should not");
    } catch (UniformInterfaceException e) {
      //verify the correct error was returned
      JsonNode nodeError = mapper.readTree(e.getResponse().getEntity(String.class));
      assertEquals("illegal_argument", nodeError.get("error").textValue());
    }

    // 2. misspell longitude
    entity.put("location", new MapUtils.HashMapBuilder<String, Double>()
        .map("latitude", lat)
        .map("longitudee", lon)); //intentionally misspelled

    try {
      this.app().collection(collectionType).post(entity);
      fail("System allowed misspelled location property - longitudee, which it should not");
    } catch (UniformInterfaceException e) {
      //verify the correct error was returned
      JsonNode nodeError = mapper.readTree(e.getResponse().getEntity(String.class));
      assertEquals("illegal_argument", nodeError.get("error").textValue());
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
    Double lat = 37.776753;

    // 1. pass only one point instead of two
    Entity entity = new Entity();
    entity.put("location", new MapUtils.HashMapBuilder<String, Double>()
        .map("latitude", lat));

    try {
      this.app().collection(collectionType).post(entity);
      fail("System allowed location with only one point, latitude, which it should not");
    } catch (UniformInterfaceException e) {
      //verify the correct error was returned
      JsonNode nodeError = mapper.readTree(e.getResponse().getEntity(String.class));
      assertEquals("illegal_argument", nodeError.get("error").textValue());
    }

    // 2. pass a values that are not doubles
    entity.put("location", new MapUtils.HashMapBuilder<String, String>()
        .map("latitude", "fred")
        .map("longitude", "barney"));

    try {
      this.app().collection(collectionType).post(entity);
      fail("System allowed misspelled location values that are not doubles for latitude and longitude, which it should not");
    } catch (UniformInterfaceException e) {
      //verify the correct error was returned
      JsonNode nodeError = mapper.readTree(e.getResponse().getEntity(String.class));
      assertEquals("illegal_argument", nodeError.get("error").textValue());
    }
  }
}
