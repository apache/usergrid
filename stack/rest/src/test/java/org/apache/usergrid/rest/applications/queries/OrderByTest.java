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


import org.apache.commons.lang.ArrayUtils;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;


/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class OrderByTest extends AbstractRestIT {
  private static Logger log = LoggerFactory.getLogger(OrderByTest.class);

  /**
   * Inserts a number of entities. Query a subset of entities
   * with unspecified 'order by' and then ordered by 'created'
   * to ensure the result is unchanged.
   * 1. Insert entities
   * 2. Query without 'order by'
   * 3. Query with 'order by'
   * 4. Ensure the same entities are returned
   * @throws IOException
   */
  @Test
  // USERGRID-1400
  public void orderByShouldNotAffectResults() throws IOException {

    long created = 0;
    Entity actor = new Entity();
    actor.put("displayName", "Erin");
    Entity props = new Entity();
    props.put("actor", actor);
    props.put("verb", "go");
    props.put("content", "bragh");
    //1. Insert entities
    for (int i = 0; i < 20; i++) {
      props.put("ordinal", i);
      Entity activity = this.app().collection("activity").post(props);
      log.info("Created", activity.get("created").toString());
      if (i == 5) {
        created = Long.parseLong(activity.get("created").toString());
      }
    }

    refreshIndex();
    //2. Query without 'order by'
    String query = "select * where created > " + created;
    QueryParameters params = new QueryParameters().setQuery(query);
    Collection activitiesWithoutOrderBy = this.app().collection("activities").get(params);
    assertEquals(10, activitiesWithoutOrderBy.response.getEntityCount());
    //3. Query with 'order by'
    query = query + " order by created desc";
    params.setQuery(query);
    Collection activitiesWithOrderBy = this.app().collection("activities").get(params);
    assertEquals(10, activitiesWithOrderBy.response.getEntityCount());
    //4. Ensure the same entities are returned
    while(activitiesWithoutOrderBy.hasNext() && activitiesWithOrderBy.hasNext()) {
      Entity activityWithoutOrderBy = activitiesWithoutOrderBy.next();
      Entity activityWithOrderBy = activitiesWithOrderBy.next();
      assertEquals(activityWithoutOrderBy.get("uuid").toString(), activityWithOrderBy.get("uuid").toString());
    }
  }


  /**
   * Ensure successful query when 'limit' and 'order by' are specified
   * 1. Insert entities
   * 2. Query a subset of the entities, specifying order and limit
   * 3. Ensure the correct number of results are returned
   * @throws IOException
   */
  @Test
  // USERGRID-1520
  public void orderByComesBeforeLimitResult() throws IOException {

    Entity actor = new Entity();
    actor.put("displayName", "Erin");
    Entity props = new Entity();
    props.put("actor", actor);
    props.put("verb", "go");
    props.put("content", "bragh");
    //1. Insert entities
    for (int i = 0; i < 20; i++) {
      props.put("ordinal", i);
      this.app().collection("activity").post(props);
    }

    refreshIndex();
    //2. Query a subset of the entities, specifying order and limit
    String query = "select * where created > " + 1 + " order by created desc";
    QueryParameters params = new QueryParameters().setQuery(query).setLimit(5);
    Collection activities = this.app().collection("activities").get(params);
    //3. Ensure the correct number of results are returned
    assertEquals(5, activities.response.getEntityCount());

  }

  /**
   * Ensure that results are returned in the correct descending order, when specified
   * 1. Insert a number of entities and add them to an array
   * 2. Query for the entities in descending order
   * 3. Validate that the order is correct
   * @throws IOException
   */
  @Test
  // USERGRID-1521
  public void orderByReturnCorrectResults() throws IOException {

    int size = 20;
    Entity[] activities = new Entity[size];

    Entity actor = new Entity();
    actor.put("displayName", "Erin");
    Entity props = new Entity();
    props.put("actor", actor);
    props.put("verb", "go");
    props.put("content", "bragh");
    //1. Insert a number of entities and add them to an array
    for (int i = 0; i < size; i++) {
      props.put("ordinal", i);
      Entity e = this.app().collection("activity").post(props);
      activities[i] = e;
      log.info(String.valueOf(e.get("uuid").toString()));
      log.info(String.valueOf(Long.parseLong(activities[0].get("created").toString())));
    }

    refreshIndex();


     ArrayUtils.reverse(activities);
    long lastCreated = Long.parseLong(activities[0].get("created").toString());
    //2. Query for the entities in descending order
    String errorQuery = String.format("select * where created <= %d order by created desc", lastCreated);
    String cursor = null;
    int index = size - 1;

    QueryParameters params = new QueryParameters().setQuery(errorQuery);
    Collection activitiesResponse = this.app().collection("activities").get(params);
    //3. Validate that the order is correct
    do {
      int returnSize = activitiesResponse.response.getEntityCount();
      //loop through the current page of results
      for (int i = 0; i < returnSize; i++, index--) {
        assertEquals(((LinkedHashMap<String, Object>) activitiesResponse.response.getEntities().get(i)).get("uuid").toString(),
            ((Entity) activities[index]).get("uuid").toString());
      }
      //grab the next page of results
      activitiesResponse = this.app().getNextPage(activitiesResponse, true);
    }
    while (activitiesResponse.getCursor() != null);
  }
}
