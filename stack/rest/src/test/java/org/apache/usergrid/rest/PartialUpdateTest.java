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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Partial update test. 
 */
public class PartialUpdateTest extends AbstractRestIT {
    private static final Logger log= LoggerFactory.getLogger(PartialUpdateTest.class );
    
    @Rule
    public TestContextSetup context = new TestContextSetup( this );

    @Test 
    public void testPartialUpdate() throws IOException {

        // create user bart

        Map<String, Object> userProperties = new LinkedHashMap<String, Object>() {{
            put( "username", "bart" );
            put( "employer", "Brawndo" );
            put( "email", "bart@personal-email.example.com" );
            put( "location", new LinkedHashMap<String, Object>() {{
                put("latitude", "37.3638875323994");
                put("longitude", "-122.12334411518498");
            }} );
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

        // Update bart's employer without specifying any required fields 
        // (with uuid specified in URL)

        Map<String, Object> updateProperties = new LinkedHashMap<String, Object>() {{
            put( "employer", "Initech" );
        }};

        try {
            mapper.readTree( 
                resource().path( "/test-organization/test-app/user/" + uuid ) 
                    .queryParam( "access_token", adminAccessToken )
                    .accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON )
                    .put( String.class, updateProperties ));

        } catch ( UniformInterfaceException uie ) {
            fail("Update failed due to: " + uie.getResponse().getEntity(String.class));
        }
        refreshIndex( "test-organization", "test-app" );

        userNode = mapper.readTree( 
            resource().path( "/test-organization/test-app/users/" + uuid )
                .queryParam( "access_token", adminAccessToken )
                .accept( MediaType.APPLICATION_JSON )
                .get( String.class ));
        assertNotNull( userNode );
        assertEquals( "Initech", userNode.withArray("entities").get(0).get("employer").asText());


        // Update bart's employer without specifying any required fields 
        // (this time with username specified in URL)

        updateProperties = new LinkedHashMap<String, Object>() {{
            put( "employer", "ACME Corporation" );
        }};

        try {
            mapper.readTree( 
                resource().path( "/test-organization/test-app/users/bart")
                    .queryParam( "access_token", adminAccessToken )
                    .accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON )
                    .put( String.class, updateProperties ));

        } catch ( UniformInterfaceException uie ) {
            fail("Update failed due to: " + uie.getResponse().getEntity(String.class));
        }
        refreshIndex( "test-organization", "test-app" );

        userNode = mapper.readTree( 
            resource().path( "/test-organization/test-app/users/bart" )
                .queryParam( "access_token", adminAccessToken )
                .accept( MediaType.APPLICATION_JSON )
                .get( String.class ));
        assertNotNull( userNode );
        assertEquals( "ACME Corporation", userNode.withArray("entities").get(0).get("employer").asText());
    }
}
