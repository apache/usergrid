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
package org.apache.usergrid.client;

import org.apache.usergrid.java.client.Usergrid;
import org.apache.usergrid.java.client.auth.UsergridAppAuth;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class UsergridResponseErrorTestCase {

    @Before
    public void before() {
        Usergrid.initSharedInstance(SDKTestConfiguration.ORG_NAME, SDKTestConfiguration.APP_NAME, SDKTestConfiguration.USERGRID_URL, SDKTestConfiguration.authFallBack);
        UsergridAppAuth appAuth = new UsergridAppAuth(SDKTestConfiguration.APP_CLIENT_ID, SDKTestConfiguration.APP_CLIENT_SECRET);
        Usergrid.authenticateApp(appAuth);
    }

    @After
    public void after() {
        Usergrid.reset();
    }

    @Test
    public void testEntityCreationSuccess() {
        String collectionName = "ect" + System.currentTimeMillis();

        Map<String, String> fields = new HashMap<>(3);
        fields.put("color", "red");
        fields.put("shape", "square");

        SDKTestUtils.createEntity(collectionName, "testEntity1", fields);
        UsergridResponse eLookUp = Usergrid.GET(collectionName, "testEntity1");
        assertNull("The returned entity is null!", eLookUp.getResponseError());

        UsergridResponse response = Usergrid.GET(collectionName, "testEntityThatShouldNotExist");
        assertFalse("Response returned should not be ok.",response.ok());
        assertTrue("StatusCode equals than 404", response.getStatusCode() == 404);
        assertNotNull("The returned entity is null!", response.getResponseError());
    }
}
