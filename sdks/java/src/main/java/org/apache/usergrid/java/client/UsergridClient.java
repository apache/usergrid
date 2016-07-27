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

import org.apache.usergrid.java.client.UsergridEnums.*;
import org.apache.usergrid.java.client.auth.UsergridAppAuth;
import org.apache.usergrid.java.client.auth.UsergridAuth;
import org.apache.usergrid.java.client.auth.UsergridUserAuth;
import org.apache.usergrid.java.client.model.*;
import org.apache.usergrid.java.client.query.UsergridQuery;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class UsergridClient {

    @NotNull public static String DEFAULT_BASE_URL = "https://api.usergrid.com";

    @NotNull private UsergridClientConfig config;
    @Nullable private UsergridUser currentUser = null;
    @Nullable private UsergridAuth tempAuth = null;

    @NotNull private final UsergridRequestManager requestManager;

    public UsergridClient(@NotNull final UsergridClientConfig config) {
        this.config = config;
        this.requestManager = new UsergridRequestManager(this);
    }

    public UsergridClient(@NotNull final String orgId, @NotNull final String appId) {
        this(new UsergridClientConfig(orgId, appId));
    }

    public UsergridClient(@NotNull final String orgId, @NotNull final String appId, @NotNull final String baseUrl) {
        this(new UsergridClientConfig(orgId, appId, baseUrl));
    }

    public UsergridClient(@NotNull final String orgId, @NotNull final String appId, @NotNull final String baseUrl, @NotNull final UsergridAuthMode authMode) {
        this(new UsergridClientConfig(orgId, appId, baseUrl, authMode));
    }

    @NotNull public UsergridClientConfig getConfig() { return this.config; }
    public void setConfig(@NotNull final UsergridClientConfig config) { this.config = config; }

    @NotNull public String getAppId() { return this.config.appId; }
    public void setAppId(@NotNull final String appId) { this.config.appId = appId; }

    @NotNull public String getOrgId() { return this.config.orgId; }
    public void setOrgId(@NotNull final String orgId) { this.config.orgId = orgId; }

    @NotNull public String getBaseUrl() { return this.config.baseUrl; }
    public void setBaseUrl(@NotNull final String baseUrl) { this.config.baseUrl = baseUrl; }

    @NotNull public String clientAppUrl() { return getBaseUrl() + "/" + getOrgId() + "/" + getAppId(); }

    @NotNull public String managementUrl() { return getBaseUrl() + "/management"; }

    @NotNull public UsergridAuthMode getAuthMode() { return this.config.authMode; }
    public void setAuthMode(@NotNull final UsergridAuthMode authMode) { this.config.authMode = authMode; }

    @Nullable public UsergridUser getCurrentUser() { return this.currentUser; }
    public void setCurrentUser(@Nullable final UsergridUser currentUser) { this.currentUser = currentUser; }

    @Nullable public UsergridUserAuth getUserAuth() { return (this.currentUser != null) ? this.currentUser.getUserAuth() : null; }

    @Nullable public UsergridAppAuth getAppAuth() { return this.config.appAuth; }
    public void setAppAuth(@Nullable final UsergridAppAuth appAuth) { this.config.appAuth = appAuth; }

    @Nullable
    public UsergridAuth authForRequests() {
        UsergridAuth authForRequests = null;
        if (tempAuth != null) {
            if (tempAuth.isValidToken()) {
                authForRequests = tempAuth;
            }
            tempAuth = null;
        } else {
            switch (config.authMode) {
                case USER: {
                    if (this.currentUser != null && this.currentUser.getUserAuth() != null && this.currentUser.getUserAuth().isValidToken()) {
                        authForRequests = this.currentUser.getUserAuth();
                    }
                    break;
                }
                case APP: {
                    if (this.config.appAuth != null && this.config.appAuth.isValidToken()) {
                        authForRequests = this.config.appAuth;
                    }
                    break;
                }
            }
        }
        return authForRequests;
    }

    @NotNull
    public UsergridClient usingAuth(@Nullable final UsergridAuth auth) {
        this.tempAuth = auth;
        return this;
    }

    @NotNull
    public UsergridClient usingToken(@NotNull final String accessToken) {
        this.tempAuth = new UsergridAuth(accessToken);
        return this;
    }

    @NotNull
    public UsergridResponse authenticateApp() {
        if( this.config.appAuth == null ) {
            return UsergridResponse.fromError(this,  "Invalid UsergridAppAuth.", "UsergridClient's appAuth is null.");
        }
        return this.authenticateApp(this.config.appAuth);
    }

    @NotNull
    public UsergridResponse authenticateApp(@NotNull final UsergridAppAuth auth) {
        this.config.appAuth = auth;
        return this.requestManager.authenticateApp(auth);
    }

    @NotNull
    public UsergridResponse authenticateUser(@NotNull final UsergridUserAuth userAuth) {
        return this.authenticateUser(userAuth,true);
    }

    @NotNull
    public UsergridResponse authenticateUser(@NotNull final UsergridUserAuth userAuth, final boolean setAsCurrentUser) {
        UsergridResponse response = this.requestManager.authenticateUser(userAuth);
        if( response.ok() && setAsCurrentUser ) {
            this.setCurrentUser(response.user());
        }
        return response;
    }

    @NotNull
    public UsergridResponse resetPassword(@NotNull final UsergridUser user, @NotNull final String oldPassword, @NotNull final String newPassword) {
        String usernameOrEmail = user.usernameOrEmail();
        if( usernameOrEmail == null ) {
            return UsergridResponse.fromError(this,  "Error resetting password.", "The UsergridUser object must contain a valid username or email to reset the password.");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("newpassword", newPassword);
        data.put("oldpassword", oldPassword);
        String[] pathSegments = { "users", usernameOrEmail, "password"};
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.POST, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), null, data, this.authForRequests() ,pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse logoutCurrentUser()  {
        UsergridUser currentUser = this.currentUser;
        if( currentUser != null ) {
            String uuidOrUsername = currentUser.uuidOrUsername();
            UsergridUserAuth userAuth = currentUser.getUserAuth();
            if( uuidOrUsername != null && userAuth != null ) {
                String accessToken = userAuth.getAccessToken();
                if( accessToken != null ) {
                    return logoutUser(uuidOrUsername, accessToken);
                }
            }
        }
        return UsergridResponse.fromError(this,"UsergridClient's currentUser is not valid.", "UsergridClient's currentUser is null or has no uuid or username.");
    }

    @NotNull
    public UsergridResponse logoutUserAllTokens(@NotNull final String uuidOrUsername) {
        return logoutUser(uuidOrUsername, null);
    }

    @NotNull
    public UsergridResponse logoutUser(@NotNull final String uuidOrUsername, @Nullable final String token){
        String[] pathSegments = {"users", uuidOrUsername, ""};
        int len = pathSegments.length;
        Map<String, Object> param = new HashMap<>();
        if(token != null){
            pathSegments[len-1] = "revoketoken";
            param.put("token",token);
        }
        else{
            pathSegments[len-1] = "revoketokens";
        }
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.PUT, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), param, null, this.authForRequests() , pathSegments);
        UsergridResponse response = this.sendRequest(request);
        UsergridUser currentUser = this.getCurrentUser();
        if( currentUser != null && response.ok() ) {
            if( uuidOrUsername.equalsIgnoreCase(currentUser.uuidOrUsername()) ) {
                this.setCurrentUser(null);
            }
        }
        return response;
    }

    @NotNull
    public UsergridResponse sendRequest(@NotNull final UsergridRequest request) {
        return this.requestManager.performRequest(request);
    }

    @NotNull
    public UsergridResponse GET(@NotNull final String type, @NotNull final String uuidOrName) {
        String[] pathSegments = {type, uuidOrName};
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.GET, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse GET(@NotNull final String type) {
        String[] pathSegments = {type};
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.GET, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse GET(@NotNull final UsergridQuery query) {
        String collectionName = query.getCollection();
        if( collectionName == null ) {
            return UsergridResponse.fromError(this,  "Query collection name missing.", "Query collection name is missing.");
        }
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.GET, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), query, this.authForRequests() , collectionName);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse PUT(@NotNull final String type, @NotNull final String uuidOrName, @NotNull final Map<String, ?> jsonBody) {
        String[] pathSegments = { type, uuidOrName };
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.PUT, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), null, jsonBody, this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse PUT(@NotNull final String type, @NotNull final Map<String, ?> jsonBody) {
        String uuidOrName = null;
        Object uuid = jsonBody.get(UsergridEntityProperties.UUID.toString());
        if( uuid != null ) {
            uuidOrName = uuid.toString();
        } else {
            Object name = jsonBody.get(UsergridEntityProperties.NAME.toString());
            if( name != null ) {
                uuidOrName = name.toString();
            }
        }
        if( uuidOrName == null ) {
            return UsergridResponse.fromError(this,  "jsonBody not valid..", "The `jsonBody` must contain a valid value for either `uuid` or `name`.");
        }
        String[] pathSegments = { type, uuidOrName };
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.PUT, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), null, jsonBody, this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse PUT(@NotNull final UsergridEntity entity) {
        String entityUuidOrName = entity.uuidOrName();
        if( entityUuidOrName == null ) {
            return UsergridResponse.fromError(this,  "No UUID or name found.", "The entity object must have a `uuid` or `name` assigned.");
        }
        String[] pathSegments = { entity.getType(), entityUuidOrName };
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.PUT, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), null, entity, this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse PUT(@NotNull final UsergridQuery query, @NotNull final Map<String, ?> jsonBody) {
        String collectionName = query.getCollection();
        if( collectionName == null ) {
            return UsergridResponse.fromError(this,  "Query collection name missing.", "Query collection name is missing.");
        }
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.PUT, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), null, jsonBody, null, query, this.authForRequests(),collectionName);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse POST(final @NotNull UsergridEntity entity) {
        String[] pathSegments = {entity.getType()};
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.POST, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), null, entity, this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse POST(@NotNull final List<UsergridEntity> entities) {
        if( entities.isEmpty() ) {
            return UsergridResponse.fromError(this,  "Unable to POST entities.", "entities array is empty.");
        }
        String[] pathSegments = {entities.get(0).getType()};
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.POST, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), null, entities, this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse POST(@NotNull final String type, @NotNull final String uuidOrName, @NotNull final Map<String, ?> jsonBody) {
        String[] pathSegments = {type, uuidOrName};
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.POST, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), null, jsonBody, this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse POST(@NotNull final String type, @NotNull final Map<String, ?> jsonBody) {
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.POST, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), null, jsonBody, this.authForRequests() , type);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse POST(@NotNull final String type, @NotNull final List<Map<String, ?>> jsonBodies) {
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.POST, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), null, jsonBodies, this.authForRequests() , type);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse DELETE(@NotNull final UsergridEntity entity) {
        String entityUuidOrName = entity.uuidOrName();
        if( entityUuidOrName == null ) {
            return UsergridResponse.fromError(this,  "No UUID or name found.", "The entity object must have a `uuid` or `name` assigned.");
        }
        String[] pathSegments = {entity.getType(), entityUuidOrName};
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.DELETE, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse DELETE(@NotNull final String type, @NotNull final String uuidOrName) {
        String[] pathSegments = {type, uuidOrName};
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.DELETE, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse DELETE(@NotNull final UsergridQuery query) {
        String collectionName = query.getCollection();
        if( collectionName == null ) {
            return UsergridResponse.fromError(this,  "Query collection name missing.", "Query collection name is missing.");
        }
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.DELETE, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), query, this.authForRequests() , collectionName);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse connect(@NotNull final UsergridEntity entity, @NotNull final String relationship, @NotNull final UsergridEntity to) {
        String entityUuidOrName = entity.uuidOrName();
        String toUuidOrName = to.uuidOrName();
        if( entityUuidOrName == null || toUuidOrName == null ) {
            return UsergridResponse.fromError(this, "Invalid Entity Connection Attempt.", "One or both entities that are attempting to be connected do not contain a valid UUID or Name property.");
        }
        return this.connect(entity.getType(), entityUuidOrName, relationship, to.getType(), toUuidOrName);
    }

    @NotNull
    public UsergridResponse connect(@NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String toType, @NotNull final String toName) {
        String[] pathSegments = {entityType, entityId, relationship, toType, toName};
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.POST, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse connect(@NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String toId) {
        String[] pathSegments = { entityType, entityId, relationship, toId};
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.POST, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse disconnect(@NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String fromUuid) {
        String[] pathSegments = {entityType, entityId, relationship, fromUuid};
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.DELETE, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse disconnect(@NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String fromType, @NotNull final String fromName) {
        String[] pathSegments = {entityType, entityId, relationship, fromType, fromName};
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.DELETE, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse disconnect(@NotNull final UsergridEntity entity, @NotNull final String relationship, @NotNull final UsergridEntity from) {
        String entityUuidOrName = entity.uuidOrName();
        String fromUuidOrName = from.uuidOrName();
        if( entityUuidOrName == null || fromUuidOrName == null ) {
            return UsergridResponse.fromError(this, "Invalid Entity Disconnect Attempt.", "One or both entities that are attempting to be disconnected do not contain a valid UUID or Name property.");
        }
        return this.disconnect(entity.getType(), entityUuidOrName, relationship, from.getType(), fromUuidOrName);
    }

    @NotNull
    public UsergridResponse getConnections(@NotNull final UsergridDirection direction, @NotNull final UsergridEntity entity, @NotNull final String relationship) {
        return this.getConnections(direction,entity,relationship,null);
    }

    @NotNull
    public UsergridResponse getConnections(@NotNull final UsergridDirection direction, @NotNull final UsergridEntity entity, @NotNull final String relationship, @Nullable final UsergridQuery query) {
        String entityUuidOrName = entity.uuidOrName();
        if( entityUuidOrName == null ) {
            return UsergridResponse.fromError(this, "Invalid Entity Get Connections Attempt.", "The entity must have a `uuid` or `name` assigned.");
        }
        return this.getConnections(direction,entity.getType(),entityUuidOrName,relationship,query);
    }

    @NotNull
    public UsergridResponse getConnections(@NotNull final UsergridDirection direction, @NotNull final String type, @NotNull final String uuidOrName, @NotNull final String relationship, @Nullable final UsergridQuery query) {
        String[] pathSegments = {type, uuidOrName, direction.connectionValue(), relationship};
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.GET, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), query, this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }

    @NotNull
    public UsergridResponse getConnections(@NotNull final UsergridDirection direction, @NotNull final String uuid, @NotNull final String relationship, @Nullable final UsergridQuery query) {
        String[] pathSegments = {uuid, direction.connectionValue(), relationship};
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.GET, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, this.clientAppUrl(), query, this.authForRequests() , pathSegments);
        return this.sendRequest(request);
    }
}
