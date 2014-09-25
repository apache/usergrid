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
package org.apache.usergrid.rest.applications.collection;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.utils.UUIDUtils;

import com.sun.jersey.api.client.UniformInterfaceException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.apache.usergrid.utils.MapUtils.hashMap;


/**
 * @author zznate
 * @author tnine
 */
@Concurrent()
public class CollectionsResourceIT extends AbstractRestIT {

    private static Logger log = LoggerFactory.getLogger( CollectionsResourceIT.class );


    @Test
    public void postToBadPath() throws IOException {
        Map<String, String> payload = hashMap( "name", "Austin" ).map( "state", "TX" );
        JsonNode node = null;
        try {
            node = mapper.readTree( resource().path( "/test-organization/test-organization/test-app/cities" )
                    .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));
        }
        catch ( UniformInterfaceException e ) {
            assertEquals( "Should receive a 400 Not Found", 400, e.getResponse().getStatus() );
        }
    }


    @Test
    public void postToEmptyCollection() throws IOException {
        Map<String, String> payload = new HashMap<String, String>();

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/cities" ).queryParam( "access_token", access_token )
                        .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                        .post( String.class, payload ));
        assertNull( getEntity( node, 0 ) );
        assertNull( node.get( "count" ) );
    }


    /**
     * emails with "me" in them are causing errors. Test we can post to a colleciton after creating a user with this
     * email
     * <p/>
     * USERGRID-689
     */
    @Test
    public void permissionWithMeInString() throws Exception {
        // user is created get a token
        createUser( "sumeet.agarwal@usergrid.com", "sumeet.agarwal@usergrid.com", "secret", "Sumeet Agarwal" );
        refreshIndex("test-organization", "test-app");

        String token = userToken( "sumeet.agarwal@usergrid.com", "secret" );


        //create a permission with the path "me" in it
        Map<String, String> data = new HashMap<String, String>();

        data.put( "permission", "get,post,put,delete:/users/sumeet.agarwal@usergrid.com/**" );

        String path = "/test-organization/test-app/users/sumeet.agarwal@usergrid.com/permissions";
        JsonNode posted = mapper.readTree( resource().path( path ).queryParam( "access_token", token ).accept( MediaType.APPLICATION_JSON )
                        .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, data ));


        //now post data
        data = new HashMap<String, String>();

        data.put( "name", "profile-sumeet" );
        data.put( "firstname", "sumeet" );
        data.put( "lastname", "agarwal" );
        data.put( "mobile", "122" );


        posted = mapper.readTree( resource().path( "/test-organization/test-app/nestprofiles" ).queryParam( "access_token", token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                .post( String.class, data ));

        refreshIndex("test-organization", "test-app");

        JsonNode response = mapper.readTree( resource().path( "/test-organization/test-app/nestprofiles" ).queryParam( "access_token", token )
                        .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                        .get( String.class ));

        assertNotNull( getEntity( response, 0 ) );
        assertNotNull( response.get( "count" ) );
    }


    @Test
    public void stringWithSpaces() throws IOException {
        Map<String, String> payload = hashMap( "summaryOverview", "My Summary" ).map( "caltype", "personal" );

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/calendarlists" )
                .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));


        UUID id = getEntityId( node, 0 );

        //post a second entity


        payload = hashMap( "summaryOverview", "Your Summary" ).map( "caltype", "personal" );

        node = mapper.readTree( resource().path( "/test-organization/test-app/calendarlists" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                .post( String.class, payload ));


        refreshIndex("test-organization", "test-app");

        //query for the first entity

        String query = "summaryOverview = 'My Summary'";


        JsonNode queryResponse = mapper.readTree( resource().path( "/test-organization/test-app/calendarlists" )
                .queryParam( "access_token", access_token ).queryParam( "ql", query )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));


        UUID returnedId = getEntityId( queryResponse, 0 );

        assertEquals( id, returnedId );

        assertEquals( 1, queryResponse.get( "entities" ).size() );
    }


    /**
     * Test to verify "name property returns twice in AppServices response" is fixed.
     * https://apigeesc.atlassian.net/browse/USERGRID-2318
     */
    @Test
    public void testNoDuplicateFields() throws Exception {

        {
            // create an "app_user" object with name fred
            Map<String, String> payload = hashMap( "type", "app_user" ).map( "name", "fred" );

            JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/app_users" )
                    .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, payload ));

            String uuidString = node.get( "entities" ).get( 0 ).get( "uuid" ).asText();
            UUID entityId = UUIDUtils.tryGetUUID( uuidString );
            Assert.assertNotNull( entityId );
        }

        refreshIndex("test-organization", "test-app");

        {
            // check REST API response for duplicate name property
            // have to look at raw response data, Jackson will remove dups
            String s = resource().path( "/test-organization/test-app/app_users/fred" )
                    .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class );

            int firstFred = s.indexOf( "fred" );
            int secondFred = s.indexOf( "fred", firstFred + 4 );
            Assert.assertEquals( "Should not be more than one name property", -1, secondFred );
        }
    }
}
