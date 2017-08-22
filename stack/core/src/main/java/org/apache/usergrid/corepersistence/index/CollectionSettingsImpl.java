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


import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


/**
 * Cache collection settings to reduce load on Cassandra.
 */
public class CollectionSettingsImpl implements CollectionSettings {
    private static final Logger logger = LoggerFactory.getLogger(CollectionSettingsImpl.class );

    private final MapManager mapManager;
    private final CollectionSettingsCache cache;
    private final CollectionSettingsScope scope;

    @Inject
    public CollectionSettingsImpl( CollectionSettingsScope scope, MapManager mapManager, CollectionSettingsCache cache ) {
        this.scope = scope;
        this.mapManager = mapManager;
        this.cache = cache;


    }

    @Override
    public Optional<Map<String, Object>> getCollectionSettings(final String collectionName ) {

        String settings;

        settings = cache.get(scope);

        if( settings == null ) {
            settings = mapManager.getString(collectionName);

        }

        if (settings != null) {

            if( settings.isEmpty() ){
                return Optional.absent(); // empty string means it's empty.  we store empty string for cache purposes
            }else{
                return Optional.of((Map<String, Object>) JsonUtils.parse(settings));
            }

        }else{
            cache.put(scope, ""); // store empty string here so empty is cached as well
        }

        return Optional.absent();
    }

    @Override
    public void putCollectionSettings(final String collectionName, final String collectionSchema ){
        mapManager.putString( collectionName, collectionSchema );
        cache.put(scope, collectionSchema);
    }


    @Override
    public void deleteCollectionSettings(final String collectionName){
        mapManager.delete( collectionName );
        cache.invalidate( scope );
    }


}
