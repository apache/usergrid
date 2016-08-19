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
package org.apache.usergrid.persistence.queue.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.queue.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * manages whether we take in an external in memory override for queues.
 */
@Singleton
public class QueueManagerFactoryImpl implements QueueManagerFactory {

    private static final Logger logger = LoggerFactory.getLogger( QueueManagerFactoryImpl.class );

    private final QueueFig queueFig;
    private final QueueManagerInternalFactory queuemanagerInternalFactory;
    private final Map<String,QueueManager> defaultManager;
    private final LoadingCache<QueueScope, QueueManager> queueManager =
        CacheBuilder
            .newBuilder()
            .initialCapacity(5)
            .maximumSize(100)
            .build(new CacheLoader<QueueScope, QueueManager>() {

                @Override
                public QueueManager load( QueueScope scope ) throws Exception {

                    if ( queueFig.overrideQueueForDefault() ){

                        QueueManager manager = defaultManager.get( scope.getName() );
                        if ( manager == null ) {
                            manager = new LocalQueueManager();
                            defaultManager.put( scope.getName(), manager );
                        }
                        return manager;

                    } else {
                        return queuemanagerInternalFactory.getQueueManager(scope);
                    }

                }
            });

    @Inject
    public QueueManagerFactoryImpl(final QueueFig queueFig, final QueueManagerInternalFactory queuemanagerInternalFactory){
        this.queueFig = queueFig;
        this.queuemanagerInternalFactory = queuemanagerInternalFactory;
        this.defaultManager = new HashMap<>(10);
    }

    @Override
    public QueueManager getQueueManager(QueueScope scope) {

        try {
            return queueManager.get(scope);

        } catch (ExecutionException e) {

            logger.error("Unable to load or retrieve queue manager from cache for queue {}", scope.getName());
            throw new RuntimeException(e);
        }

    }
}
