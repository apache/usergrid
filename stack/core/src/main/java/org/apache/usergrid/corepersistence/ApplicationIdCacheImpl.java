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
import org.apache.usergrid.persistence.*;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.utils.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implements the org app cache for faster runtime lookups.  These values are immutable, so this LRU cache can stay
 * full for the duration of the execution
 */
public class ApplicationIdCacheImpl implements ApplicationIdCache {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationIdCacheImpl.class);


    /**
     * Cache the pointer to our root entity manager for reference
     */
    private final EntityManager rootEm;

    private final LoadingCache<String, UUID> appCache =
        CacheBuilder.newBuilder().maximumSize( 10000 ).build( new CacheLoader<String, UUID>() {
            @Override
            public UUID load( final String key ) throws Exception {
                return fetchApplicationId( key );
            }
        } );


    public ApplicationIdCacheImpl(final EntityManagerFactory emf) {
        this.rootEm = emf.getEntityManager( emf.getManagementAppId());
    }

    @Override
    public UUID getApplicationId( final String applicationName ) {
        try {
            UUID optionalUuid = appCache.get( applicationName.toLowerCase() );
            logger.debug("Returning for key {} value {}", applicationName, optionalUuid );
            return optionalUuid;
        }
        catch ( Exception e ) {
            logger.debug("Returning for key {} value null", applicationName );
            return null;
        }
    }


    /**
     * Fetch our application id
     */
    private UUID fetchApplicationId( final String applicationName ) {

        UUID value = null;

        try {
            if ( rootEm.getApplication() == null ) {
                return null;
            }
        } catch ( Exception e ) {
            logger.error("Error looking up app", e);
        }

        try {
            Query q = Query.fromQL( Schema.PROPERTY_NAME + " = '" + applicationName.toLowerCase() + "'" );

            Results results = rootEm.searchCollection(
                rootEm.getApplicationRef(), CpNamingUtils.APPLICATION_INFOS, q);

            if ( !results.isEmpty() ) {

                Entity entity = results.iterator().next();
                Object uuidObject = entity.getProperty(Schema.PROPERTY_APPLICATION_ID);

                if (uuidObject instanceof UUID) {
                    value = (UUID) uuidObject;
                } else {
                    value = UUIDUtils.tryExtractUUID(
                        entity.getProperty(Schema.PROPERTY_APPLICATION_ID).toString());
                }

            }

            logger.debug("Loaded    for key {} value {}", applicationName, value );
            return value;
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Unable to retrieve application id", e );
        }
    }


    @Override
    public void evictAppId( final String applicationName ) {
        appCache.invalidate( applicationName.toLowerCase() );
        logger.debug("Invalidated key {}", applicationName.toLowerCase());
    }


    @Override
    public void evictAll() {
        appCache.invalidateAll();
        logger.debug("Invalidated all keys");
    }
}
