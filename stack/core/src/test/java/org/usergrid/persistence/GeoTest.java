/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.cassandra.GeoIndexManager;
import org.usergrid.persistence.cassandra.GeoIndexManager.EntityLocationRef;
import org.usergrid.utils.MapUtils;

import com.beoui.geocell.SearchResults;
import com.beoui.geocell.model.Point;

public class GeoTest extends AbstractPersistenceTest {

  private static final Logger logger = LoggerFactory.getLogger(GeoTest.class);

  public GeoTest() {
    super();
  }

  @Test
  public void testGeo() throws Exception {
    logger.info("GeoTest.testGeo");

    UUID applicationId = createApplication("testOrganization", "testGeo");
    assertNotNull(applicationId);

    EntityManager em = emf.getEntityManager(applicationId);
    assertNotNull(em);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put("username", "edanuff");
    properties.put("email", "ed@anuff.com");

    Entity user = em.create("user", properties);
    assertNotNull(user);

    EntityLocationRef loc = new EntityLocationRef(user, 37.776753, -122.407846);
    GeoIndexManager geo = em.getGeoIndexManager();
    geo.storeLocationInCollectionIndex(em.getApplicationRef(), "users", user.getUuid(), "location.coordinates", loc);

    Point center = new Point(37.774277, -122.404744);
    SearchResults<EntityLocationRef> searchResults = geo.proximitySearchCollection(em.getApplicationRef(), "users",
        "location.coordinates", center, 0, 200, GeoIndexManager.MAX_RESOLUTION, 10);

    List<EntityLocationRef> listResults = searchResults.getResults();

    assertEquals(0, listResults.size());

    searchResults = geo.proximitySearchCollection(em.getApplicationRef(), "users", "location.coordinates", center, 0,
        400, GeoIndexManager.MAX_RESOLUTION, 10);

    listResults = searchResults.getResults();

    this.dump(listResults);

    assertEquals(1, listResults.size());

    geo.removeLocationFromCollectionIndex(em.getApplicationRef(), "users", "location.coordinates", loc);

    searchResults = geo.proximitySearchCollection(em.getApplicationRef(), "users", "location.coordinates", center, 0,
        400, GeoIndexManager.MAX_RESOLUTION, 10);

    listResults = searchResults.getResults();

    this.dump(listResults);

    assertEquals(0, listResults.size());

    updatePos(em, user, 37.426373, -122.14108);

    center = new Point(37.774277, -122.404744);

    searchResults = geo.proximitySearchCollection(em.getApplicationRef(), "users", "location.coordinates", center, 0,
        200, GeoIndexManager.MAX_RESOLUTION, 10);

    listResults = searchResults.getResults();

    assertEquals(0, listResults.size());

    updatePos(em, user, 37.774277, -122.404744);

    center = new Point(37.776753, -122.407846);

    searchResults = geo.proximitySearchCollection(em.getApplicationRef(), "users", "location.coordinates", center, 0,
        1000, GeoIndexManager.MAX_RESOLUTION, 10);

    listResults = searchResults.getResults();

    assertEquals(1, listResults.size());

    // check at globally large distance
    searchResults = geo.proximitySearchCollection(em.getApplicationRef(), "users", "location.coordinates", center,
        0, Integer.MAX_VALUE, GeoIndexManager.MAX_RESOLUTION, 10);

    listResults = searchResults.getResults();

    assertEquals(1, listResults.size());

    // create a new entity so we have 2
    LinkedHashMap<String, Object> properties2 = new LinkedHashMap<String, Object>();
    properties2.put("username", "sganyo");
    properties2.put("email", "sganyo@anuff.com");
    Entity user2 = em.create("user", properties2);
    assertNotNull(user2);
    EntityLocationRef loc2 = new EntityLocationRef(user2, 31.1, 121.2);
    geo.storeLocationInCollectionIndex(em.getApplicationRef(), "users", user2.getUuid(), "location.coordinates", loc2);

    // check at 10000m distance
    searchResults = geo.proximitySearchCollection(em.getApplicationRef(), "users", "location.coordinates", center,
        0, 10000, GeoIndexManager.MAX_RESOLUTION, 10);

    listResults = searchResults.getResults();

    assertEquals(1, listResults.size());

    // check at globally large distance
    searchResults = geo.proximitySearchCollection(em.getApplicationRef(), "users", "location.coordinates", center,
        0, Integer.MAX_VALUE, GeoIndexManager.MAX_RESOLUTION, 10);

    listResults = searchResults.getResults();
    assertEquals(2, listResults.size());

    // check at globally large distance (center point close to other entity)
    center = new Point(31.14, 121.27);

    searchResults = geo.proximitySearchCollection(em.getApplicationRef(), "users", "location.coordinates", center,
        0, Integer.MAX_VALUE, GeoIndexManager.MAX_RESOLUTION, 10);

    listResults = searchResults.getResults();

    assertEquals(2, listResults.size());

    Results emSearchResults = em.searchCollection(em.getApplicationRef(), "users",
        Query.fromQL("location within 1000 of 37.776753, -122.407846"));
    assertEquals(1, emSearchResults.size());

    updatePos(em, user, 37.776753, -122.407846);

    center = new Point(37.428526, -122.140916);

    searchResults = geo.proximitySearchCollection(em.getApplicationRef(), "users", "location.coordinates", center,
        0, 1000, GeoIndexManager.MAX_RESOLUTION, 10);

    listResults = searchResults.getResults();

    assertEquals(0, listResults.size());

    emSearchResults = em.searchCollection(em.getApplicationRef(), "users",
        Query.fromQL("location within 1000 of 37.428526, -122.140916"));
    assertEquals(0, emSearchResults.size());

    properties = new LinkedHashMap<String, Object>();
    properties.put("name", "Brickhouse");
    properties.put("address", "426 Brannan Street");
    properties.put("location", getLocation(37.779632, -122.395131));

    Entity restaurant = em.create("restaurant", properties);
    assertNotNull(restaurant);

    em.createConnection(user, "likes", restaurant);

    emSearchResults = em.searchConnectedEntities(user, Query.fromQL("location within 2000 of 37.776753, -122.407846"));
    assertEquals(1, emSearchResults.size());

    emSearchResults = em.searchConnectedEntities(user, Query.fromQL("location within 1000 of 37.776753, -122.407846"));
    assertEquals(0, emSearchResults.size());

  }

  @Test
  public void testPointPaging() throws Exception {

    UUID applicationId = createApplication("testOrganization", "testPointPaging");
    assertNotNull(applicationId);

    EntityManager em = emf.getEntityManager(applicationId);
    assertNotNull(em);

    // save objects in a diagonal line from -90 -180 to 90 180

    int numEntities = 500;

    float minLattitude = -90;
    float maxLattitude = 90;
    float minLongitude = -180;
    float maxLongitude = 180;

    float lattitudeDelta = (maxLattitude - minLattitude) / numEntities;

    float longitudeDelta = (maxLongitude - minLongitude) / numEntities;

    for (int i = 0; i < numEntities; i++) {
      float lattitude = minLattitude + lattitudeDelta * i;
      float longitude = minLongitude + longitudeDelta * i;

      Map<String, Float> location = MapUtils.hashMap("latitude", lattitude).map("longitude", longitude);

      Map<String, Object> data = new HashMap<String, Object>(2);
      data.put("name", String.valueOf(i));
      data.put("location", location);

      em.create("store", data);
    }

    Query query = new Query();
    // earth's circumference is 40,075 kilometers. Up it to 50,000kilometers
    // just to be save
    query.addFilter("location within 50000000 of -90, -180");
    query.setLimit(100);

    int count = 0;
    Results results;
    
    do {
      results = em.searchCollection(em.getApplicationRef(), "stores", query);

      for (Entity entity : results.getEntities()) {
        assertEquals(String.valueOf(count), entity.getName());
        count++;
      }

      //set for the next "page"
      query.setCursor(results.getCursor());
    } while (results.getCursor() != null);

    // check we got back all 500 entities
    assertEquals(numEntities, count);
  }

  @Test
  public void testDistanceByLimit() throws Exception {

    UUID applicationId = createApplication("testOrganization", "testDistanceByLimit");
    assertNotNull(applicationId);

    EntityManager em = emf.getEntityManager(applicationId);
    assertNotNull(em);

    // save objects in a diagonal line from -90 -180 to 90 180

    int numEntities = 100;

    float minLattitude = -90;
    float maxLattitude = 90;
    float minLongitude = -180;
    float maxLongitude = 180;

    float lattitudeDelta = (maxLattitude - minLattitude) / numEntities;

    float longitudeDelta = (maxLongitude - minLongitude) / numEntities;

    for (int i = 0; i < numEntities; i++) {
      float lattitude = minLattitude + lattitudeDelta * i;
      float longitude = minLongitude + longitudeDelta * i;

      Map<String, Float> location = MapUtils.hashMap("latitude", lattitude).map("longitude", longitude);

      Map<String, Object> data = new HashMap<String, Object>(2);
      data.put("name", String.valueOf(i));
      data.put("location", location);

      em.create("store", data);
    }

    Query query = new Query();
    // earth's circumference is 40,075 kilometers. Up it to 50,000kilometers
    // just to be save
    query.addFilter("location within 0 of -90, -180");
    query.setLimit(100);

    int count = 0;

    do {
      Results results = em.searchCollection(em.getApplicationRef(), "stores", query);

      for (Entity entity : results.getEntities()) {
        assertEquals(String.valueOf(count), entity.getName());
        count++;
      }

    } while (query.getCursor() != null);

    // check we got back all 500 entities
    assertEquals(numEntities, count);
  }

  public Map<String, Object> getLocation(double latitude, double longitude) throws Exception {
    Map<String, Object> latlong = new LinkedHashMap<String, Object>();
    latlong.put("latitude", latitude);
    latlong.put("longitude", longitude);
    return latlong;
  }

  public void updatePos(EntityManager em, EntityRef entity, double latitude, double longitude) throws Exception {
    Map<String, Object> latlong = new LinkedHashMap<String, Object>();
    latlong.put("latitude", latitude);
    latlong.put("longitude", longitude);

    em.setProperty(entity, "location", latlong);
  }
}
