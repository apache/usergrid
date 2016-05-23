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

import org.apache.usergrid.java.client.UsergridEnums.UsergridAuthMode;
import org.apache.usergrid.java.client.auth.UsergridAppAuth;
import org.apache.usergrid.java.client.auth.UsergridAuth;
import org.apache.usergrid.java.client.auth.UsergridUserAuth;
import org.apache.usergrid.java.client.model.*;
import org.apache.usergrid.java.client.query.UsergridQuery;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.apache.usergrid.java.client.UsergridEnums.UsergridDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public final class Usergrid {
    @NotNull public static final String UsergridSDKVersion = "2.1.0";

    private static UsergridClient sharedClient;
    private Usergrid() { /** Private constructor because we only have static methods. **/ }

    public static boolean isInitialized() {
        return (Usergrid.sharedClient != null);
    }
    public static void reset() { Usergrid.sharedClient = null; }

    @NotNull
    public static UsergridClient getInstance() throws NullPointerException {
        if (!Usergrid.isInitialized()) {
            throw new NullPointerException("Shared client has not been initialized!");
        }
        return Usergrid.sharedClient;
    }

    @NotNull
    public static UsergridClient initSharedInstance(@NotNull final UsergridClientConfig config) {
        if (Usergrid.isInitialized()) {
            System.out.print("The Usergrid shared instance was already initialized. All subsequent initialization attempts (including this) will be ignored.");
        } else {
            Usergrid.sharedClient = new UsergridClient(config);
        }
        return Usergrid.sharedClient;
    }

    @NotNull
    public static UsergridClient initSharedInstance(@NotNull final String orgId, @NotNull final String appId) {
        return Usergrid.initSharedInstance(new UsergridClientConfig(orgId, appId));
    }

    @NotNull
    public static UsergridClient initSharedInstance(@NotNull final String orgId, @NotNull final String appId, @NotNull final String baseUrl) {
        return Usergrid.initSharedInstance(new UsergridClientConfig(orgId, appId, baseUrl));
    }

    @NotNull
    public static UsergridClient initSharedInstance(@NotNull final String orgId, @NotNull final String appId, @NotNull final String baseUrl, @NotNull final UsergridAuthMode authMode) {
        return Usergrid.initSharedInstance(new UsergridClientConfig(orgId, appId, baseUrl, authMode));
    }

    @NotNull public static UsergridClientConfig getConfig() { return Usergrid.getInstance().getConfig(); }
    public static void setConfig(@NotNull UsergridClientConfig config) { Usergrid.getInstance().setConfig(config); }

    @NotNull public static String getAppId() { return Usergrid.getInstance().getAppId(); }
    public static void setAppId(@NotNull String appId) { Usergrid.getInstance().setAppId(appId); }

    @NotNull public static String getOrgId() { return Usergrid.getInstance().getOrgId(); }
    public static void setOrgId(@NotNull String orgId) { Usergrid.getInstance().setOrgId(orgId); }

    @NotNull public static String getBaseUrl() { return Usergrid.getInstance().getBaseUrl(); }
    public static void setBaseUrl(@NotNull String baseUrl) { Usergrid.getInstance().setBaseUrl(baseUrl); }

    @NotNull public static String clientAppUrl() { return Usergrid.getInstance().clientAppUrl(); }

    @NotNull public static UsergridAuthMode getAuthMode() { return Usergrid.getInstance().getAuthMode(); }
    public static void setAuthMode(@NotNull final UsergridAuthMode authMode) { Usergrid.getInstance().setAuthMode(authMode); }

    @Nullable public static UsergridAppAuth getAppAuth() { return Usergrid.getInstance().getAppAuth(); }
    public static void setAppAuth(@Nullable final UsergridAppAuth appAuth) { Usergrid.getInstance().setAppAuth(appAuth); }

    @Nullable public static UsergridUser getCurrentUser() { return Usergrid.getInstance().getCurrentUser(); }
    public static void setCurrentUser(@Nullable final UsergridUser currentUser) { Usergrid.getInstance().setCurrentUser(currentUser); }

    @Nullable
    public static UsergridAuth authForRequests() {
        return Usergrid.getInstance().authForRequests();
    }

    @NotNull
    public static UsergridClient usingAuth(@NotNull final UsergridAuth auth) {
        return Usergrid.getInstance().usingAuth(auth);
    }

    @NotNull
    public static UsergridClient usingToken(@NotNull final String accessToken) {
        return Usergrid.getInstance().usingToken(accessToken);
    }

    @NotNull
    public static UsergridResponse resetPassword(@NotNull final UsergridUser user, @NotNull final String oldPassword, @NotNull final String newPassword) {
        return Usergrid.getInstance().resetPassword(user, oldPassword, newPassword);
    }

    @NotNull
    public static UsergridResponse authenticateApp() {
        return Usergrid.getInstance().authenticateApp();
    }

    @NotNull
    public static UsergridResponse authenticateApp(@NotNull final UsergridAppAuth appAuth) {
        return Usergrid.getInstance().authenticateApp(appAuth);
    }

    @NotNull
    public static UsergridResponse authenticateUser(@NotNull final UsergridUserAuth userAuth) {
        return Usergrid.getInstance().authenticateUser(userAuth);
    }

    @NotNull
    public static UsergridResponse authenticateUser(@NotNull final UsergridUserAuth userAuth, final boolean setAsCurrentUser) {
        return Usergrid.getInstance().authenticateUser(userAuth,setAsCurrentUser);
    }

    @NotNull
    public static UsergridResponse logoutCurrentUser() {
        return Usergrid.getInstance().logoutCurrentUser();
    }

    @NotNull
    public static UsergridResponse logoutUserAllTokens(@NotNull final String uuidOrUsername) {
        return Usergrid.getInstance().logoutUserAllTokens(uuidOrUsername);
    }

    @NotNull
    public static UsergridResponse logoutUser(@NotNull final String uuidOrUsername, @Nullable final String token) {
        return Usergrid.getInstance().logoutUser(uuidOrUsername,token);
    }

    @NotNull
    public static UsergridResponse sendRequest(@NotNull final UsergridRequest request) {
        return Usergrid.getInstance().sendRequest(request);
    }

    @NotNull
    public static UsergridResponse GET(@NotNull final String type, @NotNull final String uuidOrName) {
        return Usergrid.getInstance().GET(type, uuidOrName);
    }

    @NotNull
    public static UsergridResponse GET(@NotNull final String type) {
        return Usergrid.getInstance().GET(type);
    }

    @NotNull
    public static UsergridResponse GET(@NotNull final UsergridQuery query) {
        return Usergrid.getInstance().GET(query);
    }

    @NotNull
    public static UsergridResponse PUT(@NotNull final String type, @NotNull final String uuidOrName, @NotNull final Map<String, ?> jsonBody) {
        return Usergrid.getInstance().PUT(type, uuidOrName, jsonBody);
    }

    @NotNull
    public static UsergridResponse PUT(@NotNull final String type, @NotNull final Map<String, ?> jsonBody) {
        return Usergrid.getInstance().PUT(type, jsonBody);
    }

    @NotNull
    public static UsergridResponse PUT(@NotNull final UsergridEntity entity) {
        return Usergrid.getInstance().PUT(entity);
    }

    @NotNull
    public static UsergridResponse PUT(@NotNull final UsergridQuery query, @NotNull final Map<String, ?> jsonBody) {
        return Usergrid.getInstance().PUT(query, jsonBody);
    }

    @NotNull
    public static UsergridResponse POST(@NotNull final String type, @NotNull final String uuidOrName, @NotNull final Map<String, Object> jsonBody) {
        return Usergrid.getInstance().POST(type, uuidOrName, jsonBody);
    }

    @NotNull
    public static UsergridResponse POST(@NotNull final String type, @NotNull final Map<String, ?> jsonBody) {
        return Usergrid.getInstance().POST(type, jsonBody);
    }

    @NotNull
    public static UsergridResponse POST(@NotNull final String type, @NotNull final List<Map<String, ?>> jsonBodies) {
        return Usergrid.getInstance().POST(type, jsonBodies);
    }

    @NotNull
    public static UsergridResponse POST(@NotNull final UsergridEntity entity) throws NullPointerException {
        return Usergrid.getInstance().POST(entity);
    }

    @NotNull
    public static UsergridResponse POST(@NotNull final List<UsergridEntity> entities) {
        return Usergrid.getInstance().POST(entities);
    }

    @NotNull
    public static UsergridResponse DELETE(@NotNull final String type, @NotNull final String uuidOrName) {
        return Usergrid.getInstance().DELETE(type, uuidOrName);
    }

    @NotNull
    public static UsergridResponse DELETE(@NotNull final UsergridEntity entity) {
        return Usergrid.getInstance().DELETE(entity);
    }

    @NotNull
    public static UsergridResponse DELETE(@NotNull final UsergridQuery query) {
        return Usergrid.getInstance().DELETE(query);
    }

    @NotNull
    public static UsergridResponse connect(@NotNull final UsergridEntity entity, @NotNull final String relationship, @NotNull final UsergridEntity to) {
        return Usergrid.getInstance().connect(entity, relationship, to);
    }

    @NotNull
    public static UsergridResponse connect(@NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String fromUuid) {
        return Usergrid.getInstance().connect(entityType,entityId,relationship,fromUuid);
    }

    @NotNull
    public static UsergridResponse connect(@NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String toType, @NotNull final String toName) {
        return Usergrid.getInstance().connect(entityType,entityId,relationship,toType,toName);
    }

    @NotNull
    public static UsergridResponse disconnect(@NotNull final UsergridEntity entity, @NotNull final String relationship, @NotNull final UsergridEntity from) {
        return Usergrid.getInstance().disconnect(entity, relationship, from);
    }

    @NotNull
    public static UsergridResponse disconnect(@NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String fromUuid) {
        return Usergrid.getInstance().disconnect(entityType, entityId, relationship, fromUuid);
    }

    @NotNull
    public static UsergridResponse disconnect(@NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String fromType, @NotNull final String fromName) {
        return Usergrid.getInstance().disconnect(entityType, entityId, relationship, fromType, fromName);
    }

    @NotNull
    public static UsergridResponse getConnections(@NotNull final UsergridDirection direction, @NotNull final UsergridEntity entity, @NotNull final String relationship) {
        return Usergrid.getInstance().getConnections(direction, entity, relationship);
    }

    @NotNull
    public static UsergridResponse getConnections(@NotNull final UsergridDirection direction, @NotNull final UsergridEntity entity, @NotNull final String relationship, @Nullable final UsergridQuery query) {
        return Usergrid.getInstance().getConnections(direction, entity, relationship, query);
    }

    @NotNull
    public static UsergridResponse getConnections(@NotNull final UsergridDirection direction, @NotNull final String type, @NotNull final String uuidOrName, @NotNull final String relationship, @Nullable final UsergridQuery query) {
        return Usergrid.getInstance().getConnections(direction,type,uuidOrName,relationship,query);
    }

    @NotNull
    public static UsergridResponse getConnections(@NotNull final UsergridDirection direction, @NotNull final String uuid, @NotNull final String relationship, @Nullable final UsergridQuery query) {
        return Usergrid.getInstance().getConnections(direction, uuid, relationship, query);
    }
}