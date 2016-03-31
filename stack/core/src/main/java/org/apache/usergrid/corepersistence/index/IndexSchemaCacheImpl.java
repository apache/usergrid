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


import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.map.MapManager;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Singleton;


/**
 * Cache the calls to update and get the map manager so we don't overload cassandra when we update
 * after each call?
 */
@Singleton
public class IndexSchemaCacheImpl implements IndexSchemaCache {
    private static final Logger logger = LoggerFactory.getLogger(IndexSchemaCacheImpl.class );

    private final LoadingCache<String,Optional<String>> indexSchemaCache;
    private final MapManager mapManager;


    public IndexSchemaCacheImpl(MapManager mapManager,IndexSchemaCacheFig indexSchemaCacheFig){
        this.mapManager = mapManager;
        indexSchemaCache = CacheBuilder.newBuilder()
            .maximumSize( indexSchemaCacheFig.getCacheSize() )
            //.expireAfterWrite( indexSchemaCacheFig.getCacheTimeout(), TimeUnit.MILLISECONDS ) <-- I don't think we want this to expire that quickly.
            .build( new CacheLoader<String, Optional<String>>() {
                @Override
                public Optional<String> load( final String collectionName ) throws Exception {
                    return Optional.ofNullable( retrieveCollectionSchema( collectionName ) );
                }
            } );
    }

    private String retrieveCollectionSchema( final String collectionName ){
        return mapManager.getString( collectionName );
    }


    @Override
    public Optional<String> getCollectionSchema( final String collectionName ) {
        try {
            Optional<String> optionalCollectionSchema = indexSchemaCache.get( collectionName );
            if(!optionalCollectionSchema.isPresent()){
                indexSchemaCache.invalidate( collectionName );
                return Optional.empty();
            }
            return optionalCollectionSchema;
        }catch(Exception e){
            if(logger.isDebugEnabled()){
                logger.debug( "Returning for collection name: {} "
                    + "resulted in the following failure: {}",collectionName,e );
            }
        }
        return null;
    }


    @Override
    public void evictCollectionSchema( final String collectionName ) {
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
