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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.apache.usergrid.utils.MapUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class GeoPagingTest extends AbstractRestIT {
  private static Logger log = LoggerFactory.getLogger(GeoPagingTest.class);

  /**
   * Tests the ability to query groups by location
   * 1. Create several groups
   * 2. Query the groups from a nearby location, restricting the search
   * by creation time to a single entity where created[i-1] < created[i] < created[i+1]
   * 3. Verify that the desired entity i, and only the desired entity, is returned
   *
   * @throws IOException
   */
  @Test //("Test uses up to many resources to run reliably") // USERGRID-1403
  public void groupQueriesWithGeoPaging() throws IOException {

    int maxRangeLimit = 2000;
    long[] index = new long[maxRangeLimit];
    Double lat = 37.0;
    Double lon = -75.0;

    //Create our base entity template
    Entity actor = new Entity();
    actor.put("displayName", "Erin");
    actor.put("location", new MapUtils.HashMapBuilder<String, Double>()
        .map("latitude", lat)
        .map("longitude", lon));
    Entity props = new Entity();
    props.put("actor", actor);
    props.put("verb", "go");
    props.put("content", "bragh");
    // 1. Create several groups
    // Modifying the path will cause a new group to be created
    for (int i = 0; i < 5; i++) {
      String newPath = String.format("/kero" + i);
      props.put("path", newPath);
      props.put("ordinal", i);
      //save the entity
      Entity activity = this.app().collection("groups").post(props);
      //retrieve it again from the database
      activity = this.app().collection("groups").entity(activity).get();
      index[i] = (Long) activity.get("created");
      log.debug("Activity {} created at {}", i, index[i]);

    }
    this.refreshIndex();
    // 2. Query the groups from a nearby location, restricting the search
    //    by creation time to a single entity where created[i-1] < created[i] < created[i+1]
      //since this geo location is contained by an actor it needs to be actor.location.
    String query = "select * where actor.location within 20000 of 37.0,-75.0 "
        + " and created > " + (index[0])
        + " and created < " + (index[2])
        + " order by created";
    QueryParameters params = new QueryParameters();
    params.setQuery(query);
    Collection collection = this.app().collection("groups").get(params);
    assertEquals("Query should have returned 1 entity", 1, collection.getResponse().getEntityCount());
    Entity entity = collection.next();
    // 3. Verify that the desired entity i, and only the desired entity, is returned
    assertNotNull("Query should have returned 1 entity", entity);
    assertEquals(index[1], Long.parseLong(entity.get("created").toString()));
    assertFalse("Query should have returned only 1 entity", collection.hasNext());
    try {
      entity = collection.next();
      fail("Query should have returned only 1 entity");
    } catch (NoSuchElementException nse) {
      //We're expecting a NoSuchElementException. This is good news, so no need to do
      //anything with the exception
    }

  }


  /**
   * Creates a store then queries to check ability to find different store from up to 40 mil meters away
   * 1. Create 2 entities
   * 2. Query from a short distance of the center point to ensure that none are returned
   * 3. Query within a huge distance of the center point to ensure that both are returned
   */
  @Test
  public void testFarAwayLocationFromCenter() throws IOException {
    String collectionType = "testFarAwayLocation" + UUIDUtils.newTimeUUID();

      final double lat = 37.776753;
      final double lon =  -122.407846;

    QueryParameters queryClose = new QueryParameters();
    queryClose.setQuery("select * where location within 20000 of " + String.valueOf(lat) + ", " + String.valueOf(lon) + "");
    QueryParameters queryFar = new QueryParameters();
    queryFar.setQuery("select * where location within " + Integer.MAX_VALUE + " of " + String.valueOf(lat) + ", " + String.valueOf(lon) + "");
    // 1. Create 2 entities
    Entity props = new Entity();
    props.put("name", "usergrid");
    props.put("location", new MapUtils.HashMapBuilder<String, Double>()
        .map("latitude", -33.746369)
        .map("longitude", 150.952183));
    this.app().collection(collectionType).post(props);

    Entity props2 = new Entity();
    props2.put("name", "usergrid2");
    props2.put("location", new MapUtils.HashMapBuilder<String, Double>()
        .map("latitude", -33.889058)
        .map("longitude", 151.124024));
    this.app().collection(collectionType).post(props2);
    this.refreshIndex();

    Collection collection = this.app().collection(collectionType).get();
    assertEquals("Should return both entities", 2, collection.getResponse().getEntityCount());
    // 2. Query within a short distance of the center point to ensure that none are returned
    collection = this.app().collection(collectionType).get(queryClose);
    assertEquals("Results from nearby, should return nothing", 0, collection.getResponse().getEntityCount());
    // 3. Query within a huge distance of the center point to ensure that both are returned
    collection = this.app().collection(collectionType).get(queryFar);
    assertEquals("Results from center point to ridiculously far", 2, collection.getResponse().getEntityCount());
  }

  /**
   * Test that geo-query returns co-located entities in expected order.
   */
  @Test // USERGRID-1401
  public void groupQueriesWithConsistentResults() throws IOException {

    int maxRangeLimit = 20;
    Entity[] cats = new Entity[maxRangeLimit];

    // 1. Create several entities
    for (int i = 0; i < 20; i++) {
      Entity cat = new Entity();
      cat.put("name", "cat" + i);
      cat.put("location", new MapUtils.HashMapBuilder<String, Double>()
          .map("latitude", 37.0)
          .map("longitude", -75.0));
      cat.put("ordinal", i);
      cats[i] = cat;
      this.app().collection("cats").post(cat);
    }
    this.refreshIndex();

    QueryParameters params = new QueryParameters();
    for (int consistent = 0; consistent < 20; consistent++) {

      // 2. Query a subset of the entities
      String query = String.format(
          "select * where location within 100 of 37, -75 and ordinal >= %s and ordinal < %s",
          cats[7].get("ordinal"), cats[10].get("ordinal"));
      params.setQuery(query);
      Collection collection = this.app().collection("cats").get(params);

      assertEquals(3, collection.getResponse().getEntityCount());
      List entities = collection.getResponse().getEntities();

      // 3. Test that the entities were returned in the order expected
      for (int i = 0; i > 3; i++) {

        // shouldn't start at 10 since you're excluding it above in the query, it should return 9,8,7
        Entity entity = (Entity)entities.get(i);
        Entity savedEntity = cats[10 - i];
        assertEquals(savedEntity.get("ordinal"), entity.get("ordinal"));
      }
    }
  }


  /**
   * Creates a store right on top of the center store and checks to see if we can find that store, then find both
   * stores.
   * 1. Create 2 entities
   * 2. Query from the center point to ensure that one is returned
   * 3. Query within a huge distance of the center point to ensure that both are returned
   */
  @Test
  public void testFarAwayLocationWithOneResultCloser() throws IOException {
    String collectionType = "testFarAwayLocationWithOneResultCloser" + UUIDUtils.newTimeUUID();
    final double lat = -33.746369;
      final double lon =  150.952183;

    QueryParameters queryClose = new QueryParameters();
    queryClose.setQuery("select * where location within 10000 of " + String.valueOf(lat) + ", " + String.valueOf(lon) + "");
    QueryParameters queryFar = new QueryParameters();
    queryFar.setQuery("select * where location within " + Integer.MAX_VALUE + " of " + String.valueOf(lat) + ", " + String.valueOf(lon) + "");
    // 1. Create 2 entities
    Entity props = new Entity();
    props.put("name", "usergrid");
    props.put("location", new MapUtils.HashMapBuilder<String, Double>()
        .map("latitude", -33.746369)
        .map("longitude", 150.952183));
    this.app().collection(collectionType).post(props);

    Entity props2 = new Entity();
    props2.put("name", "usergrid2");
    props2.put("location", new MapUtils.HashMapBuilder<String, Double>()
        .map("latitude", -33.889058)
        .map("longitude", 151.124024));
    this.app().collection(collectionType).post(props2);
    this.refreshIndex();

    // 2. Query from the center point to ensure that one is returned
    Collection collection = this.app().collection(collectionType).get(queryClose);
    assertEquals("Results from nearby, should return 1 store", 1, collection.getResponse().getEntityCount());

    // 3. Query within a huge distance of the center point to ensure that both are returned
    collection = this.app().collection(collectionType).get(queryFar);
    assertEquals("Results from center point to ridiculously far", 2, collection.getResponse().getEntityCount());
  }


  /**
   * Creates two users, then a matrix of coordinates, then checks to see if any of the coordinates are near our users
   * 1. Create 2 users
   * 2. Create a list of geo points
   * 3. Test each to ensure it is not within 10000 meters of our users
   *
   * @throws IOException
   */
  @Test
  public void createHugeMatrixOfCoordinates() throws IOException {

    //1. Create 2 users
    Entity props = new Entity();
    props.put("username", "norwest");
    props.put("displayName", "norwest");
    props.put("email", "norwest@usergrid.com");
    props.put("location", new MapUtils.HashMapBuilder<String, Double>()
        .map("latitude", -33.746369)
        .map("longitude", 150.952183));
    this.app().collection("users").post(props);
    props.put("username", "ashfield");
    props.put("displayName", "ashfield");
    props.put("email", "ashfield@usergrid.com");
    props.put("location", new MapUtils.HashMapBuilder<String, Double>()
        .map("latitude", -33.746369)
        .map("longitude", 150.952183));
    this.app().collection("users").post(props);

    this.refreshIndex();
    // 2. Create a list of geo points
    List<double[]> points = new ArrayList<>();
    points.add(new double []{33.746369, -89});//Woodland, MS
    points.add(new double []{33.746369, -91});//Beulah, MS
    points.add(new double []{-1.000000, 102.000000});//Somewhere in Indonesia
    points.add(new double []{-90.000000, 90.000000});//Antarctica
    points.add(new double []{90, 90});//Santa's house

    // 3. Test each to ensure it is not within 10000 meters of our users
    Iterator<double[]> pointIterator = points.iterator();
    for ( double[] p = pointIterator.next(); pointIterator.hasNext(); p = pointIterator.next()) {


      String query = "select * where location within 10000 of " + p[0] + ", " + p[1];//locationQuery( 10000 ,center );
      QueryParameters params = new QueryParameters();
      params.setQuery(query);
      Collection collection = this.app().collection("users").get(params);
      assertEquals("Expected 0 results", 0, collection.getResponse().getEntityCount());
    }
  }
}
