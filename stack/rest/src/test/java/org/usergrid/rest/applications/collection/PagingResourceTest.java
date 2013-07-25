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
package org.usergrid.rest.applications.collection;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.usergrid.utils.MapUtils.hashMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.usergrid.java.client.entities.Entity;
import org.usergrid.java.client.response.ApiResponse;
import org.usergrid.rest.RestContextTest;
import org.usergrid.rest.test.resource.CustomCollection;
import org.usergrid.rest.test.resource.app.CustomEntity;

/**
 * Simple tests to test querying at the REST tier
 */
public class PagingResourceTest extends RestContextTest {

  @Test
  public void collectionPaging() throws Exception {

    CustomCollection things = context.application().collection("things");

    int size = 40;

    List<Map<String, String>> created = new ArrayList<Map<String, String>>(size);

    for (int i = 0; i < size; i++) {
      Map<String, String> entity = hashMap("name", String.valueOf(i));
      things.create(entity);

      created.add(entity);
    }

    // now page them all
    ApiResponse response = null;
    Iterator<Map<String, String>> entityItr = created.iterator();

    do {

      response = parse(things.get());

      for (Entity e : response.getEntities()) {
        assertTrue(entityItr.hasNext());
        assertEquals(entityItr.next().get("name"), e.getProperties().get("name").asText());
      }

      things = things.withCursor(response.getCursor());
    } while (response != null && response.getCursor() != null);

    // we paged them all
    assertFalse(entityItr.hasNext());

  }

  @Test
  public void startPaging() throws Exception {

    CustomCollection things = context.application().collection("things");

    int size = 40;

    List<Map<String, String>> created = new ArrayList<Map<String, String>>(size);

    for (int i = 0; i < size; i++) {
      Map<String, String> entity = hashMap("name", String.valueOf(i));
      things.create(entity);

      created.add(entity);
    }

    // now page them all
    ApiResponse response = null;

    UUID start = null;
    int index = 0;

    do {

      response = parse(things.get());

      for (Entity e : response.getEntities()) {
        assertEquals(created.get(index).get("name"), e.getProperties().get("name").asText());
        index++;
      }

      // decrement since we'll get this one again
      index--;

      start = response.getEntities().get(response.getEntities().size() - 1).getUuid();

      things = things.withStart(start);
    } while (response != null && response.getEntities().size() > 1);

    // we paged them all
    assertEquals(created.size() - 1, index);

  }

  @Test
  public void emptyQlandLimitIgnored() throws Exception {

    CustomCollection things = context.application().collection("things");

    Map<String, String> data = hashMap("name", "thing1");
    JsonNode response = things.create(data);

    JsonNode entity = getEntity(response, 0);

    String uuid = entity.get("uuid").asText();

    CustomEntity entityRequest = things.entity("thing1").withParam("ql", "").withParam("limit", "");
    
    JsonNode returnedEntity = getEntity(entityRequest.get(), 0);

    assertEquals(entity, returnedEntity);
    
    entityRequest = things.entity(uuid).withParam("ql", "").withParam("limit", "");

    returnedEntity = getEntity(entityRequest.get(), 0);

    assertEquals(entity, returnedEntity);

    // now do a delete
    returnedEntity = getEntity(entityRequest.delete(), 0);

    assertEquals(entity, returnedEntity);

    // verify it's gone
    returnedEntity = getEntity(things.entity(uuid).get(), 0);

    assertNull(returnedEntity);

  }
  


  private static ObjectMapper mapper = new ObjectMapper();

  private static final ApiResponse parse(JsonNode response) throws Exception {
    return mapper.readValue(response, ApiResponse.class);
  }

}
