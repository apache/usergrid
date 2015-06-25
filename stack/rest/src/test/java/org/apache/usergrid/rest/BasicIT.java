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


import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.security.TestAppUser;
import org.apache.usergrid.rest.test.security.TestUser;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.junit.Assert.*;


public class BasicIT extends AbstractRestIT {

    private static final Logger LOG = LoggerFactory.getLogger( BasicIT.class );

    @Rule
    public TestContextSetup context = new TestContextSetup( this );

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
        String orgAppPath = "/"+context.getOrgName()+"/"+context.getAppName();
        TestUser testUser = new TestAppUser( "temp"+ UUIDUtils.newTimeUUID(),
            "password","temp"+UUIDUtils.newTimeUUID()+"@usergrid.com"  ).create( context );

        //String token = userToken( "ed@anuff.com", "sesame" );
        WebResource resource = resource().path( orgAppPath+"/suspects" )
            .queryParam( "access_token", context.getActiveUser().getToken() );
        node = mapper.readTree( resource.accept( MediaType.APPLICATION_JSON ).post( String.class ));


        String uuid = "4dadf156-c82f-4eb7-a437-3e574441c4db";

        // Notice for 'name' we replace the dash in uuid string
        // with 0's making it no longer conforms to a uuid
        Map<String, String> payload = hashMap( "hair", "brown" ).map( "sex", "male" ).map( "eyes", "green" )
                .map( "name", uuid.replace( '-', '0' ) ).map( "build", "thin" ).map( "height", "6 4" );

        node = mapper.readTree( resource.queryParam( "access_token",
            context.getActiveUser().getToken() ).accept( MediaType.APPLICATION_JSON )
            .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));

        logNode( node );

        // Now this should pass with the corrections made to USERGRID-2099 which
        // disables conversion of uuid strings into UUID objects in JsonUtils
        payload = hashMap( "hair", "red" ).map( "sex", "female" ).map( "eyes", "blue" ).map( "name", uuid )
                .map( "build", "heavy" ).map( "height", "5 9" );

        node = mapper.readTree( resource.accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                       .post( String.class, payload ));

        logNode( node );
    }


    @Test
    public void testNonexistentUserAccessViaGuest() throws IOException {
        JsonNode node = null;

        try {
            WebResource resource = resource();
            resource.path( "/test-organization/test-app/users/foobarNonexistent" );
            resource.accept( MediaType.APPLICATION_JSON );
            node = mapper.readTree( resource.get( String.class ));
        }
        catch ( UniformInterfaceException e ) {
            logNode( node );
            assertEquals( "Guests should not be able to get a 404", 401, e.getResponse().getStatus() );
        }
    }


    @Test
    public void testToken() throws IOException {
        JsonNode node = null;

        String orgName = context.getOrgName();
        String appName = context.getAppName();
        String username = context.getActiveUser().getUser();
        String password = context.getActiveUser().getPassword();
        String mgmtToken = context.getActiveUser().getToken();


        // test get token for admin user with bad password

        boolean err_thrown = false;
        try {
            node = mapper.readTree( resource().path( "/management/token" ).queryParam( "grant_type", "password" )
                    .queryParam( "username", username ).queryParam( "password", "blahblah" )
                    .accept( MediaType.APPLICATION_JSON ).get( String.class ));
        }
        catch ( UniformInterfaceException e ) {
            assertEquals( "Should receive a 400 Bad Request", 400, e.getResponse().getStatus() );
            err_thrown = true;
        }
        assertTrue( "Error should have been thrown", err_thrown );

        // test get token for admin user with correct default password

        // test get admin user with token

        node = mapper.readTree( resource().path( "/management/users/"+username ).queryParam( "access_token", mgmtToken )
                .accept( MediaType.APPLICATION_JSON ).get( String.class ));

        logNode( node );

        assertEquals( username, node.get( "data" ).get( "organizations" ).get( orgName.toLowerCase() )
                    .get( "users" ).get( username ).get("name").textValue());


        // test login user with incorrect password

        err_thrown = false;
        try {
            node = mapper.readTree( resource().path( appName+"/token" ).queryParam( "grant_type", "password" )
                    .queryParam( "username", username ).queryParam( "password", "blahblah" )
                    .accept( MediaType.APPLICATION_JSON ).get( String.class ));
        }
        catch ( UniformInterfaceException e ) {
            assertEquals( "Should receive a 400 Bad Request", 400, e.getResponse().getStatus() );
            err_thrown = true;
        }
        assertTrue( "Error should have been thrown", err_thrown );

        // test login user with incorrect pin

        err_thrown = false;
        try {
            node = mapper.readTree( resource().path( appName+"/token" ).queryParam( "grant_type", "pin" )
                    .queryParam( "username", username ).queryParam( "pin", "4321" )
                    .accept( MediaType.APPLICATION_JSON ).get( String.class ));
        }
        catch ( UniformInterfaceException e ) {
            assertEquals( "Should receive a 400 Bad Request", 400, e.getResponse().getStatus() );
            err_thrown = true;
        }
        assertTrue( "Error should have been thrown", err_thrown );

        // test login user with correct password
        TestUser testUser = new TestAppUser( "temp"+ UUIDUtils.newTimeUUID(),"password",
            "temp"+UUIDUtils.newTimeUUID()+"@usergrid.com"  ).create( context );

        node = mapper.readTree( resource().path( "/"+orgName+"/"+appName+"/token" )
            .queryParam( "grant_type", "password" )
            .queryParam( "username", testUser.getUser() ).queryParam( "password", testUser.getPassword())
            .accept( MediaType.APPLICATION_JSON ).get( String.class ));

        logNode( node );

        String user_access_token = node.get( "access_token" ).textValue();
        assertTrue( isNotBlank( user_access_token ) );

        // test get app user collection with insufficient permissions

        err_thrown = false;
        try {
            node = mapper.readTree( resource().path( "/"+orgName+"/"+appName+"/users" )
                    .queryParam( "access_token", user_access_token ).accept( MediaType.APPLICATION_JSON )
                    .get( String.class ));
        }
        catch ( UniformInterfaceException e ) {
            if ( e.getResponse().getStatus() != 401 ) {
                throw e;
            }
            err_thrown = true;
        }

        // test get app user with sufficient permissions

        node = mapper.readTree( resource().path( "/"+orgName+"/"+appName+"/users/"+testUser.getUser() )
                .queryParam( "access_token", user_access_token ).accept( MediaType.APPLICATION_JSON )
                .get( String.class ));
        logNode( node );

        assertEquals( 1, node.get( "entities" ).size() );

        // test get app user collection with bad token

        err_thrown = false;
        try {
            node = mapper.readTree( resource().path( "/"+orgName+"/"+appName+"/users" )
                .queryParam( "access_token", "blahblahblah" )
                .accept( MediaType.APPLICATION_JSON ).get( String.class ));
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
            node = mapper.readTree( resource().path( "/"+orgName+"/"+appName+"/users" )
                    .accept( MediaType.APPLICATION_JSON )
                    .get( String.class ));
        }
        catch ( UniformInterfaceException e ) {
            assertEquals( "Should receive a 401 Unauthorized", 401, e.getResponse().getStatus() );
            err_thrown = true;
        }
        assertTrue( "Error should have been thrown", err_thrown );

        // test set app user pin

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add( "pin", "5678" );
        node = mapper.readTree( resource()
                .path( "/"+orgName+"/"+appName+"/users/"+testUser.getUser()+"/setpin" )
                .queryParam( "access_token", user_access_token )
                .type( "application/x-www-form-urlencoded" )
                .post( String.class, formData ));

        refreshIndex(orgName, appName);

        node = mapper.readTree( resource()
                .path( "/"+orgName+"/"+appName+"/token" )
                .queryParam( "grant_type", "pin" )
                .queryParam( "username", testUser.getUser() )
                .queryParam( "pin", "5678" )
                .accept( MediaType.APPLICATION_JSON )
                .get( String.class ));

        logNode( node );

        node = mapper.readTree( resource().path( "/"+orgName+"/"+appName+"/token" )
                                          .queryParam( "grant_type", "pin" )
                                          .queryParam( "username", testUser.getUser() )
                                          .queryParam( "pin", "5678" )
                                          .accept( MediaType.APPLICATION_JSON ).get( String.class ));

        logNode( node );

        user_access_token = node.get( "access_token" ).textValue();
        assertTrue( isNotBlank( user_access_token ) );

        refreshIndex(orgName, appName);

        // test user test extension resource

        node = mapper.readTree( resource()
                .path( "/"+orgName+"/"+appName+"/users/"+testUser.getUser()+"/test" )
                .queryParam( "access_token", user_access_token )
                .get( String.class ));
        logNode( node );

        // test create user with guest permissions (no token)

        String testUsername="burritos"+UUIDUtils.newTimeUUID();
        String testEmail="burritos"+ UUIDUtils.newTimeUUID()+"@usergrid.com";
        String testPassword= "burritos";
        String testPin = "1234";

        Map<String, String> payload =
                hashMap( "email", testEmail).map( "username", testUsername ).map( "name", testUsername )
                        .map( "password", testPassword ).map( "pin", testPin );

        node = mapper.readTree( resource().path( "/"+orgName+"/"+appName+"/users" ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));

        logNode( node );

        assertNotNull( node.get( "entities" ) );
        assertNotNull( node.get( "entities" ).get( 0 ) );
        assertNotNull( node.get( "entities" ).get( 0 ).get( "username" ) );
        assertEquals( testUsername, node.get( "entities" ).get( 0 ).get( "username" ).textValue() );

        // test create device with guest permissions (no token)

        //can't find devices endpoint. I'm not entirely sure this part of valid anymore
//        payload = hashMap( "foo", "bar" );
//
//        node = mapper.readTree( resource().path(  "/"+orgName+"/"+appName+"/devices/" + UUIDGenerator.newTimeUUID() )
//                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
//                .put( String.class, payload ));
//
//        logNode( node );

        // test create entity with guest permissions (no token), should fail

        payload = hashMap( "foo", "bar" );

        err_thrown = false;
        try {
            node = mapper.readTree( resource().path(  "/"+orgName+"/"+appName+"/items" )
                .accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));
        }
        catch ( UniformInterfaceException e ) {
            assertEquals( "Should receive a 401 Unauthorized", 401, e.getResponse().getStatus() );
            err_thrown = true;
        }
        assertTrue( "Error should have been thrown", err_thrown );
    }
}
