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
package org.apache.usergrid.rest.applications.users;


import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.CustomCollection;
import org.apache.usergrid.rest.test.resource.app.UsersCollection;

import static junit.framework.Assert.assertNull;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertEquals;


/**
 * // TODO: Document this
 */
public class MatrixQueryTests extends AbstractRestIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test
    public void orderByShouldNotAffectResults() {

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


        //now create 3 restaurants

        CustomCollection restaurants = context.collection( "restaurants" );


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



        JsonNode testGetUsers = context.collection( "users" ).get().get( "entities" );

        JsonNode likesNode =
                context.collection( "users" ).entity( "user1" ).connection( "likes" ).get().get( "entities" );


        JsonNode queryResponse = context.collection( "users" ).withMatrix(
                hashMap( "ql", "where fullname contains 'Smith'" ).map( "limit", "1000" ) ).connection( "likes" ).get();

        assertEquals( "Old Major", getEntityName( queryResponse, 0 ) );

        assertEquals( "tag", getEntityName( queryResponse, 1 ) );

        assertEquals( "Squeaky Bean", getEntityName( queryResponse, 2 ) );

        /**
         * No additional elements in the response
         */
        assertNull( getEntity( queryResponse, 3 ) );
    }
}
