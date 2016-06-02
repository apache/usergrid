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
package org.apache.usergrid.java.client.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.usergrid.java.client.Usergrid;
import org.apache.usergrid.java.client.UsergridClient;
import org.apache.usergrid.java.client.UsergridEnums.*;
import org.apache.usergrid.java.client.auth.UsergridUserAuth;
import org.apache.usergrid.java.client.query.UsergridQuery;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsergridUser extends UsergridEntity {
    @NotNull public final static String USER_ENTITY_TYPE = "user";

    @Nullable private UsergridUserAuth userAuth = null;

    @Nullable private String username;
    @Nullable private String email;
    @Nullable private String password;
    @Nullable private String picture;

    private boolean activated = false;
    private boolean disabled = false;

    public UsergridUser() {
        super(USER_ENTITY_TYPE);
    }

    public UsergridUser(@NotNull final String username, @Nullable final String password) {
        super(USER_ENTITY_TYPE);
        setUsername(username);
        setPassword(password);
    }

    public UsergridUser(@NotNull final String name, @NotNull final HashMap<String, Object> propertyMap) {
        super(USER_ENTITY_TYPE,name);
        putProperties(propertyMap);
    }

    public UsergridUser(@Nullable final String name, @Nullable final String username, @Nullable final String email, @Nullable final String password) {
        super(USER_ENTITY_TYPE,name);
        setUsername(username);
        setEmail(email);
        setPassword(password);
    }

    public void setName(@Nullable final String name) { super.setName(name); }

    @Nullable public String getUsername() { return this.username; }
    public void setUsername(@Nullable final String username) { this.username = username; }

    @Nullable public String getEmail() { return this.email; }
    public void setEmail(@Nullable final String email) { this.email = email; }

    @Nullable public String getPassword() { return this.password; }
    public void setPassword(@Nullable final String password) { this.password = password; }

    @Nullable public String getPicture() { return this.picture; }
    public void setPicture(@Nullable final String picture) { this.picture = picture; }

    public boolean isActivated() { return this.activated; }
    public void setActivated(final boolean activated) { this.activated = activated; }

    public boolean isDisabled() { return this.disabled; }
    public void setDisabled(final boolean disabled) { this.disabled = disabled; }

    @JsonIgnore @Nullable public UsergridUserAuth getUserAuth() { return this.userAuth; }
    @JsonIgnore public void setUserAuth(@Nullable final UsergridUserAuth userAuth) { this.userAuth = userAuth; }

    @Nullable
    public String uuidOrUsername() {
        String uuidOrUsername = this.getUuid();
        if( uuidOrUsername == null ) {
            uuidOrUsername = this.getUsername();
        }
        return uuidOrUsername;
    }

    @Nullable
    public String usernameOrEmail() {
        String usernameOrEmail = this.getUsername();
        if( usernameOrEmail == null ) {
            usernameOrEmail = this.getEmail();
        }
        return usernameOrEmail;
    }

    public static boolean checkAvailable(@Nullable final String email, @Nullable final String username) {
        return UsergridUser.checkAvailable(Usergrid.getInstance(), email, username);
    }

    public static boolean checkAvailable(@NotNull final UsergridClient client, @Nullable final String email, @Nullable final String username) {
        if (email == null && username == null) {
            throw new IllegalArgumentException("email and username both are null ");
        }
        UsergridQuery query = new UsergridQuery(USER_ENTITY_TYPE);
        if (username != null) {
            query.eq(UsergridUserProperties.USERNAME.toString(), username);
        }
        if (email != null) {
            query.or().eq(UsergridUserProperties.EMAIL.toString(), email);
        }
        return client.GET(query).first() != null;
    }

    @NotNull
    public UsergridResponse create() {
        return this.create(Usergrid.getInstance());
    }

    @NotNull
    public UsergridResponse create(@NotNull final UsergridClient client) {
        UsergridResponse response = client.POST(this);
        UsergridUser createdUser = response.user();
        if( createdUser != null ) {
            this.copyAllProperties(createdUser);
        }
        return response;
    }

    @NotNull
    public UsergridResponse login(@NotNull final String username, @NotNull final String password) {
        return this.login(Usergrid.getInstance(),username,password);
    }

    @NotNull
    public UsergridResponse login(@NotNull final UsergridClient client, @NotNull final String username, @NotNull final String password) {
        UsergridUserAuth userAuth = new UsergridUserAuth(username,password);
        UsergridResponse response = client.authenticateUser(userAuth,false);
        if( response.ok() ) {
            this.userAuth = userAuth;
        }
        return response;
    }

    @NotNull
    public UsergridResponse resetPassword(@NotNull final String oldPassword, @NotNull final String newPassword) {
        return this.resetPassword(Usergrid.getInstance(),oldPassword,newPassword);
    }

    @NotNull
    public UsergridResponse resetPassword(@NotNull final UsergridClient client, @NotNull final String oldPassword, @NotNull final String newPassword) {
        return client.resetPassword(this,oldPassword,newPassword);
    }

    @NotNull
    public UsergridResponse reauthenticate() {
        return this.reauthenticate(Usergrid.getInstance());
    }

    @NotNull
    public UsergridResponse reauthenticate(@NotNull final UsergridClient client) {
        return this.userAuth == null ? UsergridResponse.fromError(client, "Invalid UsergridUserAuth.", "No UsergridUserAuth found on the UsergridUser.") : client.authenticateUser(this.userAuth, false);
    }

    @NotNull
    public UsergridResponse logout() {
        return this.logout(Usergrid.getInstance());
    }

    @NotNull
    public UsergridResponse logout(@NotNull final UsergridClient client) {
        UsergridResponse response;
        String uuidOrUsername = this.uuidOrUsername();
        String accessToken = (this.userAuth != null) ? this.userAuth.getAccessToken() : null;
        if (uuidOrUsername == null || accessToken == null ) {
            response = UsergridResponse.fromError(client,  "Logout Failed.", "UUID or Access Token not found on UsergridUser object.");
        } else {
            response = client.logoutUser(uuidOrUsername, accessToken);
            if( response.ok() ) {
                this.userAuth = null;
            }
        }
        return response;
    }
}
