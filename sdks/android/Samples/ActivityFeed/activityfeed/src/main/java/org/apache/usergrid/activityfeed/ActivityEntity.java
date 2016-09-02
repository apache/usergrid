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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import org.apache.usergrid.java.client.model.UsergridEntity;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

@SuppressWarnings("unused")
public class ActivityEntity extends UsergridEntity {

    public static final String ACTIVITY_ENTITY_TYPE = "activity";

    private String content;
    private JsonNode actor;

    @Nullable @JsonIgnore
    public String getDisplayName() {
        if( actor != null ) {
            JsonNode displayName = actor.get("displayName");
            if( displayName != null ) {
                return displayName.asText();
            }
        }
        return null;
    }

    @Nullable public String getContent() { return this.content; }
    public void setContent(@NonNull String content) {
        this.content = content;
    }

    @Nullable public JsonNode getActor() { return this.actor; }
    public void setActor(@NonNull JsonNode actor) {
        this.actor = actor;
    }

    public ActivityEntity() {
        super(ACTIVITY_ENTITY_TYPE);
    }

    public ActivityEntity(@JsonProperty("type") @NotNull String type) {
        super(type);
    }

    public ActivityEntity(@NonNull final String displayName, @NonNull final String email, @Nullable final String picture, @NonNull final String content) {
        super(ACTIVITY_ENTITY_TYPE);
        HashMap<String,Object> actorMap = new HashMap<>();
        actorMap.put("displayName",displayName);
        actorMap.put("email",email);
        if( picture != null ) {
            HashMap<String,Object> imageMap = new HashMap<>();
            imageMap.put("url",picture);
            imageMap.put("height",80);
            imageMap.put("width",80);
            actorMap.put("image",imageMap);
        }
        HashMap<String,Object> activityMap = new HashMap<>();
        activityMap.put("verb","post");
        activityMap.put("actor",actorMap);
        activityMap.put("content",content);
        this.putProperties(activityMap);
    }
}
