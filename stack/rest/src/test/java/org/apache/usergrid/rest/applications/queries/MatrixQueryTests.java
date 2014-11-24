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

import java.util.Map;
import java.util.UUID;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.CustomCollection;
import org.apache.usergrid.rest.test.resource.app.UsersCollection;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import org.junit.Rule;
import org.junit.Test;


public class MatrixQueryTests extends AbstractRestIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test
    public void simpleMatrix() throws Exception {

        /**
         * Create 3 users which we will use for sub searching
         */
        UsersCollection users = context.users();

        Map user1 =
                hashMap( "username", "user1" ).map( "email", "testuser1@usergrid.com" ).map( "fullname", "Bob Smith" );

        users.create( user1 );


        Map user2 =
                hashMap( "username", "user2" ).map( "email", "testuser2@usergrid.com" ).map( "fullname", "Fred Smith" );

        users.create( user2 );


        Map user3 = hashMap( "username", "user3" ).map( "email", "testuser3@usergrid.com" )
                .map( "fullname", "Frank Grimes" );

        users.create( user3 );


        //now create 4 restaurants

        CustomCollection restaurants = context.customCollection( "restaurants" );


        Map restaurant1 = hashMap( "name", "Old Major" );

        UUID restaurant1Id = getEntityId( restaurants.create( restaurant1 ), 0 );

        Map restaurant2 = hashMap( "name", "tag" );

        UUID restaurant2Id = getEntityId( restaurants.create( restaurant2 ), 0 );

        Map restaurant3 = hashMap( "name", "Squeaky Bean" );

        UUID restaurant3Id = getEntityId( restaurants.create( restaurant3 ), 0 );

        Map restaurant4 = hashMap( "name", "Lola" );

        UUID restaurant4Id = getEntityId( restaurants.create( restaurant4 ), 0 );


        //now like our 3 users


        //user 1 likes old major
        users.user( "user1" ).connection( "likes" ).entity( restaurant1Id ).post();

        users.user( "user1" ).connection( "likes" ).entity( restaurant2Id ).post();


        //user 2 likes tag and squeaky bean
        users.user( "user2" ).connection( "likes" ).entity( restaurant2Id ).post();

        users.user( "user2" ).connection( "likes" ).entity( restaurant3Id ).post();

        //user 3 likes  Lola (it shouldn't appear in the results)

        users.user( "user3" ).connection( "likes" ).entity( restaurant4Id ).post();


        //now query with matrix params


//        JsonNode testGetUsers = context.collection( "users" ).get().get( "entities" );
//
//        JsonNode likesNode =
//                context.collection( "users" ).entity( "user1" ).connection( "likes" ).get().get( "entities" );
//
//
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
    }


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
