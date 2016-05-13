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
public class UsergridAppAuth extends UsergridAuth {

    @NotNull private String clientId;
    @NotNull private String clientSecret;

    @NotNull public String getClientId() { return clientId; }
    public void setClientId(@NotNull final String clientId) { this.clientId = clientId; }

    @NotNull private String getClientSecret() { return clientSecret; }
    public void setClientSecret(@NotNull final String clientSecret) { this.clientSecret = clientSecret; }

    @NotNull
    @Override
    public HashMap<String, String> credentialsMap() {
        HashMap<String,String> credentials = super.credentialsMap();
        credentials.put("grant_type", "client_credentials");
        credentials.put("client_id", this.clientId);
        credentials.put("client_secret", this.clientSecret);
        return credentials;
    }

    public UsergridAppAuth() {
        this("","");
    }

    public UsergridAppAuth(@JsonProperty("clientId") @NotNull final String clientId, @JsonProperty("clientSecret") @NotNull final String clientSecret) {
        super();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
}
