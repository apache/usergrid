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

import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.endpoints.CollectionEndpoint;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.junit.Ignore;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class MatrixQueryTests extends AbstractRestIT {

    /**
     * Test standard connection queries
     * 1. Insert a number of users
     * 2. Insert a number of restaurants
     * 3. Create "likes" connections between users and restaurants
     * 4. Retrieve "likes" connections per user and ensure the correct restaurants are returned
     *
     * @throws Exception
     */
    @Ignore
    @Test
    public void connectionsTest() throws Exception {

        //1. Insert a number of users
        Entity user1 = new Entity();
        user1.put("username", "user1");
        user1.put("email", "testuser1@usergrid.com");
        user1.put("fullname", "Bob Smith");

        Entity user2 = new Entity();
        user2.put("username", "user2");
        user2.put("email", "testuser2@usergrid.com");
        user2.put("fullname", "Fred Smith");

        Entity user3 = new Entity();
        user3.put("username", "user3");
        user3.put("email", "testuser3@usergrid.com");
        user3.put("fullname", "Frank Grimes");

        user1 = this.app().collection("users").post(user1);
        user2 = this.app().collection("users").post(user2);
        user3 = this.app().collection("users").post(user3);

        //2. Insert a number of restaurants

        Entity restaurant1 = new Entity();
        restaurant1.put("name", "Old Major");
        Entity restaurant2 = new Entity();
        restaurant2.put("name", "tag");
        Entity restaurant3 = new Entity();
        restaurant3.put("name", "Squeaky Bean");
        Entity restaurant4 = new Entity();
        restaurant4.put("name", "Lola");
        restaurant1 = this.app().collection("restaurants").post(restaurant1);
        restaurant2 = this.app().collection("restaurants").post(restaurant2);
        restaurant3 = this.app().collection("restaurants").post(restaurant3);
        restaurant4 = this.app().collection("restaurants").post(restaurant4);
        this.refreshIndex();

        //3. Create "likes" connections between users and restaurants
        //user 1 likes old major
        this.app().collection("users").entity(user1).connection("likes").collection("restaurants").entity(restaurant1).post();
        this.app().collection("users").entity(user1).connection("likes").collection("restaurants").entity(restaurant2).post();

        //user 2 likes tag and squeaky bean
        this.app().collection("users").entity(user2).connection("likes").collection("restaurants").entity(restaurant2).post();
        this.app().collection("users").entity(user2).connection("likes").collection("restaurants").entity(restaurant3).post();

        //user 3 likes  Lola (it shouldn't appear in the results)
        this.app().collection("users").entity(user3).connection("likes").collection("restaurants").entity(restaurant4).post();
        this.refreshIndex();

        //4. Retrieve "likes" connections per user and ensure the correct restaurants are returned
        Collection user1likes = this.app().collection("users").entity(user1).connection("likes").get();
        assertEquals(2, user1likes.getResponse().getEntityCount());

        Collection user2likes = this.app().collection("users").entity(user2).connection("likes").get();
        assertEquals(2, user2likes.getResponse().getEntityCount());

        Collection user3likes = this.app().collection("users").entity(user3).connection("likes").get();
        assertEquals(1, user3likes.getResponse().getEntityCount());
    }

    @Ignore
    @Test
    public void largeRootElements() {


        // create 4 restaurants

        CollectionEndpoint restaurants = this.app().collection("restaurants");

        Entity restaurant1 = new Entity().chainPut("name", "Old Major");

        UUID restaurant1Id = restaurants.post(restaurant1).getUuid();

        Entity restaurant2 = new Entity().chainPut("name", "tag");

        UUID restaurant2Id = restaurants.post(restaurant2).getUuid();

        Entity restaurant3 = new Entity().chainPut("name", "Squeaky Bean");

        UUID restaurant3Id = restaurants.post(restaurant3).getUuid();

        /**
         * Create 3 users which we will use for sub searching
         */
        CollectionEndpoint users = this.app().collection("users");


        int max = 1000;
        int count = (int) (max * 1.1);

        for (int i = 0; i < count; i++) {

            String username = "user" + i;
            String email = username + "@usergrid.com";

            Entity user1 = new Entity().chainPut("username", username).chainPut("email", email).chainPut("fullname", i + " Smith");

            users.post(user1);

            /**
             * Change our links every other time.  This way we should get all 3
             */

            if (i % 2 == 0) {
                users.entity(username).connection("likes").entity(restaurant1Id).post();

                users.entity(username).connection("likes").entity(restaurant2Id).post();
            } else {

                users.entity(username).connection("likes").entity(restaurant2Id).post();

                users.entity(username).connection("likes").entity(restaurant3Id).post();
            }
        }


        //set our limit to 1k.  We should get only 3 results, but this should run
        Collection queryResponse = this.app().collection("users").matrix(
            new QueryParameters().addParam("ql", "where fullname contains 'Smith'").addParam("limit", "1000")).collection("likes").get();

        assertEquals("Old Major", queryResponse.getResponse().getEntities().get(0).get("name"));

        assertEquals("tag", queryResponse.getResponse().getEntities().get(1).get("name"));

        assertEquals("Squeaky Bean", queryResponse.getResponse().getEntities().get(2).get("name"));

        /**
         * No additional elements in the response
         */
        assertNull(queryResponse.getResponse().getEntities().get(3));
    }
}
