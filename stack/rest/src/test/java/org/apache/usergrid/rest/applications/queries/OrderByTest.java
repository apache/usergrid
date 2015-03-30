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
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.Assert.assertEquals;


/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class OrderByTest extends QueryTestBase {
    private static Logger log = LoggerFactory.getLogger(OrderByTest.class);

    /**
     * Test correct sort order for Long properties
     *
     * @throws IOException
     */
    @Test
    public void orderByLongAsc() throws IOException {
        int numOfEntities = 20;
        String collectionName = "activities";
        //create our test entities
        generateTestEntities(numOfEntities, collectionName);

        QueryParameters params = new QueryParameters()
            .setQuery("select * order by ordinal asc")
            .setLimit(numOfEntities);
        Collection activities = this.app().collection("activities").get(params);
        assertEquals(numOfEntities, activities.getResponse().getEntityCount());
        //results should be ordered by ordinal
        int index = 0;
        while (activities.hasNext()) {
            Entity activity = activities.next();
            //make sure the correct ordinal properties are returned
            assertEquals(index++, Long.parseLong(activity.get("ordinal").toString()));
        }
    }

    /**
     * Test correct sort order for Long properties
     *
     * @throws IOException
     */
    @Test
    public void orderByLongDesc() throws IOException {
        int numOfEntities = 20;
        String collectionName = "activities";
        //create our test entities
        generateTestEntities(numOfEntities, collectionName);

        QueryParameters params = new QueryParameters()
            .setQuery("select * order by ordinal desc")
            .setLimit(numOfEntities);
        Collection activities = this.app().collection("activities").get(params);
        assertEquals(numOfEntities, activities.getResponse().getEntityCount());
        //Since the sort order is descending, start at the last entity we created
        int index = numOfEntities - 1;
        //results should be sorted by ordinal
        while (activities.hasNext()) {
            Entity activity = activities.next();
            //make sure the correct ordinal properties are returned
            //decrement the index to get the next entity in reverse order
            assertEquals(index--, Long.parseLong(activity.get("ordinal").toString()));
        }
    }

    /**
     * Test correct sort order for Boolean properties
     *
     * @throws IOException
     */
    @Test
    public void orderByBooleanAsc() throws IOException {
        int numOfEntities = 20;
        String collectionName = "activities";
        //create our test entities
        generateTestEntities(numOfEntities, collectionName);

        QueryParameters params = new QueryParameters()
            .setQuery("select * order by madeup asc")
            .setLimit(numOfEntities);
        Collection activities = this.app().collection("activities").get(params);
        assertEquals(numOfEntities, activities.getResponse().getEntityCount());
        int index = 0;
        //results should be sorted false, then true
        //The first half of entities returned should have "madeup = false"
        //The second half of entities returned should have "madeup = true"
        while (activities.hasNext()) {
            Entity activity = activities.next();
            if (index++ < numOfEntities / 2) {
                assertEquals("false", activity.get("madeup").toString());
            } else {
                assertEquals("true", activity.get("madeup").toString());
            }
        }
    }

    /**
     * Test correct sort order for Boolean properties
     *
     * @throws IOException
     */
    @Test
    public void orderByBooleanDesc() throws IOException {
        int numOfEntities = 20;
        String collectionName = "activities";
        //create our test entities
        generateTestEntities(numOfEntities, collectionName);

        QueryParameters params = new QueryParameters()
            .setQuery("select * order by madeup desc")
            .setLimit(numOfEntities);
        Collection activities = this.app().collection("activities").get(params);
        assertEquals(numOfEntities, activities.getResponse().getEntityCount());
        int index = 0;
        //results should be sorted true, then false
        while (activities.hasNext()) {
            Entity activity = activities.next();
            //make sure the booleans are ordered correctly
            //The first half of entities returned should have "madeup = true"
            //The second half of entities returned should have "madeup = false"
            if (index++ < numOfEntities / 2) {
                assertEquals("true", activity.get("madeup").toString());
            } else {
                assertEquals("false", activity.get("madeup").toString());
            }
        }
    }

    /**
     * Test correct sort order for String properties
     *
     * @throws IOException
     */
    @Test
    public void orderByStringAsc() throws IOException {
        int numOfEntities = 20;
        String collectionName = "activities";
        //create our test entities
        generateTestEntities(numOfEntities, collectionName);
        //Sort by the "verb" property to test alphabetical sorting of string properties
        QueryParameters params = new QueryParameters()
            .setQuery("select * order by verb asc")
            .setLimit(numOfEntities);
        Collection activities = this.app().collection("activities").get(params);
        assertEquals(numOfEntities, activities.getResponse().getEntityCount());
        int index = 0;
        //results should be sorted "go", then "stop"
        while (activities.hasNext()) {
            Entity activity = activities.next();
            //The first half of entities returned should have "verb = 'go'"
            //The second half of entities returned should have "verb = 'stop'"
            if (index++ < numOfEntities / 2) {
                assertEquals("go", activity.get("verb").toString());
            } else {
                assertEquals("stop", activity.get("verb").toString());
            }
        }
    }

    /**
     * Test correct sort order for String properties
     *
     * @throws IOException
     */
    @Test
    public void orderByStringDesc() throws IOException {
        int numOfEntities = 20;
        String collectionName = "activities";
        //create our test entities
        generateTestEntities(numOfEntities, collectionName);

        //Sort by the "verb" property, DESC to test reverse alphabetical sorting of string properties
        QueryParameters params = new QueryParameters()
            .setQuery("select * order by verb desc")
            .setLimit(numOfEntities);
        Collection activities = this.app().collection("activities").get(params);
        assertEquals(numOfEntities, activities.getResponse().getEntityCount());
        int index = 0;
        //results should be sorted "stop", then "go"
        while (activities.hasNext()) {
            Entity activity = activities.next();
            //The first half of entities returned should have "verb = 'stop'"
            //The second half of entities returned should have "verb = 'go'"
            if (index++ < numOfEntities / 2) {
                assertEquals("stop", activity.get("verb").toString());
            } else {
                assertEquals("go", activity.get("verb").toString());
            }
        }

    }

    /**
     * Inserts a number of entities. Query a subset of entities
     * with unspecified 'order by' and then ordered by 'created'
     * to ensure the result is unchanged.
     * 1. Insert entities
     * 2. Query without 'order by'
     * 3. Query with 'order by'
     * 4. Ensure the same entities are returned
     *
     * @throws IOException
     */
    @Test
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
        assertEquals(10, activitiesWithoutOrderBy.getResponse().getEntityCount());
        //3. Query with 'order by'
        query = query + " order by created desc";
        params.setQuery(query);
        Collection activitiesWithOrderBy = this.app().collection("activities").get(params);
        assertEquals(10, activitiesWithOrderBy.getResponse().getEntityCount());
        //4. Ensure the same entities are returned
        while (activitiesWithoutOrderBy.hasNext() && activitiesWithOrderBy.hasNext()) {
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
     *
     * @throws IOException
     */
    @Test
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
        assertEquals(5, activities.getResponse().getEntityCount());

    }

    /**
     * Ensure that results are returned in the correct descending order, when specified
     * 1. Insert a number of entities and add them to an array
     * 2. Query for the entities in descending order
     * 3. Validate that the order is correct
     *
     * @throws IOException
     */
    @Test
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
        int index = size - 1;

        QueryParameters params = new QueryParameters().setQuery(errorQuery);
        Collection activitiesResponse = this.app().collection("activities").get(params);
        //3. Validate that the order is correct
        do {
            int returnSize = activitiesResponse.getResponse().getEntityCount();
            //loop through the current page of results
            for (int i = 0; i < returnSize; i++, index--) {
                assertEquals(activitiesResponse.getResponse().getEntities().get(i).get("uuid").toString(),
                    (activities[i]).get("uuid").toString());
            }
            //grab the next page of results
            activitiesResponse = this.app().collection("activities").getNextPage(activitiesResponse, params, true);
        }
        while (activitiesResponse.getCursor() != null);
    }
}
