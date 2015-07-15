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

import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Ignore;


public class EventsResourceIT extends AbstractRestIT {

    private static Logger log = LoggerFactory.getLogger( EventsResourceIT.class );


    @Test
    @Ignore("Events not working yet")
    public void testEventPostandGet() throws IOException {

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put( "timestamp", 0 );
        payload.put("category", "advertising");
        payload.put("counters", new LinkedHashMap<String, Object>() {
            {
                put("ad_clicks", 5);
            }
        });

        ApiResponse node = this.app().collection("events")
            .post(payload);

        assertNotNull(node.getEntities());
        String advertising = node.getEntity().get("uuid").toString();

        refreshIndex();

        payload = new LinkedHashMap<String, Object>();
        payload.put( "timestamp", 0 );
        payload.put( "category", "sales" );
        payload.put( "counters", new LinkedHashMap<String, Object>() {
            {
                put( "ad_sales", 20 );
            }
        } );

        node = this.app().collection("events")
                .post(  payload );

        assertNotNull(node.getEntities());
        String sales = node.getEntity().get("uuid").toString();

        refreshIndex( );

        payload = new LinkedHashMap<String, Object>();
        payload.put( "timestamp", 0 );
        payload.put( "category", "marketing" );
        payload.put( "counters", new LinkedHashMap<String, Object>() {
            {
                put( "ad_clicks", 10 );
            }
        } );

        node = this.app().collection( "events" )
            .post(payload);

        assertNotNull(node.getEntities());
        String marketing = node.getEntity().get( "uuid" ).toString();

        refreshIndex();

        String lastId = null;

        Collection collection;
        // subsequent GETs advertising
        for ( int i = 0; i < 3; i++ ) {

            collection = this.app().collection( "events" )
                .get();

            assertEquals("Expected Advertising", advertising, ((Map<String, Object>) ((Map<String, Object>) collection.getResponse().getProperties().get("messages")).get(0)).get("uuid").toString());
            lastId = collection.getResponse().getProperties().get("last").toString();
        }

        // check sales event in queue
        collection = this.app().collection( "events" )
            .get();


        assertEquals( "Expected Sales", sales,((Map<String, Object>) ((Map<String, Object>) collection.getResponse().getProperties().get("messages")).get(0)).get("uuid").toString());
        lastId = collection.getResponse().getProperties().get("last").toString();


        // check marketing event in queue
        collection = this.app().collection( "events" )
            .get();

        assertEquals( "Expected Marketing", marketing, ((Map<String, Object>) ((Map<String, Object>) collection.getResponse().getProperties().get("messages")).get(0)).get("uuid").toString());
    }
}
