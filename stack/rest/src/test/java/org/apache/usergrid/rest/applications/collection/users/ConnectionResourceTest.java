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
import org.junit.Rule;
import org.junit.Test;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.CustomCollection;

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
    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test
    public void connectionsQueryTest() throws IOException {


        CustomCollection activities = context.customCollection( "peeps" );

        Map stuff = hashMap( "type", "chicken" );

        activities.create( stuff );


        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put( "username", "todd" );

        Map<String, Object> objectOfDesire = new LinkedHashMap<String, Object>();
        objectOfDesire.put( "codingmunchies", "doritoes" );

        resource().path( "/test-organization/test-app/users" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                .post( String.class, payload );

        payload.put( "username", "scott" );


        resource().path( "/test-organization/test-app/users" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                .post( String.class, payload );
    /*finish setting up the two users */


        refreshIndex("test-organization", "test-app");

        ClientResponse toddWant = resource().path( "/test-organization/test-app/users/todd/likes/peeps" )
                .queryParam( "access_token", access_token ).accept( MediaType.TEXT_HTML )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( ClientResponse.class, objectOfDesire );

        assertEquals( 200, toddWant.getStatus() );

        refreshIndex("test-organization", "test-app");

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/peeps" ).queryParam( "access_token", access_token )
                        .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                        .get( String.class ));

        String uuid = node.get( "entities" ).get( 0 ).get( "uuid" ).textValue();


        try {
            node = mapper.readTree( resource().path( "/test-organization/test-app/users/scott/likes/" + uuid )
                    .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
            assert ( false );
        }
        catch ( UniformInterfaceException uie ) {
            assertEquals( 404, uie.getResponse().getClientResponseStatus().getStatusCode() );
        }
    }


    @Test
    public void connectionsLoopbackTest() throws IOException {

        CustomCollection things = context.customCollection( "things" );

        UUID thing1Id = getEntityId( things.create( hashMap( "name", "thing1" ) ), 0 );

        UUID thing2Id = getEntityId( things.create( hashMap( "name", "thing2" ) ), 0 );


        refreshIndex(context.getOrgName(), context.getAppName());

        //create the connection
        things.entity( thing1Id ).connection( "likes" ).entity( thing2Id ).post();


        refreshIndex(context.getOrgName(), context.getAppName());

        //test we have the "likes" in our connection meta data response

        JsonNode response = things.entity( "thing1" ).get();

        String url = getEntity( response, 0 ).get( "metadata" ).get( "connections" ).get( "likes" ).asText();


        assertNotNull( "Connection url returned in entity", url );

        //trim off the start /
        url = url.substring( 1 );


        //now that we know the URl is correct, follow it

        response = context.customCollection( url ).get();

        UUID returnedUUID = getEntityId( response, 0 );

        assertEquals( thing2Id, returnedUUID );


        //now follow the loopback, which should be pointers to the other entity

        url = getEntity( response, 0 ).get( "metadata" ).get( "connecting" ).get( "likes" ).asText();

        assertNotNull( "Incoming edge URL provited", url );

        //trim off the start /
        url = url.substring( 1 );

        //now we should get thing1 from the loopback url

        response = context.customCollection( url ).get();

        UUID returned = getEntityId( response, 0 );

        assertEquals( "Should point to thing1 as an incoming entity connection", thing1Id, returned );
    }


    @Test
    public void connectionsUUIDTest() throws IOException {

        CustomCollection things = context.customCollection( "things" );

        UUID thing1Id = getEntityId( things.create( hashMap( "name", "thing1" ) ), 0 );

        UUID thing2Id = getEntityId( things.create( hashMap( "name", "thing2" ) ), 0 );


        refreshIndex(context.getOrgName(), context.getAppName());

        //create the connection
        things.entity( thing1Id ).connection( "likes" ).entity( thing2Id ).post();


        refreshIndex(context.getOrgName(), context.getAppName());

        //test we have the "likes" in our connection meta data response

        JsonNode response = things.entity( "thing1" ).get();

        String url = getEntity( response, 0 ).get( "metadata" ).get( "connections" ).get( "likes" ).asText();


        assertNotNull( "Connection url returned in entity", url );

        //trim off the start /
        url = url.substring( 1 );


        //now that we know the URl is correct, follow it

        response = context.customCollection( url ).get();

        UUID returnedUUID = getEntityId( response, 0 );

        assertEquals( thing2Id, returnedUUID );

        //get on the collection works, now get it directly by uuid

        //now we should get thing1 from the loopback url

        response = things.entity( thing1Id ).connection( "likes" ).entity( thing2Id ).get();

        UUID returned = getEntityId( response, 0 );

        assertEquals( "Should point to thing2 as an entity connection", thing2Id, returned );
    }

    @Test //USERGRID-3011
    public void connectionsDeleteSecondEntityInConnectionTest() throws IOException {

        CustomCollection things = context.customCollection( "things" );

        UUID thing1Id = getEntityId( things.create( hashMap( "name", "thing1" ) ), 0 );

        UUID thing2Id = getEntityId( things.create( hashMap( "name", "thing2" ) ), 0 );

        refreshIndex(context.getOrgName(), context.getAppName());

        //create the connection
        things.entity( thing1Id ).connection( "likes" ).entity( thing2Id ).post();

        JsonNode response = things.entity( "thing2" ).delete();

        refreshIndex(context.getOrgName(), context.getAppName());

        JsonNode node = things.entity ( "thing2" ).get();

        assertNull(node);

    }

    @Test //USERGRID-3011
    public void connectionsDeleteFirstEntityInConnectionTest() throws IOException {

        CustomCollection things = context.customCollection( "things" );

        UUID thing1Id = getEntityId( things.create( hashMap( "name", "thing1" ) ), 0 );

        UUID thing2Id = getEntityId( things.create( hashMap( "name", "thing2" ) ), 0 );

        refreshIndex(context.getOrgName(), context.getAppName());

        //create the connection
        things.entity( thing1Id ).connection( "likes" ).entity( thing2Id ).post();

        JsonNode response = things.entity( "thing1" ).delete();

        refreshIndex(context.getOrgName(), context.getAppName());

        JsonNode node = things.entity ( "thing1" ).get();

        assertNull(node);

    }


}
