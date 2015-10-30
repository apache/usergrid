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
package org.apache.usergrid.rest.applications.collection.devices;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.endpoints.CollectionEndpoint;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.junit.Test;


import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Ignore;

import javax.ws.rs.ClientErrorException;


public class DevicesResourceIT extends AbstractRestIT {

    @Test
    public void putWithUUIDShouldCreateAfterDelete() throws IOException {

        Entity payload = new Entity().chainPut("name", "foo");
        UUID uuid = UUID.randomUUID();


        CollectionEndpoint devicesResource  =this.app().collection("devices");
        Entity entity = devicesResource.entity(uuid).put(payload);
        refreshIndex();

        // create
        assertNotNull( entity );
        String newUuid = entity.getUuid().toString();
        assertEquals( uuid.toString(), newUuid );

        // delete
        ApiResponse deleteResponse =devicesResource.entity(uuid).delete();
        assertNotNull(deleteResponse.getEntities().get(0));

        refreshIndex();

        // check deleted
        try {
            entity = devicesResource.entity(uuid).get();
            fail( "should get 404 error" );
        }
        catch ( ClientErrorException e ) {
            assertEquals( 404, e.getResponse().getStatus() );
        }
        refreshIndex();

        // create again
        entity = devicesResource.entity(uuid).put(payload);


        assertNotNull( entity );

        refreshIndex();

        // check existence
        entity = devicesResource.entity(uuid).get();

        assertNotNull(entity );
    }
}
