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
import org.apache.usergrid.java.client.UsergridEnums.*;
import org.apache.usergrid.java.client.auth.UsergridAppAuth;
import org.apache.usergrid.java.client.auth.UsergridUserAuth;
import org.apache.usergrid.java.client.model.UsergridUser;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClientAuthTestCase {

    @Before
    public void before() {
        Usergrid.initSharedInstance(SDKTestConfiguration.ORG_NAME, SDKTestConfiguration.APP_NAME, SDKTestConfiguration.USERGRID_URL);
    }

    @After
    public void after() {
        Usergrid.reset();
    }

    @Test
    public void clientAuth_APP() {
        Usergrid.setAuthMode(UsergridAuthMode.APP);
        UsergridAppAuth appAuth = new UsergridAppAuth(SDKTestConfiguration.APP_CLIENT_ID, SDKTestConfiguration.APP_CLIENT_SECRET);
        UsergridResponse response = Usergrid.authenticateApp(appAuth);
        assertTrue("response status is OK", response.ok());
        assertNull("no error thrown", response.getResponseError());
        assertTrue("appAuth.isValidToken should be true", appAuth.isValidToken());
        assertNotNull("should have a valid token", appAuth.getAccessToken());
        assertNotNull("should have an expiry", appAuth.getExpiry());
        assertEquals("client.appAuth.token should be set to the token returned from Usergrid", Usergrid.getAppAuth(), appAuth);
        assertTrue("should have a token that is not empty", appAuth.getAccessToken().length() > 0);
        assertTrue("client.appAuth.expiry should be set to a future date", appAuth.getExpiry() > System.currentTimeMillis());
    }

    @Test
    public void clientAuth_USER() {
        Usergrid.setAuthMode(UsergridAuthMode.USER);
        UsergridUserAuth userAuth = new UsergridUserAuth(SDKTestConfiguration.APP_UserName, SDKTestConfiguration.APP_Password);
        UsergridResponse response = Usergrid.authenticateUser(userAuth);
        assertTrue("response status is OK", response.ok());
        assertNull("no error thrown", response.getResponseError());
        assertTrue("appAuth.isValidToken should be true", userAuth.isValidToken());
        assertNotNull("should have a token", userAuth.getAccessToken());
        assertNotNull("should have an expiry", userAuth.getExpiry());

        UsergridUser currentUser = Usergrid.getCurrentUser();
        assertNotNull("client.currentUser should not be null", currentUser);
        assertNotNull("client.currentUser().getUserAuth() should not be null", currentUser.getUserAuth());
        assertEquals("client.currentUser().userAuth should be the same as userAuth", currentUser.getUserAuth(), userAuth);
        assertTrue("should have a token that is not empty", userAuth.getAccessToken().length() > 0);
        assertTrue("client.currentUser().userAuth.getExpiry() should be set to a future date", userAuth.getExpiry() > System.currentTimeMillis());
        assertEquals("client.authForRequests() should be the same as userAuth", Usergrid.authForRequests(), userAuth);
    }

    @Test
    public void clientAuth_NONE() {
        Usergrid.setAuthMode(UsergridAuthMode.NONE);
        UsergridUserAuth userAuth = new UsergridUserAuth(SDKTestConfiguration.APP_UserName, SDKTestConfiguration.APP_Password);
        Usergrid.authenticateUser(userAuth);
        assertNull("no auth should be returned from client.authForRequests", Usergrid.authForRequests());
    }
}
