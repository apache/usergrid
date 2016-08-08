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
package org.apache.usergrid.activityfeed;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

import org.apache.usergrid.activityfeed.activities.FeedActivity;
import org.apache.usergrid.activityfeed.callbacks.GetFeedMessagesCallback;
import org.apache.usergrid.activityfeed.callbacks.PostFeedMessageCallback;
import org.apache.usergrid.activityfeed.helpers.AlertDialogHelpers;
import org.apache.usergrid.android.UsergridAsync;
import org.apache.usergrid.android.UsergridSharedDevice;
import org.apache.usergrid.android.UsergridUserAsync;
import org.apache.usergrid.android.callbacks.UsergridResponseCallback;
import org.apache.usergrid.java.client.Usergrid;
import org.apache.usergrid.java.client.UsergridEnums;
import org.apache.usergrid.java.client.UsergridRequest;
import org.apache.usergrid.java.client.auth.UsergridUserAuth;
import org.apache.usergrid.java.client.model.UsergridEntity;
import org.apache.usergrid.java.client.model.UsergridUser;
import org.apache.usergrid.java.client.query.UsergridQuery;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.apache.usergrid.java.client.response.UsergridResponseError;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public final class UsergridManager {

    private static final String ORG_ID = "rwalsh";
    private static final String APP_ID = "sandbox";
    private static final String BASE_URL = "https://api.usergrid.com";
    private static final String ANDROID_NOTIFIER_ID = "androidPushNotifier";

    public static String GCM_SENDER_ID = "186455511595";
    public static String GCM_REGISTRATION_ID = "";

    private UsergridManager() {}

    public static void initializeSharedInstance(@NonNull final Context context) {
        Usergrid.initSharedInstance(ORG_ID,APP_ID,BASE_URL);
        Usergrid.setAuthMode(UsergridEnums.UsergridAuthMode.USER);
        UsergridEntity.mapCustomSubclassToType(ActivityEntity.ACTIVITY_ENTITY_TYPE,ActivityEntity.class);
        UsergridSharedDevice.saveSharedDevice(context, new UsergridResponseCallback() {
            @Override
            public void onResponse(@NotNull UsergridResponse response) { }
        });
        registerPush(context);
    }

    public static void registerPush(Context context) {
        final String regId = GCMRegistrar.getRegistrationId(context);
        if ("".equals(regId)) {
            GCMRegistrar.register(context, GCM_SENDER_ID);
        } else {
            if (GCMRegistrar.isRegisteredOnServer(context)) {
                Log.i("", "Already registered with GCM");
            } else {
                registerPush(context, regId);
            }
        }
    }

    public static void registerPush(@NonNull final Context context, @NonNull final String registrationId) {
        GCM_REGISTRATION_ID = registrationId;
        UsergridAsync.applyPushToken(context, registrationId, ANDROID_NOTIFIER_ID, new UsergridResponseCallback() {
            @Override
            public void onResponse(@NonNull UsergridResponse response) {
                if( !response.ok() && response.getResponseError() != null ) {
                    System.out.print("Error Description :" + response.getResponseError().toString());
                }
            }
        });
    }

    public static void loginUser(@NonNull final Activity activity, @NonNull final String username, @NonNull final String password) {
        UsergridAsync.authenticateUser(new UsergridUserAuth(username,password), new UsergridResponseCallback() {
            @Override
            public void onResponse(@NotNull final UsergridResponse response) {
                final UsergridUser currentUser = Usergrid.getCurrentUser();
                if( response.ok() && currentUser != null ) {
                    UsergridAsync.connect("users", "me", "devices", UsergridSharedDevice.getSharedDeviceUUID(activity), new UsergridResponseCallback() {
                        @Override
                        public void onResponse(@NotNull UsergridResponse response) {
                            AlertDialogHelpers.showScrollableAlert(activity,"Authenticate User Successful","User Description: \n\n " + currentUser.toPrettyString(), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.startActivity(new Intent(activity,FeedActivity.class));
                                }
                            });
                        }
                    });
                } else {
                    AlertDialogHelpers.showAlert(activity,"Error Authenticating User","Invalid username or password.");
                }
            }
        });
    }

    public static void logoutCurrentUser(@NonNull final Activity activity) {
        UsergridAsync.disconnect("users", "me", "devices", UsergridSharedDevice.getSharedDevice(activity).getUuid(), new UsergridResponseCallback() {
            @Override
            public void onResponse(@NotNull UsergridResponse response) {
                UsergridAsync.logoutCurrentUser(new UsergridResponseCallback() {
                    @Override
                    public void onResponse(@NotNull UsergridResponse response) {
                        System.out.print(response.toString());
                    }
                });
            }
        });
    }

    public static void createUserAccount(@NonNull final Activity activity, @NonNull final String name, @NonNull final String username, @NonNull final String email, @NonNull final String password) {
        final UsergridUser user = new UsergridUser(name,username,email,password);
        UsergridUserAsync.create(user, new UsergridResponseCallback() {
            @Override
            public void onResponse(@NotNull UsergridResponse response) {
                final UsergridUser responseUser = response.user();
                if( response.ok() && responseUser != null ) {
                    AlertDialogHelpers.showScrollableAlert(activity, "Creating Account Successful", "User Description: \n\n " + responseUser.toPrettyString(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.finish();
                        }
                    });
                } else {
                    String errorMessage = "Unknown Error";
                    UsergridResponseError responseError = response.getResponseError();
                    if( responseError != null ) {
                        errorMessage = responseError.getErrorDescription();
                    }
                    AlertDialogHelpers.showAlert(activity,"Error Creating Account",errorMessage);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static void getFeedMessages(@NonNull final GetFeedMessagesCallback callback) {
        final UsergridQuery feedMessagesQuery = new UsergridQuery("users/me/feed").desc(UsergridEnums.UsergridEntityProperties.CREATED.toString());
        UsergridAsync.GET(feedMessagesQuery, new UsergridResponseCallback() {
            @Override
            public void onResponse(@NotNull UsergridResponse response) {
                ArrayList<ActivityEntity> feedMessages = new ArrayList<>();
                if( response.ok() ) {
                    List feedEntities = response.getEntities();
                    if( feedEntities != null ) {
                        Collections.reverse(feedEntities);
                        feedMessages.addAll((List<ActivityEntity>)feedEntities);
                    }
                }
                callback.onResponse(feedMessages);
            }
        });
    }

    public static void postFeedMessage(@NonNull final String messageText, @NonNull final PostFeedMessageCallback callback) {
        final UsergridUser currentUser = Usergrid.getCurrentUser();
        if( currentUser != null ) {
            String usernameOrEmail = currentUser.usernameOrEmail();
            if( usernameOrEmail == null ) {
                usernameOrEmail = "";
            }
            String email = currentUser.getEmail();
            if( email == null ) {
                email = "";
            }
            String picture = currentUser.getPicture();
            final ActivityEntity activityEntity = new ActivityEntity(usernameOrEmail,email,picture,messageText);
            UsergridAsync.POST("users/me/activities",activityEntity.toMapValue(), new UsergridResponseCallback() {
                @Override
                public void onResponse(@NotNull UsergridResponse response) {
                    final UsergridEntity responseEntity = response.entity();
                    if( response.ok() && responseEntity != null && responseEntity instanceof ActivityEntity ) {
                        callback.onSuccess((ActivityEntity)responseEntity);
                        UsergridManager.sendPushToFollowers(messageText);
                    }
                }
            });
        }
    }

    public static void followUser(@NonNull final Activity activity, @NonNull final String username) {
        UsergridAsync.connect("users", "me", "following", "users", username, new UsergridResponseCallback() {
            @Override
            public void onResponse(@NotNull UsergridResponse response) {
                if( response.ok() ) {
                    activity.finish();
                } else {
                    String errorMessage = "Unknown Error";
                    UsergridResponseError responseError = response.getResponseError();
                    if( responseError != null ) {
                        String errorDescription = responseError.getErrorDescription();
                        if( errorDescription != null ) {
                            errorMessage = errorDescription;
                        }
                    }
                    AlertDialogHelpers.showAlert(activity,"Error Following User",errorMessage);
                }
            }
        });
    }

    public static void sendPushToFollowers(@NonNull final String message) {
        HashMap<String,String> notificationMap = new HashMap<>();
        notificationMap.put(ANDROID_NOTIFIER_ID,message);
        final HashMap<String,HashMap<String,String>> payloadMap = new HashMap<>();
        payloadMap.put("payloads",notificationMap);

        UsergridAsync.GET("users/me/followers", new UsergridResponseCallback() {
            @Override
            public void onResponse(@NotNull UsergridResponse response) {
                if( response.ok() ) {
                    String followerUserNames = "";
                    final List<UsergridUser> users = response.users();
                    if( users != null && !users.isEmpty() ) {
                        for( UsergridUser user : users ) {
                            String username = user.getUsername();
                            if( username != null && !username.isEmpty() ) {
                                followerUserNames += username + ";";
                            }
                        }
                        if( !followerUserNames.isEmpty() ) {
                            final UsergridRequest notificationRequest = new UsergridRequest(UsergridEnums.UsergridHttpMethod.POST,UsergridRequest.APPLICATION_JSON_MEDIA_TYPE,Usergrid.clientAppUrl(),null,payloadMap,Usergrid.authForRequests(),"users", followerUserNames, "notifications");
                            UsergridAsync.sendRequest(notificationRequest, new UsergridResponseCallback() {
                                @Override
                                public void onResponse(@NonNull UsergridResponse response) {}
                            });
                        }
                    }
                }
            }
        });
    }
}
