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
package org.apache.usergrid.rest.applications.events;


import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.rest.AbstractRestIT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Ignore;


public class EventsResourceIT extends AbstractRestIT {

    private static Logger log = LoggerFactory.getLogger( EventsResourceIT.class );


    @Test
    @Ignore("until we have a fix, see also: https://issues.apache.org/jira/browse/USERGRID-212")
    public void testEventPostandGet() throws IOException {

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put( "timestamp", 0 );
        payload.put( "category", "advertising" );
        payload.put( "counters", new LinkedHashMap<String, Object>() {
            {
                put( "ad_clicks", 5 );
            }
        } );

        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/events" )
                .queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .post( String.class, payload ));

        assertNotNull( node.get( "entities" ) );
        String advertising = node.get( "entities" ).get( 0 ).get( "uuid" ).asText();

        refreshIndex("test-organization","test-app");

        payload = new LinkedHashMap<String, Object>();
        payload.put( "timestamp", 0 );
        payload.put( "category", "sales" );
        payload.put( "counters", new LinkedHashMap<String, Object>() {
            {
                put( "ad_sales", 20 );
            }
        } );

        node = mapper.readTree( resource().path( "/test-organization/test-app/events" )
                .queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .post( String.class, payload ));

        assertNotNull( node.get( "entities" ) );
        String sales = node.get( "entities" ).get( 0 ).get( "uuid" ).asText();

        refreshIndex("test-organization","test-app");

        payload = new LinkedHashMap<String, Object>();
        payload.put( "timestamp", 0 );
        payload.put( "category", "marketing" );
        payload.put( "counters", new LinkedHashMap<String, Object>() {
            {
                put( "ad_clicks", 10 );
            }
        } );

        node = mapper.readTree( resource().path( "/test-organization/test-app/events" )
                .queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .post( String.class, payload ));

        assertNotNull( node.get( "entities" ) );
        String marketing = node.get( "entities" ).get( 0 ).get( "uuid" ).asText();

        refreshIndex("test-organization","test-app");

        String lastId = null;

        // subsequent GETs advertising
        for ( int i = 0; i < 3; i++ ) {

            node = mapper.readTree( resource().path( "/test-organization/test-app/events" )
                    .queryParam( "access_token", access_token )
                    .accept( MediaType.APPLICATION_JSON )
                    .type( MediaType.APPLICATION_JSON_TYPE )
                    .get( String.class ));
            logNode( node );
            assertEquals( "Expected Advertising", advertising, node.get( "messages" ).get( 0 ).get( "uuid" ).asText() );
            lastId = node.get( "last" ).asText();
        }

        // check sales event in queue
        node = mapper.readTree( resource().path( "/test-organization/test-app/events" )
                .queryParam( "last", lastId )
                .queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .get( String.class ));

        logNode( node );
        assertEquals( "Expected Sales", sales, node.get( "messages" ).get( 0 ).get( "uuid" ).asText() );
        lastId = node.get( "last" ).asText();


        // check marketing event in queue
        node = mapper.readTree( resource().path( "/test-organization/test-app/events" )
                .queryParam( "last", lastId )
                .queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .get( String.class ));

        logNode( node );
        assertEquals( "Expected Marketing", marketing, node.get( "messages" ).get( 0 ).get( "uuid" ).asText() );
    }
}
