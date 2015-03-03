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
package org.apache.usergrid.rest.applications.collection;


import java.io.IOException;

import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.junit.Test;

import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.CustomCollection;

import org.apache.usergrid.rest.test.resource2point0.model.Collection;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;


/**
 * Simple tests to test querying at the REST tier
 */

public class BrowserCompatibilityTest extends org.apache.usergrid.rest.test.resource2point0.AbstractRestIT {


    /**
     * Test to check chrome type accept headers
     */
    @Test
    public void testChromeHtmlTypes() throws Exception {
        testBrowserAccept( "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8" );
    }


    /**
     * Test to check firefox type accept headers
     */
    @Test
    public void testFireFoxHtmlTypes() throws Exception {
        testBrowserAccept( "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8" );
    }

    /**
     * Test to check safari type accept headers
     */
    @Test
    public void testSafariTypes() throws Exception {
        testBrowserAccept( "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8" );
    }

    /**
     * Helper method to run browser accept header tests
     */
    private void testBrowserAccept( String acceptHeader ) throws IOException {

        //make anew entity and verify that it is accurate
        String name = "thing1";
        Entity payload = new Entity();
        payload.put("name", name);
        Entity entity = this.app().collection("things").post(payload);
        assertEquals(entity.get("name"), name);
        String uuid = entity.getAsString("uuid");
        this.refreshIndex();

        //now get this new entity with "text/html" in the accept header
        Entity returnedEntity = this.app().collection("things").withAcceptHeader(acceptHeader).entity(entity).get();
        String returnedUUID = returnedEntity.getAsString("uuid");

        //and make sure we got the same entity back
        assertEquals( uuid, returnedUUID );
    }
}
