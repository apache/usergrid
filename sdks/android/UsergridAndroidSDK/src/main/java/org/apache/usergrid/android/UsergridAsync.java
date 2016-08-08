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

import android.content.Context;

import org.apache.usergrid.android.callbacks.UsergridResponseCallback;
import org.apache.usergrid.android.tasks.UsergridAsyncTask;
import org.apache.usergrid.java.client.Usergrid;
import org.apache.usergrid.java.client.UsergridClient;
import org.apache.usergrid.java.client.UsergridEnums.UsergridDirection;
import org.apache.usergrid.java.client.UsergridRequest;
import org.apache.usergrid.java.client.auth.UsergridAppAuth;
import org.apache.usergrid.java.client.auth.UsergridUserAuth;
import org.apache.usergrid.java.client.model.UsergridEntity;
import org.apache.usergrid.java.client.model.UsergridUser;
import org.apache.usergrid.java.client.query.UsergridQuery;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public final class UsergridAsync {

    private UsergridAsync() { }

    public static void applyPushToken(@NotNull final Context context, @NotNull final String pushToken, @NotNull final String notifier, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.applyPushToken(Usergrid.getInstance(),context,pushToken,notifier,responseCallback);
    }

    public static void applyPushToken(@NotNull final UsergridClient client, @NotNull final Context context, @NotNull final String pushToken, @NotNull final String notifier, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridSharedDevice.applyPushToken(client,context,notifier,pushToken,responseCallback);
    }

    public static void authenticateApp(@NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.authenticateApp(Usergrid.getInstance(),responseCallback);
    }

    public static void authenticateApp(@NotNull final UsergridClient client, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.authenticateApp();
            }
        }).execute();
    }

    public static void authenticateApp(@NotNull final UsergridAppAuth auth, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.authenticateApp(Usergrid.getInstance(),auth,responseCallback);
    }

    public static void authenticateApp(@NotNull final UsergridClient client, @NotNull final UsergridAppAuth auth, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.authenticateApp(auth);
            }
        }).execute();
    }

    public static void authenticateUser(@NotNull final UsergridUserAuth userAuth, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.authenticateUser(Usergrid.getInstance(),userAuth,true,responseCallback);
    }

    public static void authenticateUser(@NotNull final UsergridClient client, @NotNull final UsergridUserAuth userAuth, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.authenticateUser(client,userAuth,true,responseCallback);
    }

    public static void authenticateUser(@NotNull final UsergridUserAuth userAuth, final boolean setAsCurrentUser, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.authenticateUser(Usergrid.getInstance(),userAuth,setAsCurrentUser,responseCallback);
    }

    public static void authenticateUser(@NotNull final UsergridClient client, @NotNull final UsergridUserAuth userAuth, final boolean setAsCurrentUser, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.authenticateUser(userAuth,setAsCurrentUser);
            }
        }).execute();
    }

    public static void resetPassword(@NotNull final UsergridUser user, @NotNull final String oldPassword, @NotNull final String newPassword, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.resetPassword(Usergrid.getInstance(),user,oldPassword,newPassword,responseCallback);
    }

    public static void resetPassword(@NotNull final UsergridClient client, @NotNull final UsergridUser user, @NotNull final String oldPassword, @NotNull final String newPassword, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.resetPassword(user,oldPassword,newPassword);
            }
        }).execute();
    }

    public static void logoutCurrentUser(@NotNull final UsergridResponseCallback responseCallback)  {
        UsergridAsync.logoutCurrentUser(Usergrid.getInstance(),responseCallback);
    }

    public static void logoutCurrentUser(@NotNull final UsergridClient client, @NotNull final UsergridResponseCallback responseCallback)  {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.logoutCurrentUser();
            }
        }).execute();
    }

    public static void logoutUserAllTokens(@NotNull final String uuidOrUsername, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.logoutUser(Usergrid.getInstance(),uuidOrUsername, null, responseCallback);
    }

    public static void logoutUserAllTokens(@NotNull final UsergridClient client, @NotNull final String uuidOrUsername, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.logoutUser(client,uuidOrUsername, null, responseCallback);
    }

    public static void logoutUser(@NotNull final UsergridClient client, @NotNull final String uuidOrUsername, @Nullable final String token, @NotNull final UsergridResponseCallback responseCallback){
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.logoutUser(uuidOrUsername,token);
            }
        }).execute();
    }

    public static void sendRequest(@NotNull final UsergridRequest request, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.sendRequest(Usergrid.getInstance(),request,responseCallback);
    }

    public static void sendRequest(@NotNull final UsergridClient client, @NotNull final UsergridRequest request, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.sendRequest(request);
            }
        }).execute();
    }

    public static void GET(@NotNull final String collection, @NotNull final String uuidOrName, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.GET(Usergrid.getInstance(),collection,uuidOrName,responseCallback);
    }

    public static void GET(@NotNull final UsergridClient client, @NotNull final String collection, @NotNull final String uuidOrName, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.GET(collection,uuidOrName);
            }
        }).execute();
    }

    public static void GET(@NotNull final String type, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.GET(Usergrid.getInstance(),type,responseCallback);
    }

    public static void GET(@NotNull final UsergridClient client, @NotNull final String type, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.GET(type);
            }
        }).execute();
    }

    public static void GET(@NotNull final UsergridQuery query, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.GET(Usergrid.getInstance(),query,responseCallback);
    }

    public static void GET(@NotNull final UsergridClient client, @NotNull final UsergridQuery query, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.GET(query);
            }
        }).execute();
    }

    public static void PUT(@NotNull final String type, @NotNull final String uuidOrName, @NotNull final Map<String, Object> jsonBody, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.PUT(Usergrid.getInstance(),type,uuidOrName,jsonBody,responseCallback);
    }

    public static void PUT(@NotNull final UsergridClient client, @NotNull final String type, @NotNull final String uuidOrName, @NotNull final Map<String, Object> jsonBody, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.PUT(type, uuidOrName, jsonBody);
            }
        }).execute();
    }

    public static void PUT(@NotNull final String type, @NotNull final Map<String, Object> jsonBody, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.PUT(Usergrid.getInstance(),type,jsonBody,responseCallback);
    }

    public static void PUT(@NotNull final UsergridClient client, @NotNull final String type, @NotNull final Map<String, Object> jsonBody, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.PUT(type, jsonBody);
            }
        }).execute();
    }

    public static void PUT(@NotNull final UsergridEntity entity, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.PUT(Usergrid.getInstance(),entity,responseCallback);
    }

    public static void PUT(@NotNull final UsergridClient client, @NotNull final UsergridEntity entity, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.PUT(entity);
            }
        }).execute();
    }

    public static void PUT(@NotNull final UsergridQuery query, @NotNull final Map<String, Object> jsonBody, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.PUT(Usergrid.getInstance(),query,jsonBody,responseCallback);
    }

    public static void PUT(@NotNull final UsergridClient client, @NotNull final UsergridQuery query, @NotNull final Map<String, Object> jsonBody, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.PUT(query, jsonBody);
            }
        }).execute();
    }

    public static void POST(@NotNull final UsergridEntity entity, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.POST(Usergrid.getInstance(),entity,responseCallback);
    }

    public static void POST(@NotNull final UsergridClient client, @NotNull final UsergridEntity entity, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.PUT(entity);
            }
        }).execute();
    }

    public static void POST(@NotNull final List<UsergridEntity> entities, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.POST(Usergrid.getInstance(),entities,responseCallback);
    }

    public static void POST(@NotNull final UsergridClient client, @NotNull final List<UsergridEntity> entities, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.POST(entities);
            }
        }).execute();
    }

    public static void POST(@NotNull final String type, @NotNull final String uuidOrName, @NotNull final Map<String, ?> jsonBody, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.POST(Usergrid.getInstance(),type,uuidOrName,jsonBody,responseCallback);
    }

    public static void POST(@NotNull final UsergridClient client, @NotNull final String type, @NotNull final String uuidOrName, @NotNull final Map<String, ?> jsonBody, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.POST(type, uuidOrName, jsonBody);
            }
        }).execute();
    }

    public static void POST(@NotNull final String type, @NotNull final Map<String, ?> jsonBody, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.POST(Usergrid.getInstance(),type,jsonBody,responseCallback);
    }

    public static void POST(@NotNull final UsergridClient client, @NotNull final String type, @NotNull final Map<String, ?> jsonBody, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.POST(type, jsonBody);
            }
        }).execute();
    }

    public static void POST(@NotNull final String type, @NotNull final List<Map<String, ?>> jsonBodies, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.POST(Usergrid.getInstance(),type,jsonBodies,responseCallback);
    }

    public static void POST(@NotNull final UsergridClient client, @NotNull final String type, @NotNull final List<Map<String, ?>> jsonBodies, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.POST(type, jsonBodies);
            }
        }).execute();
    }

    public static void DELETE(@NotNull final UsergridEntity entity, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.DELETE(Usergrid.getInstance(),entity,responseCallback);
    }

    public static void DELETE(@NotNull final UsergridClient client, @NotNull final UsergridEntity entity, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.DELETE(entity);
            }
        }).execute();
    }

    public static void DELETE(@NotNull final String type, @NotNull final String uuidOrName, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.DELETE(Usergrid.getInstance(),type,uuidOrName,responseCallback);
    }

    public static void DELETE(@NotNull final UsergridClient client, @NotNull final String type, @NotNull final String uuidOrName, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.DELETE(type, uuidOrName);
            }
        }).execute();
    }

    public static void DELETE(@NotNull final UsergridQuery query, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.DELETE(Usergrid.getInstance(),query,responseCallback);
    }

    public static void DELETE(@NotNull final UsergridClient client, @NotNull final UsergridQuery query, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.DELETE(query);
            }
        }).execute();
    }

    public static void connect(@NotNull final UsergridEntity entity, @NotNull final String relationship, @NotNull final UsergridEntity to, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.connect(Usergrid.getInstance(),entity,relationship,to,responseCallback);
    }

    public static void connect(@NotNull final UsergridClient client, @NotNull final UsergridEntity entity, @NotNull final String relationship, @NotNull final UsergridEntity to, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.connect(entity, relationship, to);
            }
        }).execute();
    }

    public static void connect(@NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String toType, @NotNull final String toName, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.connect(Usergrid.getInstance(),entityType,entityId,relationship,toType,toName,responseCallback);
    }

    public static void connect(@NotNull final UsergridClient client, @NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String toType, @NotNull final String toName, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.connect(entityType, entityId, relationship, toType, toName);
            }
        }).execute();
    }

    public static void connect(@NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String toId, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.connect(Usergrid.getInstance(),entityType,entityId,relationship,toId,responseCallback);
    }

    public static void connect(@NotNull final UsergridClient client, @NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String toId, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.connect(entityType, entityId, relationship, toId);
            }
        }).execute();
    }

    public static void disconnect(@NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String fromUuid, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.disconnect(Usergrid.getInstance(),entityType,entityId,relationship,fromUuid,responseCallback);
    }

    public static void disconnect(@NotNull final UsergridClient client, @NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String fromUuid, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.disconnect(entityType, entityId, relationship, fromUuid);
            }
        }).execute();
    }

    public static void disconnect(@NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String fromType, @NotNull final String fromName, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.disconnect(Usergrid.getInstance(),entityType,entityId,relationship,fromType,fromName,responseCallback);
    }

    public static void disconnect(@NotNull final UsergridClient client, @NotNull final String entityType, @NotNull final String entityId, @NotNull final String relationship, @NotNull final String fromType, @NotNull final String fromName, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.disconnect(entityType, entityId, relationship, fromType, fromName);
            }
        }).execute();
    }

    public static void disconnect(@NotNull final UsergridEntity entity, @NotNull final String relationship, @NotNull final UsergridEntity from, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.disconnect(Usergrid.getInstance(),entity,relationship,from,responseCallback);
    }

    public static void disconnect(@NotNull final UsergridClient client, @NotNull final UsergridEntity entity, @NotNull final String relationship, @NotNull final UsergridEntity from, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.disconnect(entity, relationship, from);
            }
        }).execute();
    }

    public static void getConnections(@NotNull final UsergridDirection direction, @NotNull final UsergridEntity entity, @NotNull final String relationship, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.getConnections(Usergrid.getInstance(),direction,entity,relationship,null,responseCallback);
    }

    public static void getConnections(@NotNull final UsergridClient client, @NotNull final UsergridDirection direction, @NotNull final UsergridEntity entity, @NotNull final String relationship, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.getConnections(client,direction,entity,relationship,null,responseCallback);
    }

    public static void getConnections(@NotNull final UsergridDirection direction, @NotNull final UsergridEntity entity, @NotNull final String relationship, @Nullable final UsergridQuery query, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.getConnections(Usergrid.getInstance(),direction,entity,relationship,query,responseCallback);
    }

    public static void getConnections(@NotNull final UsergridClient client, @NotNull final UsergridDirection direction, @NotNull final UsergridEntity entity, @NotNull final String relationship, @Nullable final UsergridQuery query, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.getConnections(direction, entity, relationship, query);
            }
        }).execute();
    }

    public static void getConnections(@NotNull final UsergridDirection direction, @NotNull final String type, @NotNull final String uuidOrName, @NotNull final String relationship, @Nullable final UsergridQuery query, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.getConnections(Usergrid.getInstance(),direction,type,uuidOrName,relationship,query,responseCallback);
    }

    public static void getConnections(@NotNull final UsergridClient client, @NotNull final UsergridDirection direction, @NotNull final String type, @NotNull final String uuidOrName, @NotNull final String relationship, @Nullable final UsergridQuery query, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.getConnections(direction, type, uuidOrName, relationship, query);
            }
        }).execute();
    }

    public static void getConnections(@NotNull final UsergridDirection direction, @NotNull final String uuid, @NotNull final String relationship, @Nullable final UsergridQuery query, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridAsync.getConnections(Usergrid.getInstance(),direction,uuid,relationship,query,responseCallback);
    }

    public static void getConnections(@NotNull final UsergridClient client, @NotNull final UsergridDirection direction, @NotNull final String uuid, @NotNull final String relationship, @Nullable final UsergridQuery query, @NotNull final UsergridResponseCallback responseCallback) {
        (new UsergridAsyncTask(responseCallback) {
            @Override
            public UsergridResponse doTask() {
                return client.getConnections(direction, uuid, relationship, query);
            }
        }).execute();
    }
}
