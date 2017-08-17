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

import com.google.inject.Inject;
import org.apache.usergrid.corepersistence.asyncevents.CollectionClearTooSoonException;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Cache collection version to reduce load on Cassandra.
 */
public class CollectionVersionManagerImpl implements CollectionVersionManager {
    private static final Logger logger = LoggerFactory.getLogger(CollectionVersionManagerImpl.class );

    private final MapManager mapManager;
    private final CollectionVersionCache cache;
    private final CollectionScope scope;
    private final CollectionVersionFig collectionVersionFig;
    private final String collectionName;

    private static final String MAP_PREFIX_VERSION = "VERSION:";
    private static final String MAP_PREFIX_LAST_CHANGED = "LASTCHANGED:";

    @Inject
    public CollectionVersionManagerImpl(CollectionScope scope, MapManager mapManager, CollectionVersionCache cache, CollectionVersionFig collectionVersionFig) {
        this.scope = scope;
        this.mapManager = mapManager;
        this.cache = cache;
        this.collectionVersionFig = collectionVersionFig;
        this.collectionName = scope.getCollectionName();
    }

    @Override
    public String getCollectionVersion(final boolean bypassCache) {

        String version = null;
        if (!bypassCache) {
            version = cache.get(scope);
        }

        if( version == null ) {
            version = mapManager.getString(MAP_PREFIX_VERSION+collectionName);
        }

        if (version != null) {
            return version;
        }else{
            cache.put(scope, ""); // store empty string here so empty is cached as well
        }

        return "";
    }

    @Override
    public Long getTimeLastChanged() {
        return mapManager.getLong(MAP_PREFIX_LAST_CHANGED+collectionName);
    }

    @Override
    public String getVersionedCollectionName(final boolean bypassCache) {
        String collectionVersion = getCollectionVersion(bypassCache);
        return CollectionVersionUtils.buildVersionedNameString(collectionName, collectionVersion, false);
    }

    // returns old collection version
    @Override
    public String updateCollectionVersion() throws CollectionClearTooSoonException {
        // check for time last changed
        Long timeLastChanged = getTimeLastChanged();
        long timeBetweenDeletes = collectionVersionFig.getTimeBetweenDeletes();
        if (timeLastChanged != null) {
            if (System.currentTimeMillis() - timeLastChanged < timeBetweenDeletes) {
                // too soon
                throw new CollectionClearTooSoonException(timeLastChanged, timeBetweenDeletes);
            }
        }

        String oldCollectionVersion = getCollectionVersion(true);
        String newCollectionVersion = generateNewCollectionVersion();
        mapManager.putLong(MAP_PREFIX_LAST_CHANGED+collectionName, System.currentTimeMillis());
        mapManager.putString(MAP_PREFIX_VERSION+collectionName, newCollectionVersion);
        cache.put(scope, newCollectionVersion);
        logger.info("Replacing collection version for collection {}, application {}: oldVersion={} newVersion={}",
            collectionName, scope.getApplication().getUuid(), oldCollectionVersion, newCollectionVersion);
        return oldCollectionVersion;
    }

    private static String generateNewCollectionVersion() {
        return UUIDGenerator.newTimeUUID().toString();
    }

}
