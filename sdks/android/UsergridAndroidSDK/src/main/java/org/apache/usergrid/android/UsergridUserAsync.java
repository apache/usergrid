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
package org.apache.usergrid.android;

import org.apache.usergrid.android.callbacks.UsergridCheckAvailabilityCallback;
import org.apache.usergrid.android.callbacks.UsergridResponseCallback;
import org.apache.usergrid.android.tasks.UsergridAsyncTask;

import org.apache.usergrid.java.client.Usergrid;
import org.apache.usergrid.java.client.UsergridClient;
import org.apache.usergrid.java.client.UsergridEnums.*;
import org.apache.usergrid.java.client.model.UsergridUser;
import org.apache.usergrid.java.client.query.UsergridQuery;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public final class UsergridUserAsync {

    private UsergridUserAsync() {}

    public static void checkAvailable(@Nullable final String email, @Nullable final String username, @NotNull final UsergridCheckAvailabilityCallback checkAvailabilityCallback) {
        UsergridUserAsync.checkAvailable(Usergrid.getInstance(), email, username, checkAvailabilityCallback);
    }

    public static void checkAvailable(@NotNull final UsergridClient client, @Nullable final String email, @Nullable final String username, @NotNull final UsergridCheckAvailabilityCallback checkAvailabilityCallback) {
        if (email == null && username == null) {
            checkAvailabilityCallback.onResponse(false);
            return;
        }
        UsergridQuery query = new UsergridQuery(UsergridUser.USER_ENTITY_TYPE);
        if (username != null) {
            query.eq(UsergridUserProperties.USERNAME.toString(), username);
        }
        if (email != null) {
            query.or().eq(UsergridUserProperties.EMAIL.toString(), email);
        }
        UsergridAsync.GET(client, query, new UsergridResponseCallback() {
            @Override
            public void onResponse(@NotNull UsergridResponse response) {
                checkAvailabilityCallback.onResponse((response.ok() && response.first() != null));
            }
        });
    }

    public static void create(@NotNull final UsergridUser user, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridUserAsync.create(Usergrid.getInstance(),user,responseCallback);
    }

    public static void create(@NotNull final UsergridClient client, @NotNull final UsergridUser user, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return user.create(client);
            }
        }).execute();
    }

    public static void login(@NotNull final UsergridUser user, @NotNull final String username, @NotNull final String password, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridUserAsync.login(Usergrid.getInstance(),user,username,password,responseCallback);
    }

    public static void login(@NotNull final UsergridClient client, @NotNull final UsergridUser user, @NotNull final String username, @NotNull final String password, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return user.login(client,username,password);
            }
        }).execute();
    }

    public static void resetPassword(@NotNull final UsergridUser user, @NotNull final String oldPassword, @NotNull final String newPassword, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridUserAsync.resetPassword(Usergrid.getInstance(),user,oldPassword,newPassword,responseCallback);
    }

    public static void resetPassword(@NotNull final UsergridClient client, @NotNull final UsergridUser user, @NotNull final String oldPassword, @NotNull final String newPassword, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return user.resetPassword(client,oldPassword,newPassword);
            }
        }).execute();
    }

    public static void reauthenticate(@NotNull final UsergridUser user, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridUserAsync.reauthenticate(Usergrid.getInstance(),user,responseCallback);
    }

    public static void reauthenticate(@NotNull final UsergridClient client, @NotNull final UsergridUser user, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return user.reauthenticate(client);
            }
        }).execute();
    }

    public static void logout(@NotNull final UsergridUser user, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridUserAsync.logout(Usergrid.getInstance(),user,responseCallback);
    }

    public static void logout(@NotNull final UsergridClient client, @NotNull final UsergridUser user, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return user.logout(client);
            }
        }).execute();
    }
}
