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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * Cache collection settings to reduce load on Cassandra.
 */
@Singleton
public class CollectionSettingsCacheImpl implements CollectionSettingsCache {
    private static final Logger logger = LoggerFactory.getLogger(CollectionSettingsCacheImpl.class );

    private final LoadingCache<String,Optional<Map<String, Object>>> indexSchemaCache;
    private final MapManager mapManager;


    public CollectionSettingsCacheImpl( MapManager mapManager, CollectionSettingsCacheFig indexSchemaCacheFig) {
        this.mapManager = mapManager;

        indexSchemaCache = CacheBuilder.newBuilder()
            .maximumSize( indexSchemaCacheFig.getCacheSize() )
            .expireAfterWrite( indexSchemaCacheFig.getCacheTimeout(), TimeUnit.MILLISECONDS )
            .build( new CacheLoader<String, Optional<Map<String, Object>>>() {
                @Override
                public Optional<Map<String, Object>> load( final String collectionName ) throws Exception {
                    return Optional.fromNullable( retrieveCollectionSchema( collectionName ) );
                }
            } );
    }

    private Map retrieveCollectionSchema( final String collectionName ){
        String collectionIndexingSchema = mapManager.getString( collectionName );
        Map parsedCollectionIndexingSchema = null;
        if(collectionIndexingSchema!=null){
            return (Map) JsonUtils.parse( collectionIndexingSchema );

        }
        return parsedCollectionIndexingSchema;
    }


    @Override
    public Optional<Map<String, Object>> getCollectionSettings(final String collectionName ) {

        try {
            Optional<Map<String, Object>> optionalCollectionSchema = indexSchemaCache.get( collectionName );
            if(!optionalCollectionSchema.isPresent()){
                indexSchemaCache.invalidate( collectionName );
                return Optional.absent();
            }
            return optionalCollectionSchema;

        } catch ( Exception e ) {
            if(logger.isDebugEnabled()){
                logger.debug( "Returning for collection name: {} "
                    + "resulted in the following failure: {}",collectionName,e );
            }
        }
        return null;
    }

    @Override
    public void putCollectionSettings(final String collectionName, final String collectionSchema ){
        mapManager.putString( collectionName, collectionSchema );
        evictCollectionSettings( collectionName );
    }


    @Override
    public void deleteCollectionSettings(final String collectionName){
        mapManager.delete( collectionName );
        evictCollectionSettings( collectionName );
    }


    @Override
    public void evictCollectionSettings(final String collectionName ) {
        indexSchemaCache.invalidate( collectionName );
        if(logger.isDebugEnabled() ){
            logger.debug( "Invalidated key {}",collectionName );
        }

    }


    @Override
    public void evictCache() {
        indexSchemaCache.invalidateAll();
        if(logger.isDebugEnabled()){
            logger.debug( "Invalidated all keys" );
        }
    }
}
