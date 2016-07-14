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


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.map.MapManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.map.MapManagerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


@Singleton
public class CollectionSettingsFactory {

    private final LoadingCache<CollectionSettingsScope,CollectionSettings> indexSchemaCache;

    @Inject
    public CollectionSettingsFactory( final CollectionSettingsCacheFig fig,
                                      final MapManagerFactory mapManagerFactory,
                                      final CollectionSettingsCache collectionSettingsCache ){


       indexSchemaCache  = CacheBuilder.newBuilder()
            .maximumSize( fig.getCacheSize() )
            .expireAfterWrite( fig.getCacheTimeout(), TimeUnit.MILLISECONDS )
            .build( new CacheLoader<CollectionSettingsScope, CollectionSettings>() {
                @Override
                public CollectionSettings load( CollectionSettingsScope scope ) throws Exception {

                    final MapManager mm = mapManagerFactory
                        .createMapManager( CpNamingUtils.getEntityTypeMapScope(scope.getApplication()));
                    return new CollectionSettingsImpl( scope, mm, collectionSettingsCache ) ;
                }
            } );
    }


    public CollectionSettings getInstance( CollectionSettingsScope scope ) {


        try {
            return indexSchemaCache.get(scope);
        }catch (ExecutionException e){
            throw new RuntimeException(e);
        }

    }


}
