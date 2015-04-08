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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.utils.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.apache.usergrid.persistence.Schema.PROPERTY_APPLICATION_ID;


/**
 * Implements the org app cache for faster runtime lookups.  These values are immutable, so this LRU cache can stay
 * full for the duration of the execution
 */
@Singleton
public class ApplicationIdCacheImpl implements ApplicationIdCache {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationIdCacheImpl.class);


    /**
     * Cache the pointer to our root entity manager for reference
     */
    private final EntityManager rootEm;
    private final CpEntityManagerFactory emf;

    private final LoadingCache<String, Optional<UUID>> appCache;



    @Inject
    public ApplicationIdCacheImpl(final EntityManagerFactory emf, ApplicationIdCacheFig fig) {
        this.emf = (CpEntityManagerFactory)emf;
        this.rootEm = emf.getEntityManager(emf.getManagementAppId());
        appCache = CacheBuilder.newBuilder()
            .maximumSize(fig.getCacheSize())
            .expireAfterWrite(fig.getCacheTimeout(), TimeUnit.MILLISECONDS)
            .build(new CacheLoader<String, Optional<UUID>>() {
                @Override
                public Optional<UUID> load(final String key) throws Exception {
                    return fetchApplicationId(key);
                }
            });
    }

    @Override
    public UUID getApplicationId( final String applicationName ) {
        try {
            Optional<UUID> optionalUuid = appCache.get( applicationName.toLowerCase() );
            logger.debug("Returning for key {} value {}", applicationName, optionalUuid );
            return optionalUuid.get();
        } catch (Exception e) {
            logger.debug("Returning for key {} value null", applicationName );
            return null;
        }
    }


    /**
     * Fetch our application id
     */
    private Optional<UUID> fetchApplicationId( final String applicationName ) {

        UUID value = null;

        EntityCollectionManager ecm = emf.getManagerCache().getEntityCollectionManager(
            new ApplicationScopeImpl(
                new SimpleId( CpNamingUtils.MANAGEMENT_APPLICATION_ID, Schema.TYPE_APPLICATION ) ) );

        try {
            if ( rootEm.getApplication() == null ) {
                return Optional.empty();
            }
        } catch ( Exception e ) {
            logger.error("Error looking up management app", e);
        }

        try {

            // look up application_info ID for application using unique "name" field
            final Observable<Id> idObs = ecm.getIdField(
                CpNamingUtils.APPLICATION_INFO, new StringField(Schema.PROPERTY_NAME, applicationName));

            Id id = idObs.toBlocking().lastOrDefault(null);
            if(id != null) {
                value = id.getUuid();
                logger.debug("Loaded for key {} value {}", applicationName, value );
            }else{
                logger.debug("Could not load value for key {} ", applicationName );
            }
            return value == null ? Optional.<UUID>empty() : Optional.of(value);
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
