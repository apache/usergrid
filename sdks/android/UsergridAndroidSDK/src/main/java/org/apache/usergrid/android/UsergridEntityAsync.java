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

import org.apache.usergrid.android.callbacks.UsergridResponseCallback;
import org.apache.usergrid.android.tasks.UsergridAsyncTask;
import org.apache.usergrid.java.client.Usergrid;
import org.apache.usergrid.java.client.UsergridClient;
import org.apache.usergrid.java.client.UsergridEnums.UsergridDirection;
import org.apache.usergrid.java.client.model.UsergridEntity;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class UsergridEntityAsync {

    private UsergridEntityAsync() {}

    public static void reload(@NotNull final UsergridEntity entity, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridEntityAsync.reload(Usergrid.getInstance(),entity,responseCallback);
    }

    public static void reload(@NotNull final UsergridClient client, @NotNull final UsergridEntity entity, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return entity.reload(client);
            }
        }).execute();
    }

    public static void save(@NotNull final UsergridEntity entity, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridEntityAsync.save(Usergrid.getInstance(),entity,responseCallback);
    }

    public static void save(@NotNull final UsergridClient client, @NotNull final UsergridEntity entity, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return entity.save(client);
            }
        }).execute();
    }

    public static void remove(@NotNull final UsergridEntity entity, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridEntityAsync.remove(Usergrid.getInstance(),entity, responseCallback);
    }

    public static void remove(@NotNull final UsergridClient client, @NotNull final UsergridEntity entity, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return entity.remove(client);
            }
        }).execute();
    }

    public static void connect(@NotNull final UsergridEntity entity, @NotNull final String relationship, @NotNull final UsergridEntity toEntity, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridEntityAsync.connect(Usergrid.getInstance(), entity, relationship, toEntity, responseCallback);
    }

    public static void connect(@NotNull final UsergridClient client, @NotNull final UsergridEntity entity, @NotNull final String relationship, @NotNull final UsergridEntity toEntity, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return entity.connect(client,relationship,toEntity);
            }
        }).execute();
    }

    public static void disconnect(@NotNull final UsergridEntity entity, @NotNull final String relationship, @NotNull final UsergridEntity fromEntity, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridEntityAsync.disconnect(Usergrid.getInstance(), entity, relationship, fromEntity, responseCallback);
    }

    public static void disconnect(@NotNull final UsergridClient client, @NotNull final UsergridEntity entity, @NotNull final String relationship, @NotNull final UsergridEntity fromEntity, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return entity.disconnect(client,relationship,fromEntity);
            }
        }).execute();
    }

    public static void getConnections(@NotNull final UsergridEntity entity, @NotNull final UsergridDirection direction, @NotNull final String relationship, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridEntityAsync.getConnections(Usergrid.getInstance(),entity,direction,relationship,responseCallback);
    }

    public static void getConnections(@NotNull final UsergridClient client, @NotNull final UsergridEntity entity, @NotNull final UsergridDirection direction, @NotNull final String relationship, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return entity.getConnections(client,direction,relationship);
            }
        }).execute();
    }
}
