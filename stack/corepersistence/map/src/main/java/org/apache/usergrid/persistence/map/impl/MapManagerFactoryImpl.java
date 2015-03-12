/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.map.impl;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Execution;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.MapScope;

import java.util.concurrent.ExecutionException;

/**
 * Returns map managers, built to handle caching
 */
@Singleton
public class MapManagerFactoryImpl implements MapManagerFactory {
    private final MapSerialization mapSerialization;
    private LoadingCache<MapScope, MapManager> mmCache =
        CacheBuilder.newBuilder().maximumSize( 1000 ).build( new CacheLoader<MapScope, MapManager>() {
            public MapManager load( MapScope scope ) {
                return  new MapManagerImpl(scope,mapSerialization);
            }
        } );

    @Inject
    public MapManagerFactoryImpl(final MapSerialization mapSerialization){

        this.mapSerialization = mapSerialization;
    }

    @Override
    public MapManager createMapManager(MapScope scope) {
        Preconditions.checkNotNull(scope);
        try{
            return mmCache.get(scope);
        }catch (ExecutionException ee){
            throw new RuntimeException(ee);
        }
    }

    @Override
    public void invalidate() {
        mmCache.invalidateAll();
    }
}
