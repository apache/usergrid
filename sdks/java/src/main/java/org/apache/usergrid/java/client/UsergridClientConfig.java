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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UsergridClientConfig {

    // The organization identifier.
    @NotNull public String orgId;

    // The application identifier.
    @NotNull public String appId;

    // The base URL that all calls will be made with.
    @NotNull public String baseUrl = UsergridClient.DEFAULT_BASE_URL;

    // The `UsergridAuthMode` value used to determine what type of token will be sent, if any.
    @NotNull public UsergridAuthMode authMode = UsergridAuthMode.USER;

    @Nullable public UsergridAppAuth appAuth = null;

    @SuppressWarnings("unused")
    private UsergridClientConfig() {}

    public UsergridClientConfig(@NotNull final String orgId, @NotNull final String appId) {
        this.orgId = orgId;
        this.appId = appId;
    }

    public UsergridClientConfig(@NotNull final String orgId, @NotNull final String appId, @NotNull final String baseUrl) {
        this.orgId = orgId;
        this.appId = appId;
        this.baseUrl = baseUrl;
    }

    public UsergridClientConfig(@NotNull final String orgId, @NotNull final String appId, @NotNull final String baseUrl, @NotNull final UsergridAuthMode authMode) {
        this.orgId = orgId;
        this.appId = appId;
        this.baseUrl = baseUrl;
        this.authMode = authMode;
    }
}
