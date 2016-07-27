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
package org.apache.usergrid.java.client.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

@SuppressWarnings("unused")
public class UsergridUserAuth extends UsergridAuth {

    @NotNull private String username;
    @NotNull private String password;
    private boolean isAdminUser = false;

    @NotNull public String getUsername() { return username; }
    public void setUsername(@NotNull final String username) { this.username = username; }

    @NotNull private String getPassword() { return password; }
    public void setPassword(@NotNull final String password) { this.password = password; }

    public boolean isAdminUser() { return isAdminUser; }

    @NotNull
    @Override
    public HashMap<String, String> credentialsMap() {
        HashMap<String,String> credentials = super.credentialsMap();
        credentials.put("grant_type", "password");
        credentials.put("username", this.username);
        credentials.put("password", this.password);
        return credentials;
    }

    public UsergridUserAuth() {
        this("","");
    }

    public UsergridUserAuth(@JsonProperty("username") @NotNull final String username,
                            @JsonProperty("password") @NotNull final String password) {
        super();
        this.username = username;
        this.password = password;
    }

    public UsergridUserAuth(@JsonProperty("username") @NotNull final String username,
                            @JsonProperty("password") @NotNull final String password,
                            @JsonProperty("isAdminUser") final boolean isAdminUser) {
        super();
        this.username = username;
        this.password = password;
        this.isAdminUser = isAdminUser;
    }
}
