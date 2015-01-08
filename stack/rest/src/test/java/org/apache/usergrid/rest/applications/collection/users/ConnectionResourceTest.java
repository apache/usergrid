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


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.endpoints.CollectionEndpoint;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.User;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import java.io.IOException;

import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.*;


/**
 * Contains tests that center around using connections with entities and users.
 */
public class ConnectionResourceTest extends AbstractRestIT {

    private CollectionEndpoint thingsResource;
    private UUID thing1Id;
    private UUID thing2Id;


    /**
     * Setup two thing objects for use in the following tests
     */
    @Before
    public void setup(){
        this.thingsResource =  this.app().collection("things");

        thing1Id =  thingsResource.post( new Entity().chainPut("name", "thing1") ).getUuid();

        thing2Id = thingsResource.post( new Entity().chainPut("name", "thing2") ).getUuid();

        refreshIndex();
    }
    /**
     * Checks to see that a connection associated with one user cannot be retrieved by a different user.
     * @throws IOException
     */
    @Test
    public void connectionMisMatchTest() throws IOException {

        //Creates collection and posts a chicken entity to it.
        CollectionEndpoint activities = this.app().collection("peeps");

        Entity stuff = new Entity().chainPut("name", "chicken").chainPut("type","chicken");

        activities.post(stuff);

        //Create two users
        User payload = new User("todd", "todd", "todd@apigee.com", "password");
        this.app().collection("users").post(payload);
        payload = new User("scott", "scott", "scott@apigee.com", "password");
        this.app().collection("users").post(payload);

        refreshIndex();

        //Set user Todd  to connect to the chicken entity.
        Entity entity = this.app().collection("users").entity("todd").connection("likes").collection("peeps").entity("chicken").post();

        assertNotNull(entity);

        refreshIndex();

        //Get the collection and get the chicken entity.
        Collection collection = this.app().collection("peeps").get();

        String uuid = collection.next().get("uuid").toString();

        //Set user Scott to get back a nonexistant connection.
        try {
            this.app().collection("users").entity("scott").connection("likes").entity(uuid).get();

            assert ( false );
        }
        catch ( UniformInterfaceException uie ) {
            assertEquals( 404, uie.getResponse().getStatus() );
        }
    }


    /**
     * Checks that we can setup a connection loop and that we can retrieve both entities from the loop.
     * @throws IOException
     */
    @Test
    public void connectionsLoopbackTest() throws IOException {

        //create a connection loop by having thing1 connect to thing2 and vise versa.
        thingsResource.entity( thing1Id ).connection( "likes" ).entity( thing2Id ).post();
        thingsResource.entity( thing2Id ).connection( "likes" ).entity( thing1Id ).post();

        refreshIndex();

        //Do a get on thing1 to make sure we have the connection present
        Collection collection =this.app().collection("things").entity(thing1Id).connection( "likes" ).get();

        assertTrue("Connection url returned in entity", collection.hasNext());

        //Verify that thing1 is connected to thing2
        UUID returnedUUID  = collection.next().getUuid();

        assertEquals( thing2Id, returnedUUID );

        //now follow the loopback from thing2, which should be pointers to thing1

        collection  = this.app().collection("things").entity(thing2Id).connection("likes").get();

        UUID returned = collection.next().getUuid();

        assertEquals( "Should point to thing1 as an incoming entity connection", thing1Id, returned );
    }


    /**
     * Checks that we can get a valid uuid from a connection url and follow it to the correct entity.
     * @throws IOException
     */
    @Test
    public void connectionsUrlTest() throws IOException {

        //Create a connection between thing1 and thing2
        thingsResource.entity( thing1Id ).connection( "likes" ).entity( thing2Id ).post();


        refreshIndex();

        //Do a get on thing1 to make sure we have the connection present
        Entity response = thingsResource.entity( "thing1" ).get();

        String url =((Map) ((Map)response.get( "metadata" )).get( "connections" )).get("likes").toString();


        assertNotNull( "Connection url returned in entity", url );

        //trim off the starting / from the url.
        url = url.substring( 1 );


        //now that we know the URl is correct, follow it to get the entity in the connection

        Collection collection = this.app().collection(url).get();

        UUID returnedUUID =collection.next().getUuid();

        assertEquals( thing2Id, returnedUUID );

        //get on the collection works, now get it directly by uuid. We should also get thing1 from the loopback url.
        response = thingsResource.entity( thing1Id ).connection( "likes" ).entity( thing2Id ).get();

        UUID returned = response.getUuid();

        assertEquals( "Should point to thing2 as an entity connection", thing2Id, returned );
    }


    /**
     * Deletes the connected to entity and make sure the delete persists.
     * @throws IOException
     */
    @Test //USERGRID-3011
    public void deleteConnectedEntity() throws IOException {

        //create the connection
        thingsResource.entity( thing1Id ).connection( "likes" ).entity( thing2Id ).post();

        refreshIndex();

        //Delete the connected entity.
        thingsResource.entity( "thing2" ).delete();

        refreshIndex();

        //Make sure that we can no longer retrieve the entity.
        int status = 0;
        try {
            thingsResource.entity("thing2").get();
            fail( "Entity should have been deleted." );
        }catch (UniformInterfaceException e){
            status = e.getResponse().getStatus();
        }
        assertEquals(404,status);


    }


    /**
     * Delete the connecting entity and make sure the delete persists.
     * @throws IOException
     */
    @Test //USERGRID-3011
    public void deleteConnectingEntity() throws IOException {

        //create the connection
        thingsResource.entity( thing1Id ).connection( "likes" ).entity( thing2Id ).post();

        refreshIndex();

        //Delete the connecting entity
        thingsResource.entity( "thing1" ).delete();

        refreshIndex();

        //Make sure that we can no longer retrieve the entity.
        int status = 0;
        try {
            thingsResource.entity("thing1").get();
            fail( "Entity should have been deleted." );
        }catch (UniformInterfaceException e){
            status = e.getResponse().getStatus();
        }
        assertEquals(404,status);

    }


}
