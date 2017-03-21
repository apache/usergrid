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
package org.apache.usergrid.services.notifications;

import com.google.common.cache.*;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.queue.LegacyQueueManager;
import org.apache.usergrid.services.notifications.impl.ApplicationQueueManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


@Singleton
public class ApplicationQueueManagerCache{

    private static final Logger logger = LoggerFactory.getLogger( ApplicationQueueManagerCache.class );


    private final Cache<UUID, ApplicationQueueManager> cache;

    private static final String CACHE_TTL_PROP = "usergrid.push.queuemanager.cache.time-to-live";
    private static final String CACHE_MAX_SIZE_PROP = "usergrid.push.queuemanager.cache.size";

    public ApplicationQueueManagerCache(){

        // set a smaller ttl
        long ttl = 10;
        int configuredMaxSize;

        try{
            ttl = Integer.parseInt(System.getProperty(CACHE_TTL_PROP));
        } catch (NumberFormatException e){
            // already defaulted to 1 above
        }

        try{
            configuredMaxSize = Integer.parseInt(System.getProperty(CACHE_MAX_SIZE_PROP));
        } catch (NumberFormatException e){
            configuredMaxSize = 200;
        }

        this.cache = CacheBuilder.newBuilder()
            .maximumSize(Math.min(1000,configuredMaxSize))
            .expireAfterAccess(ttl, TimeUnit.MINUTES)
            .removalListener(new RemovalListener<UUID, ApplicationQueueManager>() {
                @Override
                public void onRemoval(
                    RemovalNotification<UUID, ApplicationQueueManager> queueManagerNotifiication) {
                    try {
                        if ( queueManagerNotifiication.getValue() != null) {
                            queueManagerNotifiication.getValue().stop();
                        }
                    } catch (Exception ie) {
                        logger.error("Failed to shutdown push queue manager from cache", ie.getMessage());
                    }
                }
            }).build();

    }

    public void put(UUID key, ApplicationQueueManager value){

        cache.put(key, value);
    }

    public ConcurrentMap<UUID, ApplicationQueueManager> asMap(){

        return cache.asMap();
    }

    public ApplicationQueueManager get(UUID key){
        return cache.getIfPresent(key);
    }

    public void invalidate(UUID key){
        cache.invalidate(key);
    }

    public void invalidateAll(){
        cache.invalidateAll();
    }


    public ApplicationQueueManager getApplicationQueueManager( final EntityManager entityManager,
                                                               final LegacyQueueManager legacyQueueManager,
                                                               final JobScheduler jobScheduler,
                                                               final MetricsFactory metricsService,
                                                               final Properties properties ) {


        ApplicationQueueManager manager = cache.getIfPresent(entityManager.getApplicationId());

        if(manager != null){
            if(logger.isTraceEnabled()){
                logger.trace("Returning push queue manager from cache for application: {}", entityManager.getApplicationId());
            }
            return manager;

        }else {
            if(logger.isTraceEnabled()) {
                logger.trace("Push queue manager not found in cache, loading for application: {}", entityManager.getApplicationId());
            }
            manager = new ApplicationQueueManagerImpl(
                jobScheduler,
                entityManager,
                legacyQueueManager,
                metricsService,
                properties
            );

            cache.put(entityManager.getApplicationId(), manager);

            return manager;


        }


    }


}
