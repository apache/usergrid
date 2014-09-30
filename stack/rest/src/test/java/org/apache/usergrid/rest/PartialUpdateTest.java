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
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotEquals;


/**
 * Partial update test. 
 */
public class PartialUpdateTest extends AbstractRestIT {
    private static final Logger log= LoggerFactory.getLogger(PartialUpdateTest.class );
    
    @Rule
    public TestContextSetup context = new TestContextSetup( this );

    double latitude=37.772837;
    double longitude=-122.409895;

    Map<String, Object> geolocation = new LinkedHashMap<String, Object>() {{
        put("latitude", latitude);
        put("longitude", longitude);
    }};

    @Test 
    public void testPartialUpdate() throws IOException {

        // create user bart
        Map<String, Object> userProperties = new LinkedHashMap<String, Object>() {{
            put( "username", "bart" );
            put( "employer", "Brawndo" );
            put( "email", "bart@personal-email.example.com" );
            put( "location", geolocation);
        }};

        JsonNode userNode = mapper.readTree( 
            resource().path( "/test-organization/test-app/users" )
                .queryParam( "access_token", adminAccessToken )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON )
                .post( String.class, userProperties ));

        assertNotNull( userNode );
        String uuid = userNode.withArray("entities").get(0).get("uuid").asText();
        assertNotNull( uuid );
        refreshIndex( "test-organization", "test-app" );

        Map<String, Object> updateProperties = new LinkedHashMap<String, Object>();
        // update user bart passing only an update to a property
        for(int i=1; i<10; i++) {
            geolocation.put("latitude", latitude += 0.00001);
            geolocation.put("longitude", longitude += 0.00001);
            updateProperties.put("employer", "Initech");
            updateProperties.put("location", geolocation);

            try {
                JsonNode updatedNode = mapper.readTree(
                        resource().path("/test-organization/test-app/user/" + uuid)
                                .queryParam("access_token", adminAccessToken)
                                .accept(MediaType.APPLICATION_JSON)
                                .type(MediaType.APPLICATION_JSON)
                                .put(String.class, updateProperties));
                assertNotNull(updatedNode);

            } catch (UniformInterfaceException uie) {
                fail("Update failed due to: " + uie.getResponse().getEntity(String.class));
            }

            refreshIndex("test-organization", "test-app");

            userNode = mapper.readTree(
                    resource().path("/test-organization/test-app/users/" + uuid)
                            .queryParam("access_token", adminAccessToken)
                            .accept(MediaType.APPLICATION_JSON)
                            .get(String.class));
            assertNotNull(userNode);
            assertEquals("Initech", userNode.withArray("entities").get(0).get("employer").asText());
            assertNotEquals(latitude, userNode.withArray("entities").get(0).get("location").get("latitude"));
            assertNotEquals(longitude, userNode.withArray("entities").get(0).get("location").get("longitude"));
        }

        // Update bart's employer without specifying any required fields 
        // (this time with username specified in URL)

        updateProperties = new LinkedHashMap<String, Object>() {{
            put( "employer", "ACME Corporation" );
        }};

        for(int i=1; i<10; i++) {
            try {
                mapper.readTree(
                        resource().path("/test-organization/test-app/users/bart")
                                .queryParam("access_token", adminAccessToken)
                                .accept(MediaType.APPLICATION_JSON)
                                .type(MediaType.APPLICATION_JSON)
                                .put(String.class, updateProperties));

            } catch (UniformInterfaceException uie) {
                fail("Update failed due to: " + uie.getResponse().getEntity(String.class));
            }
            refreshIndex("test-organization", "test-app");

            userNode = mapper.readTree(
                    resource().path("/test-organization/test-app/users/bart")
                            .queryParam("access_token", adminAccessToken)
                            .accept(MediaType.APPLICATION_JSON)
                            .get(String.class));
            assertNotNull(userNode);
            assertEquals("ACME Corporation", userNode.withArray("entities").get(0).get("employer").asText());
        }
    }
}
