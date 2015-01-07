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


import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class AndOrQueryTest extends AbstractRestIT {
  /**
   * Ensure limit is respected in queries
   * 1. Insert a number of entities
   * 2. Set half the entities to have a 'madeup' property of 'true'
   * and set the other half to 'false'
   * 3. Query all entities where "madeup = true"
   * 4. Limit the query to half of the number of entities
   * 5. Ensure the correct entities are returned
   *
   * @throws IOException
   */
  @Test //USERGRID-900
  public void queriesWithAndPastLimit() throws IOException {
    int numValuesTested = 40;
    long created = 0;

    Entity actor = new Entity();
    actor.put("displayName", "Erin");
    Entity props = new Entity();
    props.put("actor", actor);
    props.put("verb", "go");
    props.put("content", "bragh");
    //1. Insert a number of entities
    for (int i = 0; i < numValuesTested; i++) {
      //2. Set half the entities to have a 'madeup' property of 'true'
      // and set the other half to 'false'
      if (i < numValuesTested / 2) {
        props.put("madeup", false);
      } else {
        props.put("madeup", true);
      }

      props.put("ordinal", i);
      Entity activity = this.app().collection("activities").post(props);
      if (i == 0) {
        created = Long.parseLong(activity.get("created").toString());
      }
    }

    this.refreshIndex();
    //3. Query all entities where "madeup = true"
    String errorQuery = "select * where created >= " + created + "AND madeup = true";
    QueryParameters params = new QueryParameters()
        .setQuery(errorQuery)
        .setLimit(numValuesTested / 2);//4. Limit the query to half of the number of entities
    Collection activities = this.app().collection("activities").get(params);
    //5. Ensure the correct entities are returned
    assertEquals(numValuesTested / 2, activities.response.getEntityCount());
    while (activities.hasNext()) {
      assertTrue(Boolean.parseBoolean(activities.next().get("madeup").toString()));
    }
  }


  /**
   * Test negated query
   * 1. Insert a number of entities
   * 2. Set half the entities to have a 'verb' property of 'go'
   * and set the other half to 'stop'
   * 3. Query all entities where "NOT verb = 'go'"
   * 4. Limit the query to half of the number of entities
   * 5. Ensure the returned entities have "verb = 'stop'"
   *
   * @throws IOException
   */
  @Test //USERGRID-1475
  public void negatedQuery() throws IOException {
    int numValuesTested = 20;

    Entity actor = new Entity();
    actor.put("displayName", "Erin");
    Entity props = new Entity();
    props.put("actor", actor);
    props.put("verb", "go");
    props.put("content", "bragh");
    //1. Insert a number of entities
    for (int i = 0; i < numValuesTested; i++) {
      //2. Set half the entities to have a 'verb' property of 'go'
      // and set the other half to 'stop'
      if (i % 2 == 0) {
        props.put("verb", "go");
      } else {
        props.put("verb", "stop");
      }

      props.put("ordinal", i);
      this.app().collection("activities").post(props);
    }


    this.refreshIndex();
    //3. Query all entities where "NOT verb = 'go'"
    String query = "select * where not verb = 'go'";
    //4. Limit the query to half of the number of entities
    QueryParameters params = new QueryParameters().setQuery(query).setLimit(numValuesTested / 2);
    Collection activities = this.app().collection("activities").get(params);
    //5. Ensure the returned entities have "verb = 'stop'"
    assertEquals(numValuesTested / 2, activities.response.getEntityCount());
    while (activities.hasNext()) {
      assertEquals("stop", activities.next().get("verb").toString());
    }


  }

  /**
   * Ensure queries return a subset of entities in the correct order
   * 1. Insert a number of entities
   * 2. Query for a subset of the entities
   * 3. Validate that the correct entities are returned
   *
   * @throws Exception
   */
  @Test //USERGRID-1615
  public void queryReturnCount() throws Exception {
    int numValuesTested = 20;

    Entity actor = new Entity();
    actor.put("displayName", "Erin");
    Entity props = new Entity();
    props.put("actor", actor);
    props.put("verb", "go");
    props.put("content", "bragh");
    //1. Insert a number of entities
    for (int i = 0; i < numValuesTested; i++) {
      props.put("ordinal", i);
      this.app().collection("activities").post(props);
    }
    this.refreshIndex();
    //2. Query for a subset of the entities
    String inCorrectQuery = "select * where ordinal >= 10 order by ordinal asc";
    QueryParameters params = new QueryParameters().setQuery(inCorrectQuery).setLimit(numValuesTested / 2);
    Collection activities = this.app().collection("activities").get(params);
    //3. Validate that the correct entities are returned
    assertEquals(numValuesTested / 2, activities.response.getEntityCount());

    List entities = activities.response.getEntities();
    for (int i = 0; i < numValuesTested / 2; i++) {
      assertEquals(numValuesTested / 2 + i, Integer.parseInt(((LinkedHashMap<String, Object>) entities.get(i)).get("ordinal").toString()));
    }

  }

  /**
   * Validate sort order with AND/OR query
   * 1. Insert entities
   * 2. Use AND/OR query to retrieve entities
   * 3. Verify the order of results
   *
   * @throws Exception
   */
  @Test
  public void queryCheckAsc() throws Exception {
    int numOfEntities = 20;
    String collectionName = "imagination";

    Entity props = new Entity();
    props.put("WhoHelpedYou", "Ruff");
    //1. Insert entities
    for (int i = 0; i < numOfEntities; i++) {
      props.put("ordinal", i);
      this.app().collection(collectionName).post(props);
    }

    this.refreshIndex();

    //2. Use AND/OR query to retrieve entities
    String inquisitiveQuery = "select * where Ordinal gte 0 and Ordinal lte 10 "
        + "or WhoHelpedYou eq 'Ruff' ORDER BY Ordinal asc";
    QueryParameters params = new QueryParameters().setQuery(inquisitiveQuery).setLimit(numOfEntities / 2);
    Collection activities = this.app().collection(collectionName).get(params);

    //3. Verify the order of results
    assertEquals(numOfEntities / 2, activities.response.getEntityCount());
    List entities = activities.response.getEntities();
    for (int i = 0; i < numOfEntities / 2; i++) {
      assertEquals(i, Integer.parseInt(((LinkedHashMap<String, Object>) entities.get(i)).get("ordinal").toString()));
    }
  }


  /**
   * Test a standard query
   * 1. Insert a number of entities
   * 2. Issue a query
   * 3. validate that a full page of (10) entities is returned
   *
   * @throws Exception
   */
  @Test
  public void queryReturnCheck() throws Exception {
    int numOfEntities = 20;
    String collectionName = "imagination";

    Entity props = new Entity();
    props.put("WhoHelpedYou", "Ruff");
    //1. Insert a number of entities
    for (int i = 0; i < numOfEntities; i++) {
      props.put("ordinal", i);
      this.app().collection(collectionName).post(props);
    }

    this.refreshIndex();

    //2. Issue a query
    String inquisitiveQuery = String.format("select * where ordinal >= 0 and ordinal <= %d or WhoHelpedYou = 'Ruff'", numOfEntities);
    QueryParameters params = new QueryParameters().setQuery(inquisitiveQuery);
    Collection activities = this.app().collection(collectionName).get(params);

    //3. validate that a full page of (10) entities is returned
    assertEquals(10, activities.response.getEntityCount());
    List entities = activities.response.getEntities();
    for (int i = 0; i < 10; i++) {
      assertEquals(i, Integer.parseInt(((LinkedHashMap<String, Object>) entities.get(i)).get("ordinal").toString()));
    }
  }

  /**
   * Test a standard query using alphanumeric operators
   * 1. Insert a number of entities
   * 2. Issue a query using alphanumeric operators
   * 3. validate that a full page of (10) entities is returned
   *
   * @throws Exception
   */
  @Test
  public void queryReturnCheckWithShortHand() throws Exception {
    int numOfEntities = 10;
    String collectionName = "imagination";

    Entity props = new Entity();
    props.put("WhoHelpedYou", "Ruff");
    //1. Insert a number of entities
    for (int i = 0; i < numOfEntities; i++) {
      props.put("ordinal", i);
      this.app().collection(collectionName).post(props);
    }

    this.refreshIndex();

    //2. Issue a query using alphanumeric operators
    String inquisitiveQuery = "select * where Ordinal gte 0 and Ordinal lte 2000 or WhoHelpedYou eq 'Ruff'";
    QueryParameters params = new QueryParameters().setQuery(inquisitiveQuery);
    Collection activities = this.app().collection(collectionName).get(params);

    //3. validate that a full page of (10) entities is returned
    assertEquals(10, activities.response.getEntityCount());
    List entities = activities.response.getEntities();
    for (int i = 0; i < 10; i++) {
      assertEquals(i, Integer.parseInt(((LinkedHashMap<String, Object>) entities.get(i)).get("ordinal").toString()));
    }
  }

}
