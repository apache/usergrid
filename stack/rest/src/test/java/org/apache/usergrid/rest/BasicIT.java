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
package org.apache.usergrid.rest;


import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.assertNotNull;


public class BasicIT extends AbstractRestIT {

    private static final Logger LOG = LoggerFactory.getLogger( BasicIT.class );


    public BasicIT() throws Exception {
        super();
    }


    public void tryTest() {
        WebResource webResource = resource();
        String json = webResource.path( "/test/hello" ).accept( MediaType.APPLICATION_JSON ).get( String.class );
        assertTrue( isNotBlank( json ) );

        LOG.info( json );
    }


    /**
     * For USERGRID-2099 where putting an entity into a generic collection is resulting in a CCE when the name is a UUID
     * string.
     */
    @Test
    public void testGenericCollectionEntityNameUuid() throws Exception {
        JsonNode node = null;

        String token = userToken( "ed@anuff.com", "sesame" );
        WebResource resource =
                resource().path( "/test-organization/test-app/suspects" ).queryParam( "access_token", token );
        node = resource.accept( MediaType.APPLICATION_JSON ).post( JsonNode.class );


        String uuid = "4dadf156-c82f-4eb7-a437-3e574441c4db";

        // Notice for 'name' we replace the dash in uuid string
        // with 0's making it no longer conforms to a uuid
        Map<String, String> payload = hashMap( "hair", "brown" ).map( "sex", "male" ).map( "eyes", "green" )
                .map( "name", uuid.replace( '-', '0' ) ).map( "build", "thin" ).map( "height", "6 4" );

        node = resource.queryParam( "access_token", token ).accept( MediaType.APPLICATION_JSON )
                       .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );

        logNode( node );

        // Now this should pass with the corrections made to USERGRID-2099 which
        // disables conversion of uuid strings into UUID objects in JsonUtils
        payload = hashMap( "hair", "red" ).map( "sex", "female" ).map( "eyes", "blue" ).map( "name", uuid )
                .map( "build", "heavy" ).map( "height", "5 9" );

        node = resource.accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                       .post( JsonNode.class, payload );

        logNode( node );
    }


    @Test
    public void testNonexistentUserAccessViaGuest() {
        JsonNode node = null;

        try {
            WebResource resource = resource();
            resource.path( "/test-organization/test-app/users/foobarNonexistent" );
            resource.accept( MediaType.APPLICATION_JSON );
            node = resource.get( JsonNode.class );
        }
        catch ( UniformInterfaceException e ) {
            logNode( node );
            assertEquals( "Guests should not be able to get a 404", 401, e.getResponse().getStatus() );
        }
    }


    @Test
    public void testToken() {
        JsonNode node = null;

        // test get token for admin user with bad password

        boolean err_thrown = false;
        try {
            node = resource().path( "/management/token" ).queryParam( "grant_type", "password" )
                    .queryParam( "username", "test@usergrid.com" ).queryParam( "password", "blahblah" )
                    .accept( MediaType.APPLICATION_JSON ).get( JsonNode.class );
        }
        catch ( UniformInterfaceException e ) {
            assertEquals( "Should receive a 400 Bad Request", 400, e.getResponse().getStatus() );
            err_thrown = true;
        }
        assertTrue( "Error should have been thrown", err_thrown );

        // test get token for admin user with correct default password

        String mgmtToken = adminToken();
        // test get admin user with token

        node = resource().path( "/management/users/test@usergrid.com" ).queryParam( "access_token", mgmtToken )
                .accept( MediaType.APPLICATION_JSON ).get( JsonNode.class );

        logNode( node );

        assertEquals( "Test User",
                node.get( "data" ).get( "organizations" ).get( "test-organization" ).get( "users" ).get( "test" )
                    .get( "name" ).getTextValue() );


        // test login user with incorrect password

        err_thrown = false;
        try {
            node = resource().path( "/test-app/token" ).queryParam( "grant_type", "password" )
                    .queryParam( "username", "ed@anuff.com" ).queryParam( "password", "blahblah" )
                    .accept( MediaType.APPLICATION_JSON ).get( JsonNode.class );
        }
        catch ( UniformInterfaceException e ) {
            assertEquals( "Should receive a 400 Bad Request", 400, e.getResponse().getStatus() );
            err_thrown = true;
        }
        assertTrue( "Error should have been thrown", err_thrown );

        // test login user with incorrect pin

        err_thrown = false;
        try {
            node = resource().path( "/test-app/token" ).queryParam( "grant_type", "pin" )
                    .queryParam( "username", "ed@anuff.com" ).queryParam( "pin", "4321" )
                    .accept( MediaType.APPLICATION_JSON ).get( JsonNode.class );
        }
        catch ( UniformInterfaceException e ) {
            assertEquals( "Should receive a 400 Bad Request", 400, e.getResponse().getStatus() );
            err_thrown = true;
        }
        assertTrue( "Error should have been thrown", err_thrown );

        // test login user with correct password

        node = resource().path( "/test-organization/test-app/token" ).queryParam( "grant_type", "password" )
                .queryParam( "username", "ed@anuff.com" ).queryParam( "password", "sesame" )
                .accept( MediaType.APPLICATION_JSON ).get( JsonNode.class );

        logNode( node );

        String user_access_token = node.get( "access_token" ).getTextValue();
        assertTrue( isNotBlank( user_access_token ) );

        // test get app user collection with insufficient permissions

        err_thrown = false;
        try {
            node = resource().path( "/test-organization/test-app/users" )
                    .queryParam( "access_token", user_access_token ).accept( MediaType.APPLICATION_JSON )
                    .get( JsonNode.class );
        }
        catch ( UniformInterfaceException e ) {
            if ( e.getResponse().getStatus() != 401 ) {
                throw e;
            }
            err_thrown = true;
        }
        // assertTrue("Error should have been thrown", err_thrown);

        // test get app user with sufficient permissions

        node = resource().path( "/test-organization/test-app/users/edanuff" )
                .queryParam( "access_token", user_access_token ).accept( MediaType.APPLICATION_JSON )
                .get( JsonNode.class );
        logNode( node );

        assertEquals( 1, node.get( "entities" ).size() );

        // test get app user collection with bad token

        err_thrown = false;
        try {
            node = resource().path( "/test-organization/test-app/users" ).queryParam( "access_token", "blahblahblah" )
                    .accept( MediaType.APPLICATION_JSON ).get( JsonNode.class );
        }
        catch ( UniformInterfaceException e ) {
            if ( e.getResponse().getStatus() != 401 ) {
                throw e;
            }
            err_thrown = true;
        }
        assertTrue( "Error should have been thrown", err_thrown );

        // test get app user collection with no token

        err_thrown = false;
        try {
            node = resource().path( "/test-organization/test-app/users" ).accept( MediaType.APPLICATION_JSON )
                    .get( JsonNode.class );
        }
        catch ( UniformInterfaceException e ) {
            assertEquals( "Should receive a 401 Unauthorized", 401, e.getResponse().getStatus() );
            err_thrown = true;
        }
        assertTrue( "Error should have been thrown", err_thrown );

        // test login app user with pin

        node = resource().path( "/test-organization/test-app/token" ).queryParam( "grant_type", "pin" )
                .queryParam( "username", "ed@anuff.com" ).queryParam( "pin", "1234" )
                .accept( MediaType.APPLICATION_JSON ).get( JsonNode.class );

        logNode( node );

        user_access_token = node.get( "access_token" ).getTextValue();
        assertTrue( isNotBlank( user_access_token ) );

        // test set app user pin

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add( "pin", "5678" );
        node = resource().path( "/test-organization/test-app/users/ed@anuff.com/setpin" )
                .queryParam( "access_token", user_access_token ).type( "application/x-www-form-urlencoded" )
                .post( JsonNode.class, formData );

        node = resource().path( "/test-organization/test-app/token" ).queryParam( "grant_type", "pin" )
                .queryParam( "username", "ed@anuff.com" ).queryParam( "pin", "5678" )
                .accept( MediaType.APPLICATION_JSON ).get( JsonNode.class );

        logNode( node );

        user_access_token = node.get( "access_token" ).getTextValue();
        assertTrue( isNotBlank( user_access_token ) );

        // test user test extension resource

        node = resource().path( "/test-organization/test-app/users/ed@anuff.com/test" ).get( JsonNode.class );
        logNode( node );

        // test create user with guest permissions (no token)

        Map<String, String> payload =
                hashMap( "email", "ed.anuff@gmail.com" ).map( "username", "ed.anuff" ).map( "name", "Ed Anuff" )
                        .map( "password", "sesame" ).map( "pin", "1234" );

        node = resource().path( "/test-organization/test-app/users" ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );

        logNode( node );

        assertNotNull( node.get( "entities" ) );
        assertNotNull( node.get( "entities" ).get( 0 ) );
        assertNotNull( node.get( "entities" ).get( 0 ).get( "username" ) );
        assertEquals( "ed.anuff", node.get( "entities" ).get( 0 ).get( "username" ).getTextValue() );

        // test create device with guest permissions (no token)

        payload = hashMap( "foo", "bar" );

        node = resource().path( "/test-organization/test-app/devices/" + UUIDGenerator.newTimeUUID() )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                .put( JsonNode.class, payload );

        logNode( node );

        // test create entity with guest permissions (no token), should fail

        payload = hashMap( "foo", "bar" );

        err_thrown = false;
        try {
            node = resource().path( "/test-organization/test-app/items" ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( JsonNode.class, payload );
        }
        catch ( UniformInterfaceException e ) {
            assertEquals( "Should receive a 401 Unauthorized", 401, e.getResponse().getStatus() );
            err_thrown = true;
        }
        assertTrue( "Error should have been thrown", err_thrown );
    }
}
