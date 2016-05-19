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
import org.apache.usergrid.java.client.UsergridClient;
import org.apache.usergrid.java.client.UsergridEnums;
import org.apache.usergrid.java.client.UsergridRequest;
import org.apache.usergrid.java.client.auth.UsergridAppAuth;
import org.apache.usergrid.java.client.model.UsergridEntity;
import org.apache.usergrid.java.client.model.UsergridUser;
import org.apache.usergrid.java.client.auth.UsergridUserAuth;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.junit.After;
import org.junit.Test;

import java.util.List;

public class UsergridClientAuthTestCase {

    @After
    public void after() {
        Usergrid.reset();
    }

    @Test
    public void clientAppInit() {
        Usergrid.initSharedInstance(SDKTestConfiguration.ORG_NAME, SDKTestConfiguration.APP_NAME, SDKTestConfiguration.USERGRID_URL, SDKTestConfiguration.authFallBack);
        Usergrid.authenticateApp(new UsergridAppAuth(SDKTestConfiguration.APP_CLIENT_ID, SDKTestConfiguration.APP_CLIENT_SECRET));

        //should fall back to using no authentication when currentUser is not authenticated and authFallback is set to NONE
        UsergridClient client = Usergrid.getInstance();
//        client.config.authMode = UsergridEnums.UsergridAuthMode.NONE;

        String[] segments = {client.getOrgId(), client.getAppId(), "users"};
        UsergridRequest request = new UsergridRequest(UsergridEnums.UsergridHttpMethod.GET, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE,
                client.getBaseUrl(), null, null, null, null, client.authForRequests(), segments);
        client.sendRequest(request);
    }

    @Test
    public void clientUserInit() {
        Usergrid.initSharedInstance(SDKTestConfiguration.ORG_NAME, SDKTestConfiguration.APP_NAME, SDKTestConfiguration.USERGRID_URL);
        Usergrid.authenticateUser(new UsergridUserAuth(SDKTestConfiguration.APP_UserName, SDKTestConfiguration.APP_Password));
        UsergridResponse getResponse = Usergrid.GET("user","eb8145ea-e171-11e5-a5e5-2bc0953f9fe6");
        if( getResponse.getEntities() != null ) {
            UsergridEntity entity = getResponse.first();
            if( entity instanceof UsergridUser) {
                UsergridUser user = (UsergridUser) entity;
                System.out.print(user.toString());
            }
            List<UsergridUser> users = getResponse.users();
            if( users != null ) {
                System.out.print(users.get(0).toString());
            }
        }

    }
}
