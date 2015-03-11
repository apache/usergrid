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

package org.apache.usergrid.corepersistence;


import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.EntityRef;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;


/**
 * Implements the org app cache for faster runtime lookups.  These values are immutable, so this LRU cache can stay
 * full for the duration of the execution
 */
public class ApplicationIdCacheImpl implements ApplicationIdCache {


    /**
     * Cache the pointer to our root entity manager for reference
     */
    private final EntityManager rootEm;

    private final LoadingCache<String, Optional<UUID>> appCache =
        CacheBuilder.newBuilder().maximumSize( 10000 ).build( new CacheLoader<String, Optional<UUID>>() {
            @Override
            public Optional<UUID> load( final String key ) throws Exception {
                return fetchApplicationId( key );
            }
        } );


    public ApplicationIdCacheImpl(final EntityManagerFactory emf) {
        this.rootEm = emf.getEntityManager( CpNamingUtils.MANAGEMENT_APPLICATION_ID);
    }

    @Override
    public Optional<UUID> getApplicationId( final String applicationName ) {
        try {
            return appCache.get( applicationName );
        }
        catch ( ExecutionException e ) {
            throw new RuntimeException( "Unable to load app cache", e );
        }
    }


    /**
     * Fetch our application id
     */
    private Optional<UUID> fetchApplicationId( final String applicationName ) {

        try {
            UUID applicationId = null;

            final EntityRef alias = rootEm.getAlias( CpNamingUtils.APPLICATION_INFO, applicationName );
            if ( alias != null ) {
                Entity entity = rootEm.get(alias);
                applicationId = (UUID) entity.getProperty("uuid");
            }

            return Optional.fromNullable( applicationId );
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Unable to retrieve application id", e );
        }
    }


    @Override
    public void evictAppId( final String applicationName ) {
        appCache.invalidate( applicationName );
    }


    @Override
    public void evictAll() {
        appCache.invalidateAll();
    }
}
