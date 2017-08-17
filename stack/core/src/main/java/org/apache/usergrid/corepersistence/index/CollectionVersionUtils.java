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

package org.apache.usergrid.corepersistence.index;


import com.google.common.base.Preconditions;
import org.apache.usergrid.utils.StringUtils;

import java.util.regex.Pattern;

import static org.apache.usergrid.persistence.model.util.CollectionUtils.VERSIONED_NAME_SEPARATOR;

public class CollectionVersionUtils {

    public static VersionedCollectionName parseVersionedName(String versionedCollectionNameString) throws IllegalArgumentException {
        Preconditions.checkNotNull(versionedCollectionNameString, "collection name string is required");
        String collectionName;
        String collectionVersion;
        try {
            String[] parts = versionedCollectionNameString.split(Pattern.quote(VERSIONED_NAME_SEPARATOR));
            if (parts.length == 2) {
                collectionName = parts[0];
                collectionVersion = parts[1];
            } else if (parts.length == 1) {
                collectionName = parts[0];
                collectionVersion = "";
            } else {
                throw new IllegalArgumentException("Invalid format for versioned collection, versionedCollectionNameString=" + versionedCollectionNameString);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse versioned collection, versionedCollectionNameString=" + versionedCollectionNameString, e);
        }
        return new VersionedCollectionNameImpl(collectionName, collectionVersion);
    }

    public static String getBaseCollectionName(String versionedCollectionNameString) throws IllegalArgumentException {
        return parseVersionedName(versionedCollectionNameString).getCollectionName();
    }

    public static String getCollectionVersion(String versionedCollectionNameString) throws IllegalArgumentException {
        return parseVersionedName(versionedCollectionNameString).getCollectionVersion();
    }

    public static boolean collectionNameHasVersion(String collectionNameString) {
        try {
            VersionedCollectionName parsedName = parseVersionedName(collectionNameString);
            return !StringUtils.isNullOrEmpty(parsedName.getCollectionVersion());
        }
        catch (Exception e) {
            return false;
        }
    }

    public static boolean isVersionedName(String name) {
        try {
            return name.contains(VERSIONED_NAME_SEPARATOR);
        }
        catch (Exception e) {
            return false;
        }
    }

    public static String buildVersionedNameString(final String baseName, final String collectionVersion,
                                                   final boolean validateBaseName) throws IllegalArgumentException {
        return buildVersionedNameString(baseName, collectionVersion, validateBaseName, false);
    }

    public static String buildVersionedNameString(final String baseName, final String collectionVersion,
                                                  final boolean validateBaseName, final boolean forceVersion) {
        Preconditions.checkNotNull(baseName, "base name is required");
        if (validateBaseName && baseName.contains(VERSIONED_NAME_SEPARATOR)) {
            throw new IllegalArgumentException("Cannot build versioned name using a base name that already includes the version separator");
        }
        if (!forceVersion && collectionVersion == null || collectionVersion == "") {
            return baseName;
        }
        return baseName + VERSIONED_NAME_SEPARATOR + collectionVersion;

    }

    public static VersionedCollectionName createVersionedName(final String baseName, final String collectionVersion) {
        return new VersionedCollectionNameImpl(baseName, collectionVersion);
    }

}
