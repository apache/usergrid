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
package org.apache.usergrid.rest.applications.collection.users;


import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.endpoints.CollectionEndpoint;
import org.apache.usergrid.rest.test.resource2point0.model.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.apache.usergrid.cassandra.Concurrent;

import org.apache.usergrid.utils.MapUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 *
 */
@Concurrent()
public class OwnershipResourceIT extends AbstractRestIT {

    private CollectionEndpoint usersResource;
    private User user1;
    private User user2;

    @Before
    public void setup(){
        this.usersResource =  this.app().collection("users");
        String email = "testuser1@usergrid.org";
        String email2 = "testuser2@usergrid.org";

        user2 = new User("testuser2","testuser2", email2, "password" );

        user1 = new User("testuser1","testuser1", email, "password" );
        user1 = new User(this.usersResource.post(user1));
        user2 = new User(this.usersResource.post(user2));

        refreshIndex();
    }

    @Test
    public void meVerify() throws Exception {

        this.app().token().clearToken();

        Token token = this.app().token().post(new Token(user1.getUsername(),"password"));

        Entity userNode = usersResource.entity("me").get();

        assertNotNull( userNode );

        String uuid = userNode.getUuid().toString();
        assertNotNull( uuid );

        usersResource.entity(user1).connection("revoketokens").post(new Entity().chainPut("token", token.getAccessToken()));

        refreshIndex();

        try {
             userNode = usersResource.entity("me").get();

            fail();
        }
        catch ( Exception ex ) {
            ex.printStackTrace();
            assertTrue( ex.getMessage().contains( "401" ) );
        }
    }


    @Test
    public void contextualPathOwnership() throws IOException {

        // anonymous user
        this.app().token().clearToken();


        Token token = this.app().token().post(new Token(user1.getUsername(),"password"));


        // create device 1 on user1 devices
        usersResource.entity("me").collection("devices")
               .post(new Entity( ).chainPut("name", "device1").chainPut("number", "5551112222"));
        refreshIndex();

        // anonymous user
        this.app().token().clearToken();

        // create device 2 on user 2
         token = this.app().token().post(new Token(user2.getUsername(),"password"));
        usersResource.entity("me").collection("devices")
                .post(new Entity( ).chainPut("name", "device2").chainPut("number", "5552223333"));

        refreshIndex();

        // now query on user 1.
        token = this.app().token().post(new Token(user1.getUsername(),"password"));

        CollectionEndpoint devices = usersResource.entity( user1 ).collection("devices");

        Entity data = devices.entity("device1").get();
        assertNotNull( data );
        assertEquals("device1", data.get("name").toString());

        // check we can't see device2
        int status = 0;
        try {
            data = devices.entity("device2").get();
        }catch (UniformInterfaceException e){
            status = e.getResponse().getStatus();
        }
        assertEquals( status,404 );

        // do a collection load, make sure we're not loading device 2
        Collection devicesData = devices.get();

        assertEquals("device1", devicesData.next().get("name").toString());
        assertTrue(!devicesData.hasNext());

        // log in as user 2 and check it
        token = this.app().token().post(new Token(user2.getUsername(),"password"));

        devices =  usersResource.entity("me").collection("devices");

        data = devices.entity("device2").get();
        assertNotNull( data );
        assertEquals( "device2", data.get("name").toString() );

        // check we can't see device1
        status = 0;
        try{
        data = devices.entity("device1").get();
        }catch (UniformInterfaceException e){
            status = e.getResponse().getStatus();
        }
        assertEquals( status,404 );

        // do a collection load, make sure we're not loading device 1
        devicesData = devices.get();

        assertEquals("device2", devicesData.next().get("name").toString());
        assertTrue(!devicesData.hasNext());

        // we should see both devices when loaded from the root application

        devices  = this.app().collection("devices");
        // test for user 1
        token = this.app().token().post(new Token(user1.getUsername(),"password"));

        data = devices.entity("device1").get();

        assertNotNull( data );
        assertEquals( "device1", data.get("name").toString() );
        data = devices.entity("device2").get();

        assertNotNull( data );
        assertEquals( "device2", data.get("name").toString() );

        // test for user 2
        token = this.app().token().post(new Token(user2.getUsername(),"password"));


        data = devices.entity( "device1" ).get();

        assertNotNull( data );
        assertEquals( "device1",data.get( "name" ).toString() );

        data = devices.entity( "device2" ).get();

        assertNotNull( data );
        assertEquals( "device2",data.get( "name" ).toString() );
    }


    @Test
    public void contextualConnectionOwnership() throws IOException {

        // anonymous user
        this.app().token().clearToken();

         this.app().token().post(new Token(user1.getUsername(),"password"));


        // create a 4peaks restaurant
        Entity data = this.app().collection("restaurants").post(new Entity().chainPut("name", "4peaks"));

        refreshIndex();

        // create our connection
        data = usersResource.entity("me")
                .connection("likes").collection( "restaurants" ).entity( "4peaks" ).post();

        refreshIndex();

       String peaksId = data.getUuid().toString();

        // anonymous user
        this.app().token().clearToken();

        // create a restaurant and link it to user 2
        this.app().token().post(new Token(user2.getUsername(),"password"));

        data = this.app().collection("restaurants")
                      .post(new Entity().chainPut("name", "arrogantbutcher"));
        refreshIndex();

        data = usersResource.entity("me").connection( "likes" ).collection( "restaurants" )
                      .entity( "arrogantbutcher" ).post();
        refreshIndex();

        String arrogantButcherId = data.getUuid().toString();

        // now query on user 1.
        this.app().token().post(new Token(user1.getUsername(),"password"));

        CollectionEndpoint likeRestaurants =
                usersResource.entity( "me" ).connection( "likes" )
                       .collection( "restaurants" );

        // check we can get it via id
        data = likeRestaurants.entity(peaksId).get();
        assertNotNull( data );
        assertEquals("4peaks", data.get("name").toString());

        // check we can get it by name
        data = likeRestaurants.entity( "4peaks" ).get();
        assertNotNull( data );
        assertEquals( "4peaks", data.get("name").toString() );

        // check we can't see arrogantbutcher by name or id
        int status = 200;
        try {
            data = likeRestaurants.entity("arrogantbutcher").get();
        }catch (UniformInterfaceException e){
            status = e.getResponse().getStatus();
        }
        assertEquals(status, 404);

        status = 200;
        try {
            data = likeRestaurants.entity( arrogantButcherId ).get();
        }catch (UniformInterfaceException e){
            status = e.getResponse().getStatus();
        }
        assertEquals(status, 404);

        // do a collection load, make sure we're not entities we shouldn't see
        Collection collectionData = likeRestaurants.get();

        assertEquals("4peaks", collectionData.next().get("name").toString());
        assertTrue( !collectionData.hasNext() );

        // log in as user 2 and check it
        this.app().token().post(new Token(user2.getUsername(),"password"));

        likeRestaurants = usersResource.entity("me").connection( "likes" )
                                 .collection( "restaurants" );

        data = likeRestaurants.entity( arrogantButcherId ).get();
        assertNotNull( data );
        assertEquals( "arrogantbutcher", data.get("name").toString() );

        data = likeRestaurants.entity( "arrogantbutcher" ).get();
        assertNotNull( data );
        assertEquals( "arrogantbutcher", data.get("name").toString() );

        // check we can't see 4peaks
         status = 200;
        try {
            data = likeRestaurants.entity( "4peaks" ).get();
        }catch (UniformInterfaceException e){
            status = e.getResponse().getStatus();
        }
        assertEquals( status,404 );



        status = 200;
        try {
            data = likeRestaurants.entity( peaksId ).get();

        }catch (UniformInterfaceException e){
            status = e.getResponse().getStatus();
        }

        // do a collection load, make sure we're not loading device 1
        collectionData = likeRestaurants.get();

        assertEquals("arrogantbutcher", collectionData.next().get("name").toString());
        assertTrue( !collectionData.hasNext() );

        // we should see both devices when loaded from the root application

        // test for user 1
        this.app().token().post(new Token(user1.getUsername(),"password"));

        CollectionEndpoint restaurants = this.app().collection( "restaurants" );
        data = restaurants.entity( "4peaks" ).get();

        assertNotNull( data );
        assertEquals( "4peaks", data.get("name").toString() );

        data = restaurants.entity( "arrogantbutcher" ).get();

        assertNotNull( data );
        assertEquals( "arrogantbutcher", data.get("name").toString() );

        // test for user 2
        this.app().token().post(new Token(user2.getUsername(),"password"));
        restaurants = this.app().collection("restaurants");
        data = restaurants.entity( "4peaks" ).get();

        assertNotNull( data );
        assertEquals( "4peaks",data.get("name").toString() );

        data = restaurants.entity( "arrogantbutcher" ).get();

        assertNotNull( data );
        assertEquals( "arrogantbutcher",data.get("name").toString() );
    }


    @Test
    public void contextualConnectionOwnershipGuestAccess() throws IOException {

        //set up full GET,PUT,POST,DELETE access for guests
        this.app().collection("roles").entity( "guest" ).collection( "permissions" )
                .post(new Entity().chainPut("permission", "get,put,post,delete:/**"));



        Entity city = this.app().collection("cities").post(new Entity().chainPut("name", "tempe"));

        refreshIndex();

        String cityId = city.get("uuid").toString();

        // create a 4peaks restaurant
        Entity data = this.app().collection("cities").entity( "tempe" ).connection( "likes" )
                               .collection( "restaurants" ).post(new Entity().chainPut("name", "4peaks"));

        String peaksId = data.get("uuid").toString();

        data = this.app().collection("cities").entity( "tempe" ).connection( "likes" )
                      .collection( "restaurants" ).post(new Entity().chainPut("name", "arrogantbutcher"));

        String arrogantButcherId = data.get("uuid").toString();

        // now query on user 1.
        this.app().token().post(new Token(user1.getUsername(),"password"));

        CollectionEndpoint likeRestaurants =
                this.app().collection("cities").entity( "tempe" ).connection( "likes" );

        refreshIndex();

        // check we can get it via id with no collection name
        data = likeRestaurants.entity( peaksId ).get();
        assertNotNull( data );
        assertEquals( "4peaks", data.get("name").toString() );

        data = likeRestaurants.entity( arrogantButcherId ).get();
        assertEquals( "arrogantbutcher", data.get("name").toString() );

        // check we can get it via id with a collection name
        data = likeRestaurants.collection( "restaurants" ).entity( peaksId ).get();
        assertNotNull( data );
        assertEquals( "4peaks", data.get("name").toString() );

        data = likeRestaurants.collection( "restaurants" ).entity( arrogantButcherId ).get();
        assertEquals( "arrogantbutcher", data.get("name").toString() );

        // do a delete either token should work
        ApiResponse deleteResponse = likeRestaurants.collection( "restaurants" ).entity( peaksId ).delete();

        assertNotNull( deleteResponse );
        assertEquals( "4peaks", deleteResponse.getEntities().get(0).get("name").toString() );

        deleteResponse = likeRestaurants.collection( "restaurants" ).entity( arrogantButcherId ).delete();

        assertNotNull( deleteResponse );
        assertEquals( "arrogantbutcher", deleteResponse.getEntities().get(0).get("name").toString() );
    }
}
