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

package org.apache.usergrid.persistence.model.util;

import java.util.HashSet;
import java.util.Set;

public class CollectionUtils {
    public static final String VERSIONED_NAME_SEPARATOR = "~-_~_-~";

    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_GROUPS = "groups";
    public static final String COLLECTION_ASSETS = "assets";
    public static final String COLLECTION_ACTIVITIES = "activities";
    public static final String COLLECTION_EVENTS = "events";
    public static final String COLLECTION_FOLDERS = "folders";
    public static final String COLLECTION_DEVICES = "devices";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";
    public static final String COLLECTION_ROLES = "roles";

    public static final String COLLECTION_ENTITY_USER = "user";
    public static final String COLLECTION_ENTITY_GROUP = "group";
    public static final String COLLECTION_ENTITY_ASSET = "asset";
    public static final String COLLECTION_ENTITY_ACTIVITY = "activity";
    public static final String COLLECTION_ENTITY_EVENT = "event";
    public static final String COLLECTION_ENTITY_FOLDER = "folder";
    public static final String COLLECTION_ENTITY_DEVICE = "device";
    public static final String COLLECTION_ENTITY_NOTIFICATION = "notification";
    public static final String COLLECTION_ENTITY_ROLE = "role";

    private static final Set<String> customNames;

    static {
        customNames = new HashSet<>();
        customNames.add(COLLECTION_USERS);
        customNames.add(COLLECTION_GROUPS);
        customNames.add(COLLECTION_ASSETS);
        customNames.add(COLLECTION_ACTIVITIES);
        customNames.add(COLLECTION_EVENTS);
        customNames.add(COLLECTION_FOLDERS);
        customNames.add(COLLECTION_DEVICES);
        customNames.add(COLLECTION_NOTIFICATIONS);
        customNames.add(COLLECTION_ROLES);

        customNames.add(COLLECTION_ENTITY_USER);
        customNames.add(COLLECTION_ENTITY_GROUP);
        customNames.add(COLLECTION_ENTITY_ASSET);
        customNames.add(COLLECTION_ENTITY_ACTIVITY);
        customNames.add(COLLECTION_ENTITY_EVENT);
        customNames.add(COLLECTION_ENTITY_FOLDER);
        customNames.add(COLLECTION_ENTITY_DEVICE);
        customNames.add(COLLECTION_ENTITY_NOTIFICATION);
        customNames.add(COLLECTION_ENTITY_ROLE);
    }

    public static boolean isCustomCollectionOrEntityName(String collectionName) {
        return !customNames.contains(collectionName);
    }

    public static String stripEmptyVersion(final String name) {
        // versioned name with empty version is name followed by separator
        if (name.endsWith(VERSIONED_NAME_SEPARATOR)) {
            return name.substring(0, name.length() - VERSIONED_NAME_SEPARATOR.length());
        }
        return name;
    }

    public static String addEmptyVersion(final String name) {
        if (!isCustomCollectionOrEntityName(name) ||
            name.contains(VERSIONED_NAME_SEPARATOR)) {
            // not custom collection or already has version
            return name;
        }
        return name + VERSIONED_NAME_SEPARATOR;
    }

    public static String handleEmptyVersion(final String name, boolean addEmptyVersion) {
        String ret;
        if (addEmptyVersion) {
            ret = addEmptyVersion(name);
        } else {
            ret = stripEmptyVersion(name);
        }
        return ret;
    }
}
