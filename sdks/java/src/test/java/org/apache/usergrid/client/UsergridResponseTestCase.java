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
import org.apache.usergrid.java.client.UsergridEnums;
import org.apache.usergrid.java.client.auth.UsergridUserAuth;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UsergridResponseTestCase {

    @Before
    public void before() {
        Usergrid.initSharedInstance(SDKTestConfiguration.ORG_NAME, SDKTestConfiguration.APP_NAME, SDKTestConfiguration.USERGRID_URL, UsergridEnums.UsergridAuthMode.USER);
        Usergrid.authenticateUser(new UsergridUserAuth(SDKTestConfiguration.APP_UserName, SDKTestConfiguration.APP_Password));
    }

    @After
    public void after() {
        Usergrid.reset();
    }

    @Test
    public void testLogoutUser() {
        String collectionName = "ect" + System.currentTimeMillis();

        Map<String, String> fields = new HashMap<>(3);
        fields.put("color", "red");
        fields.put("shape", "square");

        SDKTestUtils.createEntity(collectionName, "testEntity1", fields);
        UsergridResponse response = Usergrid.GET(collectionName, "testEntity1");
        Object instanceObj = response.getStatusCode();
        assertTrue("The returned statusCode is and object of integer", instanceObj instanceof Integer);
        instanceObj = response.ok();
        assertTrue("The returned statusCode is and object of boolean", instanceObj instanceof Boolean);

        UsergridResponse resp = Usergrid.logoutUser(SDKTestConfiguration.APP_UserName,null);

        response = Usergrid.GET(collectionName, "testEntity1");
        assertNotNull("The response should throw an error",response.getResponseError());
    }

    @Test
    public void testLogoutCurrentUser() {
        String collectionName = "ect" + System.currentTimeMillis();

        Map<String, String> fields = new HashMap<>(3);
        fields.put("color", "red");
        fields.put("shape", "square");

        SDKTestUtils.createEntity(collectionName, "testEntity12", fields);
        UsergridResponse response = Usergrid.GET(collectionName, "testEntity12");
        assertNull("The response should not throw an error",response.getResponseError());

        Usergrid.logoutCurrentUser();
        response = Usergrid.GET(collectionName, "testEntity1");
        assertNotNull("The response should throw an error",response.getResponseError());
    }

}
