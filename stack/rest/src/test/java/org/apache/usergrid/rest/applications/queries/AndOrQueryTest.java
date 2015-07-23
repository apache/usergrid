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


import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;


/**
 * // TODO: Document this
 *
 * @since 4.0
 */
public class AndOrQueryTest extends QueryTestBase {
    private static Logger log = LoggerFactory.getLogger(AndOrQueryTest.class);


    /**
     * Test an inclusive AND query to ensure the correct results are returned
     *
     * @throws IOException
     */
    @Test
    public void queryAndInclusive() throws IOException {
        int numOfEntities = 20;
        String collectionName = "activities";
        // create our test entities
        generateTestEntities(numOfEntities, collectionName);
        // Query where madeup = true (the last half) and the last quarter of entries
        QueryParameters params = new QueryParameters()
            .setQuery("select * where madeup = true AND ordinal >= " + (numOfEntities - numOfEntities / 4));
        Collection activities = this.app().collection("activities").get(params);
        // results should have madeup = true and ordinal 15-19
        assertEquals(numOfEntities / 4, activities.getResponse().getEntityCount());
        // loop though entities that were returned, and test against the ordinals and values we are
        // expecting, starting with the last entity and decrementing
        int index = 19;
        while (activities.hasNext()) {
            Entity activity = activities.next();
            // ensure the 'madeup' property is set to true
            assertTrue(Boolean.parseBoolean(activity.get("madeup").toString()));
            // make sure the correct ordinal properties are returned
            assertEquals(index--, Long.parseLong(activity.get("ordinal").toString()));
        }

    }
    @Test
    public void someTestProp() throws IOException {
        int numOfEntities = 20;
        String collectionName = "activities";
        // create our test entities
        generateTestEntities(numOfEntities, collectionName);
        // Query where madeup = true (the last half) and the last quarter of entries
        QueryParameters params = new QueryParameters()
            .setQuery("where sometestprop = 'testprop'");
        Collection activities = this.app().collection("activities").get(params);
        // results should have madeup = true and ordinal 15-19
        assertEquals(10, activities.getResponse().getEntityCount());


    }

    @Test
    public void someTestPropPartialContains() throws IOException {
        int numOfEntities = 20;
        String collectionName = "activities";
        // create our test entities
        generateTestEntities(numOfEntities, collectionName);
        // Query where madeup = true (the last half) and the last quarter of entries
        QueryParameters params = new QueryParameters()
            .setQuery("where sometestprop contains 'test*'");
        Collection activities = this.app().collection("activities").get(params);
        // results should have madeup = true and ordinal 15-19
        assertEquals(10, activities.getResponse().getEntityCount());


    }

    /**
     * Test an exclusive AND query to ensure the correct results are returned
     *
     * @throws IOException
     */
    @Test
    public void queryAndExclusive() throws IOException {
        int numOfEntities = 20;
        String collectionName = "activities";

        generateTestEntities(numOfEntities, collectionName);

        //Query where madeup = true (the last half) and NOT the last quarter of entries
        QueryParameters params = new QueryParameters()
            .setQuery("select * where madeup = true AND NOT ordinal >= " + (numOfEntities - numOfEntities / 4));
        Collection activities = this.app().collection("activities").get(params);
        //results should have madeup = true and ordinal 10-14
        assertEquals(numOfEntities / 4, activities.getResponse().getEntityCount());
        // loop though entities that were returned, and test against the ordinals and values we are
        // expecting, starting with the last expected entity and decrementing
        int index = 14;
        while (activities.hasNext()) {
            Entity activity = activities.next();
            //ensure the 'madeup' property is set to true
            assertTrue(Boolean.parseBoolean(activity.get("madeup").toString()));
            //make sure the correct ordinal properties are returned
            assertEquals(index--, Long.parseLong(activity.get("ordinal").toString()));
        }
    }

    /**
     * Test an inclusive OR query to ensure the correct results are returned
     *
     * @throws IOException
     */
    @Test
    public void queryOrInclusive() throws IOException {
        int numOfEntities = 20;
        String collectionName = "activities";

        generateTestEntities(numOfEntities, collectionName);

        //Query where madeup = false (the first half) or the last quarter of entries
        QueryParameters params = new QueryParameters()
            .setQuery("select * where madeup = false OR ordinal >= " + (numOfEntities - numOfEntities / 4))
            .setLimit((numOfEntities));
        Collection activities = this.app().collection("activities").get(params);
        int index = numOfEntities - 1;
        int count = 0;
        int returnSize = activities.getResponse().getEntityCount();
        //loop through the returned results
        for (int i = 0; i < returnSize; i++, index--) {
            count++;
            Entity activity = activities.getResponse().getEntities().get(i);
            log.info(String.valueOf(activity.get("ordinal")) + " " + String.valueOf(activity.get("madeup")));
            //if the entity is in the first half, the property "madeup" should be false
            if (index < numOfEntities / 2) {
                assertFalse(Boolean.parseBoolean(String.valueOf(activity.get("madeup"))));
            }
            //else if the entity is in the second half, the property "madeup" should be true
            else if (index >= (numOfEntities - numOfEntities / 4)) {
                assertTrue(Boolean.parseBoolean(String.valueOf(activity.get("madeup"))));
            }
            //test to ensure that the ordinal is in the first half (where "madeup = false")
            //OR that the ordinal is in the last quarter of the entity list (where "ordinal >=  (numOfEntities - numOfEntities / 4))")
            long ordinal = Long.parseLong(String.valueOf(activity.get("ordinal")));
            assertTrue(ordinal < (numOfEntities / 2) || ordinal >= (numOfEntities - numOfEntities / 4));
        }
        //results should have madeup = false or ordinal 0-9,15-19
        //A total of 15 entities should be returned
        assertEquals(3 * numOfEntities / 4, count);
    }

    /**
     * Test an exclusive OR query to ensure the correct results are returned
     *
     * @throws IOException
     */
    @Test
    public void queryOrExclusive() throws IOException {
        int numOfEntities = 30;
        String collectionName = "activities";

        generateTestEntities(numOfEntities, collectionName);

        //Query where the verb = 'go' (half) OR the last quarter by ordinal, but NOT where verb = 'go' AND the ordinal is in the last quarter
        QueryParameters params = new QueryParameters()
            .setQuery("select * where (verb = 'go' OR ordinal >= " + (numOfEntities - numOfEntities / 4) + ") AND NOT (verb = 'go' AND ordinal >= " + (numOfEntities - numOfEntities / 4) + ")")
            .setLimit((numOfEntities));
        Collection activities = this.app().collection("activities").get(params);

        int index = numOfEntities - 1;
        int count = 0;
        int returnSize = activities.getResponse().getEntityCount();
        for (int i = 0; i < returnSize; i++, index--) {
            count++;
            Entity activity = activities.getResponse().getEntities().get(i);
            long ordinal = Long.parseLong(String.valueOf(activity.get("ordinal")));
            log.info(ordinal + " " + String.valueOf(activity.get("verb")));
            //if the entity is in the first three quarters, the property "verb" should be "go"
            if (ordinal < (numOfEntities - numOfEntities / 4)) {
                assertEquals("go", String.valueOf(activity.get("verb")));
            }
            //if the entity is in the last quarter, the property "verb" should be "stop"
            else if (ordinal >= (numOfEntities - numOfEntities / 4)) {
                assertEquals("stop", String.valueOf(activity.get("verb")));
            }
        }
        //results should be even ordinals in the first 3 quarters and odd ordinals from the last quarter
        //Should return 1 more than half the number of entities
        assertEquals(1 + numOfEntities / 2, count);
    }

    /**
     * Ensure limit is respected in queries
     * 1. Query all entities where "madeup = true"
     * 2. Limit the query to half of the number of entities
     * 3. Ensure the correct entities are returned
     *
     * @throws IOException
     */
    @Test
    public void queryWithAndPastLimit() throws IOException {
        int numValuesTested = 40;

        generateTestEntities(numValuesTested, "activities");
        //3. Query all entities where "madeup = true"
        String errorQuery = "select * where madeup = true";
        QueryParameters params = new QueryParameters()
            .setQuery(errorQuery)
            .setLimit(numValuesTested / 2);//4. Limit the query to half of the number of entities
        Collection activities = this.app().collection("activities").get(params);
        //5. Ensure the correct entities are returned
        assertEquals(numValuesTested / 2, activities.getResponse().getEntityCount());
        while (activities.hasNext()) {
            assertTrue(Boolean.parseBoolean(activities.next().get("madeup").toString()));
        }
    }


    /**
     * Test negated query
     * 1. Query all entities where "NOT verb = 'go'"
     * 2. Limit the query to half of the number of entities
     * 3. Ensure the returned entities have "verb = 'stop'"
     *
     * @throws IOException
     */
    @Test
    public void queryNegated() throws IOException {
        int numValuesTested = 20;

        generateTestEntities(numValuesTested, "activities");
        //1. Query all entities where "NOT verb = 'go'"
        String query = "select * where not verb = 'go'";
        //2. Limit the query to half of the number of entities
        QueryParameters params = new QueryParameters().setQuery(query).setLimit(numValuesTested / 2);
        Collection activities = this.app().collection("activities").get(params);
        //3. Ensure the returned entities have "verb = 'stop'"
        assertEquals(numValuesTested / 2, activities.getResponse().getEntityCount());
        while (activities.hasNext()) {
            assertEquals("stop", activities.next().get("verb").toString());
        }


    }

    /**
     * Ensure queries return a subset of entities in the correct order
     * 1. Query for a subset of the entities
     * 2. Validate that the correct entities are returned
     *
     * @throws Exception
     */
    @Test
    public void queryReturnCount() throws Exception {
        int numValuesTested = 20;

        generateTestEntities(numValuesTested, "activities");
        //1. Query for a subset of the entities
        String inCorrectQuery = "select * where ordinal >= " + (numValuesTested / 2) + " order by ordinal asc";
        QueryParameters params = new QueryParameters().setQuery(inCorrectQuery).setLimit(numValuesTested / 2);
        Collection activities = this.app().collection("activities").get(params);
        //2. Validate that the correct entities are returned
        assertEquals(numValuesTested / 2, activities.getResponse().getEntityCount());

        List<Entity> entitiesReturned = activities.getResponse().getEntities();
        for (int i = 0; i < numValuesTested / 2; i++) {
            assertEquals(numValuesTested / 2 + i, Integer.parseInt(entitiesReturned.get(i).get("ordinal").toString()));
        }

    }

    /**
     * Validate sort order with AND/OR query
     * 1. Use AND/OR query to retrieve entities
     * 2. Verify the order of results
     *
     * @throws Exception
     */
    @Test
    public void queryCheckAsc() throws Exception {
        int numOfEntities = 20;
        String collectionName = "imagination";

        generateTestEntities(numOfEntities, collectionName);

        //2. Use AND/OR query to retrieve entities
        String inquisitiveQuery = "select * where Ordinal gte 0 and Ordinal lte  " + (numOfEntities / 2)
            + " or WhoHelpedYou eq 'Ruff' ORDER BY Ordinal asc";
        QueryParameters params = new QueryParameters().setQuery(inquisitiveQuery).setLimit(numOfEntities / 2);
        Collection activities = this.app().collection(collectionName).get(params);

        //3. Verify the order of results
        assertEquals(numOfEntities / 2, activities.getResponse().getEntityCount());
        List<Entity> entitiesReturned = activities.getResponse().getEntities();
        for (int i = 0; i < numOfEntities / 2; i++) {
            assertEquals(i, Integer.parseInt(entitiesReturned.get(i).get("ordinal").toString()));
        }
    }


    /**
     * Test a standard query
     * 1. Issue a query
     * 2. validate that a full page of (10) entities is returned
     *
     * @throws Exception
     */
    @Test
    public void queryReturnCheck() throws Exception {
        int numOfEntities = 20;
        String collectionName = "imagination";

        generateTestEntities(numOfEntities, collectionName);

        //2. Issue a query
        String inquisitiveQuery = String.format("select * where ordinal >= 0 and ordinal <= %d or WhoHelpedYou = 'Ruff' ORDER BY created", numOfEntities);
        QueryParameters params = new QueryParameters().setQuery(inquisitiveQuery);
        Collection activities = this.app().collection(collectionName).get(params);

        //3. validate that a full page of (10) entities is returned
        assertEquals(10, activities.getResponse().getEntityCount());
        List<Entity> entitiesReturned = activities.getResponse().getEntities();
        for (int i = 0; i < 10; i++) {
            assertEquals(i, Integer.parseInt(entitiesReturned.get(i).get("ordinal").toString()));
        }
    }

    /**
     * Test a standard query using alphanumeric operators
     * 1. Issue a query using alphanumeric operators
     * 2. validate that a full page of (10) entities is returned
     *
     * @throws Exception
     */
    @Test
    public void queryReturnCheckWithShortHand() throws Exception {
        int numOfEntities = 10;
        String collectionName = "imagination";

        generateTestEntities(numOfEntities, collectionName);

        //2. Issue a query using alphanumeric operators
        String inquisitiveQuery = "select * where Ordinal gte 0 and Ordinal lte 2000 or WhoHelpedYou eq 'Ruff' ORDER BY created";
        QueryParameters params = new QueryParameters().setQuery(inquisitiveQuery);
        Collection activities = this.app().collection(collectionName).get(params);

        //3. validate that a full page of (10) entities is returned
        assertEquals(10, activities.getResponse().getEntityCount());
        List<Entity> entitiesReturned = activities.getResponse().getEntities();
        for (int i = 0; i < 10; i++) {
            assertEquals(i, Integer.parseInt(entitiesReturned.get(i).get("ordinal").toString()));
        }
    }

}
