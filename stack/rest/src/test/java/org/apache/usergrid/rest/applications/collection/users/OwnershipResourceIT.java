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


import org.apache.usergrid.utils.MapUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Take in tests that handle owner permissions relating to a specific user.
 */

public class OwnershipResourceIT extends AbstractRestIT {

    private CollectionEndpoint usersResource;
    private User user1;
    private User user2;


    /**
     * Setup two user objects for use in the following tests.
     */
    @Before
    public void setup(){
        this.usersResource =  this.app().collection("users");
        String email = "testuser1@usergrid.org";
        String email2 = "testuser2@usergrid.org";

        user1 = new User("testuser1","testuser1", email, "password" );
        user2 = new User("testuser2","testuser2", email2, "password" );

        user1 = new User(this.usersResource.post(user1));
        user2 = new User(this.usersResource.post(user2));

        refreshIndex();
    }


    /**
     * Verifies that me and user1 are the same and that we can revoke the token from user1 and have the me call fail.
     * @throws Exception
     */
    @Test
    public void meVerify() throws Exception {

        //Clear the applications previous token and start anonymous
        this.app().token().clearToken();

        //Set the token for getting the me entity.
        Token token = this.app().token().post(new Token(user1.getUsername(),"password"));

        //Get the entity known as "me" out of the users collection and asserts it isn't null and is equal to user1.
        Entity userNode = usersResource.entity("me").get();

        assertNotNull( userNode );
        assertEquals( user1.getName(), userNode.get( "username" ) );

        //Gets the uuid of the me entity.
        String uuid = userNode.getUuid().toString();
        assertNotNull( uuid );

        //Revoke the user1 token
        usersResource.entity(user1).connection("revoketokens").post(new Entity().chainPut("token", token.getAccessToken()));

        refreshIndex();

        //See if we can still access the me entity after revoking its token
        try {
             usersResource.entity("me").get();
             fail("This should not work after we've revoked the usertoken");
        }
        catch ( Exception ex ) {
            ex.printStackTrace();
            assertTrue( ex.getMessage().contains( "401" ) );
        }
    }


    /**
     * Checks that that can only see our own devices when looking at our own path. Then checks that we can see
     * both devices from a root path.
     * @throws IOException
     */
    @Test
    public void contextualPathOwnership() throws IOException {

        //Clear the applications previous token and start anonymous
        this.app().token().clearToken();

        //Setting the token to be in a user1 context.
        this.app().token().post(new Token(user1.getUsername(),"password"));


        // create device 1 on user1 devices
        usersResource.entity("me").collection("devices")
               .post(new Entity( ).chainPut("name", "device1").chainPut("number", "5551112222"));
        refreshIndex();

        //Clear the current user token
        this.app().token().clearToken();

        // create device 2 on user 2 and switch the context to use user2
        Token token = this.app().token().post(new Token(user2.getUsername(),"password"));
        usersResource.entity("me").collection("devices")
                .post(new Entity( ).chainPut("name", "device2").chainPut("number", "5552223333"));

        refreshIndex();

        //Check that we can get back device1 on user1
        token = this.app().token().post(new Token(user1.getUsername(),"password"));

        CollectionEndpoint devices = usersResource.entity( user1 ).collection("devices");

        Entity data = devices.entity("device1").get();
        assertNotNull( data );
        assertEquals("device1", data.get("name").toString());

        // check we can't see device2 on user1
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

        // log in as user 2 and check that we can see device 2
        token = this.app().token().post(new Token(user2.getUsername(),"password"));

        devices =  usersResource.entity("me").collection("devices");

        data = devices.entity("device2").get();
        assertNotNull( data );
        assertEquals( "device2", data.get("name").toString() );

        // check we can't see device1 on user2
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
        // test we can see both devices for user 1 under root application
        token = this.app().token().post(new Token(user1.getUsername(),"password"));

        data = devices.entity("device1").get();

        assertNotNull( data );
        assertEquals( "device1", data.get("name").toString() );
        data = devices.entity("device2").get();

        assertNotNull( data );
        assertEquals( "device2", data.get("name").toString() );

        // test we can see both devices for user 2 under root application
        token = this.app().token().post(new Token(user2.getUsername(),"password"));


        data = devices.entity( "device1" ).get();

        assertNotNull( data );
        assertEquals( "device1",data.get( "name" ).toString() );

        data = devices.entity( "device2" ).get();

        assertNotNull( data );
        assertEquals( "device2",data.get( "name" ).toString() );
    }


    /**
     * Tests that we can have our own personal connections without being seen by other users, but are still visible
     * from a root context.
     * @throws IOException
     */
    @Test
    public void contextualConnectionOwnership() throws IOException {

        // anonymous user
        this.app().token().clearToken();

        //Setting the token to be in a user1 context.
        this.app().token().post(new Token(user1.getUsername(),"password"));


        // create a 4peaks restaurant
        Entity data = this.app().collection("restaurants").post(new Entity().chainPut("name", "4peaks"));

        refreshIndex();

        //Create a restaurant and link it to user1/me
        Entity fourPeaksData = usersResource.entity("me")
                .connection("likes").collection( "restaurants" ).entity( "4peaks" ).post();

        refreshIndex();

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

        //Setting the token to be in a user1 context.
        this.app().token().post(new Token(user1.getUsername(),"password"));

        //Gets the connection between user1 and the their restaurants. In this case gets 4peaks
        CollectionEndpoint likeRestaurants =
                usersResource.entity( "me" ).connection( "likes" )
                       .collection( "restaurants" );

        //Check that we can get the 4peaks restaurant by using its uuid
        String peaksId = fourPeaksData.getUuid().toString();
        data = likeRestaurants.entity(peaksId).get();
        assertNotNull( data );
        assertEquals("4peaks", data.get("name").toString());

        //Check that we can get the restaurant by name
        data = likeRestaurants.entity( "4peaks" ).get();
        assertNotNull( data );
        assertEquals( "4peaks", data.get("name").toString() );

        // check we can't see arrogantbutcher by name or id from user1
        int status = 200;
        try {
            likeRestaurants.entity("arrogantbutcher").get();
        }catch (UniformInterfaceException e){
            status = e.getResponse().getStatus();
        }
        assertEquals(status, 404);

        status = 200;
        try {
            likeRestaurants.entity( arrogantButcherId ).get();
        }catch (UniformInterfaceException e){
            status = e.getResponse().getStatus();
        }
        assertEquals(status, 404);

        // do a collection load, make sure we're not getting entities we shouldn't see
        Collection collectionData = likeRestaurants.get();

        assertEquals("4peaks", collectionData.next().get("name").toString());
        assertTrue( !collectionData.hasNext() );

        // log in as user 2 and check that we can see the arrogantbutcher
        this.app().token().post(new Token(user2.getUsername(),"password"));

        likeRestaurants = usersResource.entity("me").connection( "likes" )
                                 .collection( "restaurants" );

        data = likeRestaurants.entity( arrogantButcherId ).get();
        assertNotNull( data );
        assertEquals( "arrogantbutcher", data.get("name").toString() );

        data = likeRestaurants.entity( "arrogantbutcher" ).get();
        assertNotNull( data );
        assertEquals( "arrogantbutcher", data.get("name").toString() );

        // check we can't see 4peaks as user2
         status = 200;
        try {
            data = likeRestaurants.entity( "4peaks" ).get();
        }catch (UniformInterfaceException e){
            status = e.getResponse().getStatus();
        }
        assertEquals( status,404 );



        status = 200;
        try {
            likeRestaurants.entity( peaksId ).get();

        }catch (UniformInterfaceException e){
            status = e.getResponse().getStatus();
        }
        assertEquals( status,404 );


        // do a collection load, make sure we're not loading device 1
        collectionData = likeRestaurants.get();

        assertEquals("arrogantbutcher", collectionData.next().get("name").toString());
        assertTrue( !collectionData.hasNext() );

        // we should see both devices when loaded from the root application

        //Check we can see both restaurants as user1 from the root application
        this.app().token().post(new Token(user1.getUsername(),"password"));

        CollectionEndpoint restaurants = this.app().collection( "restaurants" );
        data = restaurants.entity( "4peaks" ).get();

        assertNotNull( data );
        assertEquals( "4peaks", data.get("name").toString() );

        data = restaurants.entity( "arrogantbutcher" ).get();

        assertNotNull( data );
        assertEquals( "arrogantbutcher", data.get("name").toString() );

        //Check we can see both restaurants as user2 from the root application
        this.app().token().post(new Token(user2.getUsername(),"password"));
        restaurants = this.app().collection("restaurants");
        data = restaurants.entity( "4peaks" ).get();

        assertNotNull( data );
        assertEquals( "4peaks",data.get("name").toString() );

        data = restaurants.entity( "arrogantbutcher" ).get();

        assertNotNull( data );
        assertEquals( "arrogantbutcher",data.get("name").toString() );
    }


    /**
     * Checks that a once guests permissions are opened up that a user can view the connections/entities
     * and get/post/delete things on that connection.
     * @throws IOException
     */
    @Test
    public void contextualConnectionOwnershipGuestAccess() throws IOException {

        //set up full GET,PUT,POST,DELETE access for guests
        this.app().collection("roles").entity( "guest" ).collection( "permissions" )
                .post(new Entity().chainPut("permission", "get,put,post,delete:/**"));


        //Sets up the cities collection with the city tempe
        Entity city = this.app().collection("cities").post(new Entity().chainPut("name", "tempe"));

        refreshIndex();

        // create a 4peaks restaurant that is connected by a like to tempe.
        Entity data = this.app().collection("cities").entity( "tempe" ).connection( "likes" )
                               .collection( "restaurants" ).post(new Entity().chainPut("name", "4peaks"));

        String peaksId = data.get("uuid").toString();

        // create the arrogantbutcher restaurant that is connected by a like to tempe.
        data = this.app().collection("cities").entity( "tempe" ).connection( "likes" )
                      .collection( "restaurants" ).post(new Entity().chainPut("name", "arrogantbutcher"));

        String arrogantButcherId = data.get("uuid").toString();

        //Set the user to user1 and get the collection cities
        this.app().token().post(new Token(user1.getUsername(),"password"));

        CollectionEndpoint likeRestaurants =
                this.app().collection("cities").entity( "tempe" ).connection( "likes" );

        refreshIndex();

        // check we can get the resturant entities back via uuid without a collection name
        data = likeRestaurants.entity( peaksId ).get();
        assertNotNull( data );
        assertEquals( "4peaks", data.get("name").toString() );

        data = likeRestaurants.entity( arrogantButcherId ).get();
        assertEquals( "arrogantbutcher", data.get("name").toString() );

        // check we can get the restaurant via uuid with a collection name
        data = likeRestaurants.collection( "restaurants" ).entity( peaksId ).get();
        assertNotNull( data );
        assertEquals( "4peaks", data.get("name").toString() );

        data = likeRestaurants.collection( "restaurants" ).entity( arrogantButcherId ).get();
        assertEquals( "arrogantbutcher", data.get("name").toString() );

        // Delete the restaurants, either token should work for deletion
        ApiResponse deleteResponse = likeRestaurants.collection( "restaurants" ).entity( peaksId ).delete();

        assertNotNull( deleteResponse );
        assertEquals( "4peaks", deleteResponse.getEntities().get(0).get("name").toString() );

        deleteResponse = likeRestaurants.collection( "restaurants" ).entity( arrogantButcherId ).delete();

        assertNotNull( deleteResponse );
        assertEquals( "arrogantbutcher", deleteResponse.getEntities().get(0).get("name").toString() );
    }
}
