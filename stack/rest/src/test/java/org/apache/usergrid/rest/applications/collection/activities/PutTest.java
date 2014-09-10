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
package org.apache.usergrid.rest.applications.collection.activities;


import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.UniformInterfaceException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.CustomCollection;
import static org.apache.usergrid.utils.MapUtils.hashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test update and partial update.
 */
public class PutTest extends AbstractRestIT {
    private static final Logger log= LoggerFactory.getLogger( PutTest.class );
    
    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test //USERGRID-545
    public void putMassUpdateTest() throws IOException {

        CustomCollection activities = context.collection( "activities" );

        Map actor = hashMap( "displayName", "Erin" );
        Map newActor = hashMap( "displayName", "Bob" );
        Map props = new HashMap();

        props.put( "actor", actor );
        props.put( "verb", "go" );
        props.put( "content", "bragh" );


        for ( int i = 0; i < 5; i++ ) {
            props.put( "ordinal", i );
            JsonNode activity = activities.create( props );
        }

        refreshIndex(context.getOrgName(), context.getAppName());

        String query = "select * ";

        JsonNode node = activities.withQuery( query ).get();
        String uuid = node.get( "entities" ).get( 0 ).get( "uuid" ).textValue();
        StringBuilder buf = new StringBuilder( uuid );

        activities.addToUrlEnd( buf );
        props.put( "actor", newActor );
        node = activities.put( props );

        refreshIndex(context.getOrgName(), context.getAppName());

        node = activities.withQuery( query ).get();
        assertEquals( 6, node.get( "entities" ).size() );
    }

    @Test 
    public void testPartialUpdate() throws IOException {

        // create user bart

        Map<String, Object> userProperties = new LinkedHashMap<String, Object>() {{
            put( "username", "bart" );
            put( "employer", "Brawndo" );
            put( "email", "bart@personal-email.example.com" );
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

        // update user bart passing only an update to his employer

        Map<String, Object> updateProperties = new LinkedHashMap<String, Object>() {{
            put( "employer", "Initech" );
        }};

        try {
            JsonNode updatedNode = mapper.readTree( 
                resource().path( "/test-organization/test-app/user/" + uuid )
                    .queryParam( "access_token", adminAccessToken )
                    .accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON )
                    .put( String.class, updateProperties ));

        } catch ( UniformInterfaceException uie ) {
            fail("Update failed due to: " + uie.getResponse().getEntity(String.class));
        }

    }
}
