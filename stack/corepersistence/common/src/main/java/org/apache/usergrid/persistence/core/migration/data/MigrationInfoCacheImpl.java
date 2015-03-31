/*
 *
 *  *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *  *
 *
 */

package org.apache.usergrid.persistence.core.migration.data;


import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class MigrationInfoCacheImpl implements MigrationInfoCache{

    /**
     * Cache to cache versions temporarily
     */
    private final LoadingCache<String, Integer> versionCache = CacheBuilder.newBuilder()
            //cache the local value for 1 minute
            .expireAfterWrite( 1, TimeUnit.MINUTES ).build( new CacheLoader<String, Integer>() {
                @Override
                public Integer load( final String key ) throws Exception {
                    return migrationInfoSerialization.getVersion( key );
                }
            } );


    private final MigrationInfoSerialization migrationInfoSerialization;


    @Inject
    public MigrationInfoCacheImpl( final MigrationInfoSerialization migrationInfoSerialization ) {
        this.migrationInfoSerialization = migrationInfoSerialization;
    }


    @Override
    public void invalidateAll() {
        versionCache.invalidateAll();
    }


    @Override
    public void setVersion( final String pluginName, final int version ) {
        migrationInfoSerialization.setVersion( pluginName, version );
        versionCache.invalidate( pluginName );
    }


    @Override
    public int getVersion( final String pluginName ) {
        try {
            return versionCache.get( pluginName );
        }
        catch ( ExecutionException e ) {
            throw new RuntimeException("Unable to get cached version for plugin name " + pluginName);
        }
    }
}
