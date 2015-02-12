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


import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.CustomCollection;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.apache.usergrid.utils.MapUtils.hashMap;


/**
 * Simple tests to test querying at the REST tier
 */

public class BrowserCompatibilityTest extends AbstractRestIT {


    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test
    public void testChromeHtmlTypes() throws Exception {
        testBrowserAccept( "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8" );
    }


    @Test
    public void testFireFoxHtmlTypes() throws Exception {
        testBrowserAccept( "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8" );
    }


    @Test
    public void testSafariTypes() throws Exception {
        testBrowserAccept( "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8" );
    }


    private void testBrowserAccept( String acceptHeader ) throws IOException {


        CustomCollection things = context.application().customCollection( "things" );

        Map<String, String> entity = hashMap( "name", "thing1" );
        JsonNode response = things.create( entity );

        refreshIndex(context.getOrgName(), context.getAppName());

        UUID entityId = getEntityId( response, 0 );

        assertNotNull( entityId );

        //now get it with "text/html" in the type

        //now try and retrieve it
        response = things.entity( entityId ).withAccept( acceptHeader ).get();

        UUID returnedEntityId = getEntityId( response, 0 );


        assertEquals( entityId, returnedEntityId );
    }
}
