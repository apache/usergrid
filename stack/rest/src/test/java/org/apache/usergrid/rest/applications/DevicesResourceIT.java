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
package org.apache.usergrid.rest.applications;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.rest.AbstractRestIT;

import com.sun.jersey.api.client.UniformInterfaceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


@Concurrent()
public class DevicesResourceIT extends AbstractRestIT {

    @Test
    public void putWithUUIDShouldCreateAfterDelete() {

        Map<String, String> payload = new HashMap<String, String>();
        UUID uuid = UUID.randomUUID();
        payload.put( "name", "foo" );

        String path = "devices/" + uuid;

        JsonNode response = appPath( path ).put( JsonNode.class, payload );

        // create
        JsonNode entity = getEntity( response, 0 );
        assertNotNull( entity );
        String newUuid = entity.get( "uuid" ).getTextValue();
        assertEquals( uuid.toString(), newUuid );

        // delete
        response = appPath( path ).delete( JsonNode.class );
        assertNotNull( getEntity( response, 0 ) );

        // check deleted
        try {
            response = appPath( path ).get( JsonNode.class );
            fail( "should get 404 error" );
        }
        catch ( UniformInterfaceException e ) {
            assertEquals( 404, e.getResponse().getStatus() );
        }

        // create again
        response = appPath( path ).put( JsonNode.class, payload );
        entity = getEntity( response, 0 );
        assertNotNull( entity );

        // check existence
        response = appPath( path ).get( JsonNode.class );
        assertNotNull( getEntity( response, 0 ) );
    }
}
