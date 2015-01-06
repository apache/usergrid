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
import org.junit.Rule;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import java.io.IOException;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.apache.usergrid.utils.MapUtils.hashMap;


/**
 * // TODO: Document this
 *
 * @author ApigeeCorporation
 * @since 4.0
 */
public class ConnectionResourceTest extends AbstractRestIT {
   


    @Test
    public void connectionsQueryTest() throws IOException {


        CollectionEndpoint activities = this.app().collection("peeps");

        Entity stuff = new Entity().chainPut("type", "chicken");

        activities.post(stuff);


        Entity payload = new Entity();
        payload.put( "username", "todd" );

        Entity objectOfDesire = new Entity();
        objectOfDesire.put( "codingmunchies", "doritoes" );

        Entity entity = this.app().collection("users").post(payload);


        payload.put( "username", "scott" );

         entity = this.app().collection("users").post(payload);
        refreshIndex();


    /*finish setting up the two users */



        entity = this.app().collection("users").entity("todd").connection("likes").entity("peeps").post();

        assertNotNull(entity);

        refreshIndex();

        Collection collection = this.app().collection("peeps").get();

        String uuid = collection.next().get("uuid").toString();


        try {
            entity = this.app().collection("users").entity("scott").connection("likes").entity(uuid).get();

            assert ( false );
        }
        catch ( UniformInterfaceException uie ) {
            assertEquals( 404, uie.getResponse().getClientResponseStatus().getStatusCode() );
        }
    }


    @Test
    public void connectionsLoopbackTest() throws IOException {

        CollectionEndpoint things = this.app().collection("things");

        UUID thing1Id =  things.post( new Entity().chainPut("name", "thing1") ).getUuid();

        UUID thing2Id = things.post( new Entity().chainPut("name", "thing2") ).getUuid();


        refreshIndex();

        //create the connection
        things.entity( thing1Id ).connection( "likes" ).entity( thing2Id ).post();


        refreshIndex();

        //test we have the "likes" in our connection meta data response

        Entity response = things.entity( "thing1" ).get();

        String url =((Map) ((Map)response.get( "metadata" )).get( "connections" )).get( "likes" ).toString();


        assertNotNull( "Connection url returned in entity", url );

        //trim off the start /
        url = url.substring( 1 );


        //now that we know the URl is correct, follow it

        Collection collection = this.app().collection(url).get();

        Entity entity  =collection.next();
        UUID returnedUUID = entity.getUuid();

        assertEquals( thing2Id, returnedUUID );


        //now follow the loopback, which should be pointers to the other entity

        url = ((Map) ((Map)entity.get( "metadata" )).get( "connections" )).get("likes").toString();

        assertNotNull( "Incoming edge URL provited", url );

        //trim off the start /
        url = url.substring( 1 );

        //now we should get thing1 from the loopback url

        collection = this.app().collection(url).get();

        UUID returned = collection.next().getUuid();

        assertEquals( "Should point to thing1 as an incoming entity connection", thing1Id, returned );
    }


    @Test
    public void connectionsUUIDTest() throws IOException {

        CollectionEndpoint things = this.app().collection("things");

        UUID thing1Id =   things.post( new Entity().chainPut("name", "thing1") ).getUuid();

        UUID thing2Id =  things.post( new Entity().chainPut("name", "thing2") ).getUuid();


        refreshIndex();

        //create the connection
        things.entity( thing1Id ).connection( "likes" ).entity( thing2Id ).post();


        refreshIndex();

        //test we have the "likes" in our connection meta data response

        Entity response = things.entity( "thing1" ).get();

        String url =((Map) ((Map)response.get( "metadata" )).get( "connections" )).get( "likes" ).toString();


        assertNotNull( "Connection url returned in entity", url );

        //trim off the start /
        url = url.substring( 1 );


        //now that we know the URl is correct, follow it

        Collection collection = this.app().collection(url).get();

        UUID returnedUUID =collection.next().getUuid();

        assertEquals( thing2Id, returnedUUID );

        //get on the collection works, now get it directly by uuid

        //now we should get thing1 from the loopback url

        response = things.entity( thing1Id ).connection( "likes" ).entity( thing2Id ).get();

        UUID returned = response.getUuid();

        assertEquals( "Should point to thing2 as an entity connection", thing2Id, returned );
    }

    @Test //USERGRID-3011
    public void connectionsDeleteSecondEntityInConnectionTest() throws IOException {

        CollectionEndpoint things = this.app().collection("things");

        UUID thing1Id =  things.post( new Entity().chainPut("name", "thing1") ).getUuid();

        UUID thing2Id = things.post( new Entity().chainPut("name", "thing2") ).getUuid();

        refreshIndex();

        //create the connection
        things.entity( thing1Id ).connection( "likes" ).entity( thing2Id ).post();

        ApiResponse response = things.entity( "thing2" ).delete();

        refreshIndex();

        Entity node = things.entity ( "thing2" ).get();

        assertNull(node);

    }

    @Test //USERGRID-3011
    public void connectionsDeleteFirstEntityInConnectionTest() throws IOException {

        CollectionEndpoint things = this.app().collection("things");

        UUID thing1Id =   things.post( new Entity().chainPut("name", "thing1") ).getUuid();

        UUID thing2Id =   things.post( new Entity().chainPut("name", "thing2") ).getUuid();

        refreshIndex();

        //create the connection
        things.entity( thing1Id ).connection( "likes" ).entity( thing2Id ).post();

        ApiResponse response = things.entity( "thing1" ).delete();

        refreshIndex();

        Entity node = things.entity ( "thing1" ).get();

        assertNull(node);

    }


}
