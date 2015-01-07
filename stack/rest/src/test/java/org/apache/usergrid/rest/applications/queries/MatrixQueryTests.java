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
import org.junit.Test;

import static org.junit.Assert.assertEquals;


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
    assertEquals(2, user1likes.response.getEntityCount());

    Collection user2likes = this.app().collection("users").entity(user2).connection("likes").get();
    assertEquals(2, user2likes.response.getEntityCount());

    Collection user3likes = this.app().collection("users").entity(user3).connection("likes").get();
    assertEquals(1, user3likes.response.getEntityCount());
  }

  //TODO implement matrix parameters and tests!!!

//    @Test
//    public void largeRootElements() {
//
//
//        // create 4 restaurants
//
//        CustomCollection restaurants = context.collection( "restaurants" );
//
//
//        Map restaurant1 = hashMap( "name", "Old Major" );
//
//        UUID restaurant1Id = getEntityId( restaurants.create( restaurant1 ), 0 );
//
//        Map restaurant2 = hashMap( "name", "tag" );
//
//        UUID restaurant2Id = getEntityId( restaurants.create( restaurant2 ), 0 );
//
//        Map restaurant3 = hashMap( "name", "Squeaky Bean" );
//
//        UUID restaurant3Id = getEntityId( restaurants.create( restaurant3 ), 0 );
//
//
//        /**
//         * Create 3 users which we will use for sub searching
//         */
//        UsersCollection users = context.users();
//
//
//        int max = 1000;
//        int count = ( int ) (max * 1.1);
//
//        for ( int i = 0; i < count; i++ ) {
//
//            String username = "user" + i;
//            String email = username + "@usergrid.com";
//
//            Map user1 = hashMap( "username", username ).map( "email", email ).map( "fullname", i + " Smith" );
//
//            users.create( user1 );
//
//            /**
//             * Change our links every other time.  This way we should get all 3
//             */
//
//            if ( i % 2 == 0 ) {
//                users.user( username ).connection( "likes" ).entity( restaurant1Id ).post();
//
//                users.user( username ).connection( "likes" ).entity( restaurant2Id ).post();
//            }
//            else {
//
//                users.user( username ).connection( "likes" ).entity( restaurant2Id ).post();
//
//                users.user( username ).connection( "likes" ).entity( restaurant3Id ).post();
//            }
//        }
//
//
//
//        //set our limit to 1k.  We should get only 3 results, but this should run
//        JsonNode queryResponse = context.collection( "users" ).withMatrix(
//                hashMap( "ql", "where fullname contains 'Smith'" ).map( "limit", "1000" ) ).connection( "likes" ).get();
//
//        assertEquals( "Old Major", getEntityName( queryResponse, 0 ) );
//
//        assertEquals( "tag", getEntityName( queryResponse, 1 ) );
//
//        assertEquals( "Squeaky Bean", getEntityName( queryResponse, 2 ) );
//
//        /**
//         * No additional elements in the response
//         */
//        assertNull( getEntity( queryResponse, 3 ) );
//    }
}
