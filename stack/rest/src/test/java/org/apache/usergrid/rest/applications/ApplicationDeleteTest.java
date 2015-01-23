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


import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;


public class ApplicationDeleteTest  extends AbstractRestIT {


    @Test
    public void testBasicOperation() throws Exception {

        // create a collection with two entities
        String name1 = "thing1";
        String name2 = "thing2";
        String property = "one fish, two fish, red fish, blue fish";
        Entity payload = new Entity();
        payload.put("name", name1);
        payload.put("property", property);
        Entity entity1 = this.app().collection("things").post(payload);
        payload.put("name", name2);
        Entity entity2 = this.app().collection("things").post(payload);

        assertEquals(entity1.get("name"), name1);
        assertEquals(entity2.get("name"), name2);

        this.refreshIndex();

        // test that we can query those entities
        Collection collection = this.app().collection("things").get();
        assertEquals(2, collection.getNumOfEntities());

        //test that we can get the application entity
        ApiResponse appResponse = this.app().get();
        String retAppName = String.valueOf(appResponse.getProperties().get("applicationName")).toLowerCase();
        assertEquals(clientSetup.getAppName().toLowerCase() , retAppName);

        // delete the application
        try {
            this.app().delete();
        } catch ( UniformInterfaceException e ) {
            fail("Delete call threw exception status = " + e.getResponse().getStatus());
        }

        //try to get the application entity
        try {
            this.app().get();
            fail("should not be able to get app after it has been deleted");
        } catch (UniformInterfaceException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().getEntity( String.class ));
            assertEquals( "organization_application_not_found", node.get( "error" ).textValue() );
        }

        // test that we cannot delete the application a second time
        try {
            this.app().delete();
            fail("should not be able to delete app after it has been deleted");
        } catch (UniformInterfaceException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().getEntity( String.class ));
            assertEquals( "organization_application_not_found", node.get( "error" ).textValue() );
        }

        // test that we can no longer query for the entities in the collection
        try {
            this.app().collection("things").get();
            fail("should not be able to query for entities after app has been deleted");
        } catch (UniformInterfaceException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().getEntity( String.class ));
            assertEquals( "organization_application_not_found", node.get( "error" ).textValue() );
        }
    }
}
