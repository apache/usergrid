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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

@SuppressWarnings("unused")
public class UsergridAuth {

    @Nullable private String accessToken = null;
    @Nullable private Long expiry = null;
    private boolean usingToken = false;
    private boolean isAdminUser = false;

    public UsergridAuth() { }

    public UsergridAuth(@JsonProperty("accessToken") @Nullable final String accessToken) {
        this.usingToken = true;
        setAccessToken(accessToken);
    }

    public UsergridAuth(@JsonProperty("accessToken") @Nullable final String accessToken, @JsonProperty("expiry") @Nullable final Long expiry) {
        this.usingToken = true;
        setAccessToken(accessToken);
        setExpiry(expiry);
    }

    public void destroy() {
        setAccessToken(null);
        setExpiry(null);
    }

    @Nullable public String getAccessToken() { return accessToken; }
    public void setAccessToken(@Nullable final String accessToken) {
        this.accessToken = accessToken;
    }

    @Nullable public Long getExpiry() { return expiry; }
    public void setExpiry(@Nullable final Long tokenExpiry) { this.expiry = tokenExpiry; }

    public boolean isValidToken() { return (hasToken() && !isExpired()); }

    public boolean hasToken() { return accessToken != null; }

    public boolean isExpired() {
        if (expiry != null) {
            Long currentTime = System.currentTimeMillis() / 1000;
            return ((expiry / 1000) < currentTime);
        } else {
            return !this.usingToken;
        }
    }

    @NotNull
    public HashMap<String,String> credentialsMap() {
        return new HashMap<>();
    }
}