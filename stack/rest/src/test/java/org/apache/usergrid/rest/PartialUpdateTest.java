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


import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.utils.MapUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ClientErrorException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * Partial update test.
 */
public class PartialUpdateTest extends AbstractRestIT {
    private static final Logger log = LoggerFactory.getLogger(PartialUpdateTest.class);

    double latitude = 37.772837;
    double longitude = -122.409895;

    Map<String, Double> geolocation = new MapUtils.HashMapBuilder<String, Double>()
        .map("latitude", latitude)
        .map("longitude", longitude);

    @Test
    public void testPartialUpdate() throws IOException {

        // create user bart
        Entity props = new Entity();
        props.put("username", "bart");
        props.put("employer", "Brawndo");
        props.put("email", "bart@personal-email.example.com");
        props.put("location", geolocation);
        // POST the entity
        Entity userNode = this.app().collection("users").post(props);
        // make sure it was saved properly
        assertNotNull(userNode);
        String uuid = userNode.get("uuid").toString();
        assertNotNull(uuid);

        refreshIndex();

        Map<String, Object> updateProperties = new LinkedHashMap<String, Object>();
        // update user bart passing only an update to a property
        for (int i = 1; i < 10; i++) {
            // "Move" the user by incrementing their location
            Entity updateProps = new Entity();
            geolocation.put("latitude", latitude += 0.00001);
            geolocation.put("longitude", longitude += 0.00001);
            //update the User's employer property
            updateProps.put("employer", "Initech");
            updateProps.put("location", geolocation);

            try {
                // PUT the updates to the user and ensure they were saved
                userNode = this.app().collection("users").entity(userNode).put(updateProps);
            } catch (ClientErrorException uie) {
                fail("Update failed due to: " + uie.getResponse().readEntity(String.class));
            }

            refreshIndex();
            // retrieve the user from the backend
            userNode = this.app().collection("users").entity(userNode).get();

            log.info(userNode.toString());

            // verify that the user was returned
            assertNotNull(userNode);
            // Verify that the user's employer was updated
            assertEquals("Initech", userNode.get("employer").toString());
            // verify that the geo data is present
            assertNotNull(userNode.get("location"));
            assertNotNull(((Map<String, Object>) userNode.get("location")).get("latitude"));
            assertNotNull(((Map<String, Object>) userNode.get("location")).get("longitude"));

            // Verify that the location was updated correctly AND that
            // it is not the same object reference from the original POST
            log.info(geolocation.get("latitude") + " != "
                + Double.parseDouble(((Map<String, Object>) userNode.get("location")).get("latitude").toString()));
            log.info(geolocation.get("longitude") + " != "
                + Double.parseDouble(((Map<String, Object>) userNode.get("location")).get("longitude").toString()));
            assertNotSame(geolocation.get("latitude"),
                Double.parseDouble(((Map<String, Object>) userNode.get("location")).get("latitude").toString()));
            assertEquals(geolocation.get("latitude").doubleValue(),
                Double.parseDouble(((Map<String, Object>) userNode.get("location")).get("latitude").toString()), 0);
            assertNotSame(geolocation.get("longitude"),
                Double.parseDouble(((Map<String, Object>) userNode.get("location")).get("longitude").toString()));
            assertEquals(geolocation.get("longitude").doubleValue(),
                Double.parseDouble(((Map<String, Object>) userNode.get("location")).get("longitude").toString()), 0);
        }

        // Update bart's employer without specifying the full entity
        // (this time with username specified in URL)
        Entity updateProps = new Entity();
        updateProps.put("employer", "ACME Corporation");

        try { //  PUT /users/fred   put /users/uuid
            userNode = this.app().collection("users").entity(props.get("username").toString()).put(updateProps);

        } catch (ClientErrorException uie) {
            fail("Update failed due to: " + uie.getResponse().readEntity(String.class));
        }
        refreshIndex();

        userNode = this.app().collection("users").entity(userNode).get();
        assertNotNull(userNode);
        //Test that the updated property is returned
        assertEquals(updateProps.get("employer"), userNode.get("employer").toString());
        //Test that the original properties are still there
        assertEquals(props.get("username").toString(), userNode.get("username").toString());
    }
}
