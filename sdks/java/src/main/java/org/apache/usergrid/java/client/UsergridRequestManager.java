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
package org.apache.usergrid.java.client;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.usergrid.java.client.UsergridEnums.UsergridHttpMethod;
import org.apache.usergrid.java.client.auth.UsergridAppAuth;
import org.apache.usergrid.java.client.auth.UsergridAuth;
import org.apache.usergrid.java.client.model.UsergridUser;
import org.apache.usergrid.java.client.auth.UsergridUserAuth;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

import static org.apache.usergrid.java.client.utils.ObjectUtils.isEmpty;

public class UsergridRequestManager {

    @NotNull public static String USERGRID_USER_AGENT = "usergrid-java/v" + Usergrid.UsergridSDKVersion;

    @NotNull private final UsergridClient usergridClient;
    @NotNull private final OkHttpClient httpClient;

    public UsergridRequestManager(@NotNull final UsergridClient usergridClient) {
        this.usergridClient = usergridClient;
        this.httpClient = new OkHttpClient();
    }

    @NotNull
    public UsergridResponse performRequest(@NotNull final UsergridRequest usergridRequest) {
        Request request = usergridRequest.buildRequest();
        UsergridResponse usergridResponse;
        try {
            Response response = this.httpClient.newCall(request).execute();
            usergridResponse = UsergridResponse.fromResponse(this.usergridClient,usergridRequest,response);
        } catch( IOException exception ) {
            usergridResponse = UsergridResponse.fromException(this.usergridClient,exception);
        }
        return usergridResponse;
    }

    @NotNull
    private UsergridResponse authenticate(@NotNull final UsergridAuth auth) {
        Map<String, String> credentials = auth.credentialsMap();
        String url = this.usergridClient.clientAppUrl();
        if ( auth instanceof UsergridUserAuth){

            UsergridUserAuth userAuth = (UsergridUserAuth) auth;
            if( userAuth.isAdminUser()){

                url = this.usergridClient.managementUrl();
            }

        }

        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.POST, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, url, null, credentials, this.usergridClient.authForRequests(), "token");
        UsergridResponse response = performRequest(request);
        if (!isEmpty(response.getAccessToken()) && !isEmpty(response.getExpires())) {
            auth.setAccessToken(response.getAccessToken());
            auth.setExpiry(System.currentTimeMillis() + response.getExpires() - 5000);
        }
        return response;
    }

    @NotNull
    public UsergridResponse authenticateApp(@NotNull final UsergridAppAuth appAuth) {
        return this.authenticate(appAuth);
    }

    @NotNull
    public UsergridResponse authenticateUser(@NotNull final UsergridUserAuth userAuth) {
        UsergridResponse response = this.authenticate(userAuth);
        UsergridUser responseUser = response.user();
        if ( response.ok() && responseUser != null) {
            responseUser.setUserAuth(userAuth);
        }
        return response;
    }
}
