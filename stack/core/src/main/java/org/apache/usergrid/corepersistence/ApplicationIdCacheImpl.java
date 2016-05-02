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


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.exceptions.PersistenceException;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.StringField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * Implements the org app cache for faster runtime lookups.
 * These values are immutable, so this LRU cache can stay full for the duration of the execution.
 */
public class ApplicationIdCacheImpl implements ApplicationIdCache {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationIdCacheImpl.class);

    // cache the pointer to our root entity manager for reference
    private final LoadingCache<String, UUID> appCache;

    private final EntityManager managementEnityManager;

    private final ManagerCache managerCache;


    public ApplicationIdCacheImpl(
        final EntityManager managementEnityManager, ManagerCache managerCache, ApplicationIdCacheFig fig) {

        this.managementEnityManager = managementEnityManager;
        this.managerCache = managerCache;

        appCache = CacheBuilder.newBuilder()
            .maximumSize(fig.getCacheSize())
            .expireAfterWrite(fig.getCacheTimeout(), TimeUnit.MILLISECONDS)
            .build(new CacheLoader<String, UUID>() {
                @Override
                public UUID load(final String key) throws Exception {
                    UUID appId = fetchApplicationId(key);
                    if ( appId == null ) {
                        throw new PersistenceException("Error getting applicationId");
                    }
                    return appId;
                }
            });
    }

    @Override
    public UUID getApplicationId( final String applicationName ) {
        try {
            return appCache.get( applicationName.toLowerCase() );
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Returning for key {} value null due to exception: {}", applicationName, e);
            }
            return null;
        }
    }


    /**
     * Fetch our application id
     */
    private UUID fetchApplicationId( final String applicationName ) {

        UUID value = null;

        EntityCollectionManager ecm = managerCache.getEntityCollectionManager(
            new ApplicationScopeImpl(
                new SimpleId( CpNamingUtils.MANAGEMENT_APPLICATION_ID, Schema.TYPE_APPLICATION ) ) );

        try {
            if ( managementEnityManager.getApplication() == null ) {
                return null;
            }
        } catch ( Exception e ) {
            logger.error("Error looking up management app", e);
        }

        // look up application_info ID for application using unique "name" field
        final Observable<Id> idObs = ecm.getIdField(
            CpNamingUtils.APPLICATION_INFO, new StringField(Schema.PROPERTY_NAME, applicationName));
        Id id = idObs.toBlocking().lastOrDefault(null);

        if ( id != null ) {
            value = id.getUuid();

        } else {
            if (logger.isDebugEnabled()) {
                logger.debug( "Could not load value for key {} ", applicationName );
            }
        }

        return value;
    }


    @Override
    public void evictAppId( final String applicationName ) {
        appCache.invalidate( applicationName.toLowerCase() );
        if(logger.isDebugEnabled()) {
            logger.debug("Invalidated key {}", applicationName.toLowerCase());
        }
    }


    @Override
    public void evictAll() {
        appCache.invalidateAll();
        if(logger.isDebugEnabled()) {
            logger.debug("Invalidated all keys");
        }
    }
}
