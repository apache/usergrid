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

import org.apache.usergrid.java.client.*;
import org.apache.usergrid.java.client.auth.UsergridAppAuth;
import org.apache.usergrid.java.client.query.UsergridQuery;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class ClientAuthFallBackTestCase {

    private static UsergridQuery usersQuery = new UsergridQuery("users").desc("created");

    @Before
    public void before() {
        Usergrid.initSharedInstance(SDKTestConfiguration.ORG_NAME, SDKTestConfiguration.APP_NAME, SDKTestConfiguration.USERGRID_URL, SDKTestConfiguration.authFallBack);
        Usergrid.authenticateApp(new UsergridAppAuth(SDKTestConfiguration.APP_CLIENT_ID, SDKTestConfiguration.APP_CLIENT_SECRET));

        String[] segments = {"roles","guest","permissions"};
        Map<String, Object> params = new HashMap<>();
        params.put("permission","get,post,put,delete:/**");
        UsergridRequest request = new UsergridRequest(UsergridEnums.UsergridHttpMethod.DELETE, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, Usergrid.clientAppUrl(), params, null, Usergrid.authForRequests(), segments);
        Usergrid.sendRequest(request);
    }

    @After
    public void after() {
        Usergrid.setAuthMode(UsergridEnums.UsergridAuthMode.APP);
        String[] segments = {"roles","guest","permissions"};
        Map<String, Object> params = new HashMap<>();
        params.put("permission","get,post,put,delete:/**");
        UsergridRequest request = new UsergridRequest(UsergridEnums.UsergridHttpMethod.POST, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, Usergrid.clientAppUrl(), params, null, Usergrid.authForRequests(), segments);
        Usergrid.sendRequest(request);
        Usergrid.reset();
    }

    @Test
    public void authFallBackNONETest() {
        Usergrid.setAuthMode(UsergridEnums.UsergridAuthMode.NONE);
        UsergridResponse resp = Usergrid.GET(usersQuery);
        assertTrue("The returned response should have error", resp.getResponseError() != null);
    }

    @Test
    public void authFallBackAPPTest() {
        Usergrid.setAuthMode(UsergridEnums.UsergridAuthMode.APP);
        UsergridResponse resp = Usergrid.GET(usersQuery);
        assertTrue("The returned response should not have error", resp.getResponseError() == null);
    }
}
