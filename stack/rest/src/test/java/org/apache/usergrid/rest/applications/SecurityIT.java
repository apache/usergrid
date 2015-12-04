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


import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.utils.UUIDUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import javax.ws.rs.core.MediaType;
import java.util.UUID;
import static org.junit.Assert.assertEquals;

/**
 * These tests will execute requests against certain paths (with or without credentials) to ensure access is being
 * allowed according to the REST and Services permissions defined for the resource.
 */
public class SecurityIT extends AbstractRestIT {

    final String BASE_PATH = "/test-organization/test-app";
    final int UNAUTHORIZED_STATUS = 401;

    @Test
    public void testAssetsNoCredentials(){

        final UUID uuid = UUIDUtils.newTimeUUID();
        int responseStatus = 0;

        try {
            // intentionally do not add access_token
            resource().path(BASE_PATH + "/assets/" + uuid + "/data")
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(JsonNode.class);
        }
        catch (UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getStatus();
        }
        assertEquals(UNAUTHORIZED_STATUS, responseStatus);
    }

    @Test
    public void testFacebookAuthNoCredentials(){

        int responseStatus = 0;

        try {
            // intentionally do not add access_token
            resource().path(BASE_PATH + "/auth/facebook")
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(JsonNode.class);
        }
        catch (UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getStatus();
        }
        assertEquals(UNAUTHORIZED_STATUS, responseStatus);
    }

    @Test
    public void testPingIdentityAuthNoCredentials(){

        int responseStatus = 0;

        try {
            // intentionally do not add access_token
            resource().path(BASE_PATH + "/auth/pingident")
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(JsonNode.class);
        }
        catch (UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getStatus();
        }
        assertEquals(UNAUTHORIZED_STATUS, responseStatus);
    }

    @Test
    public void testFoursquareAuthNoCredentials(){

        int responseStatus = 0;

        try {
            // intentionally do not add access_token
            resource().path(BASE_PATH + "/auth/foursquare")
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(JsonNode.class);
        }
        catch (UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getStatus();
        }
        assertEquals(UNAUTHORIZED_STATUS, responseStatus);
    }

    @Test
    public void testQueuesNoCredentials(){

        int responseStatus = 0;

        try {
            // intentionally do not add access_token
            resource().path(BASE_PATH + "/queues")
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(JsonNode.class);
        }
        catch (UniformInterfaceException uie ) {
            responseStatus = uie.getResponse().getStatus();
        }
        assertEquals(UNAUTHORIZED_STATUS, responseStatus);
    }

}
