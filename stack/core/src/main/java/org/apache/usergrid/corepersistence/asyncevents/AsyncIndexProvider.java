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

package org.apache.usergrid.corepersistence.asyncevents;


import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactory;
import org.apache.usergrid.corepersistence.index.IndexProcessorFig;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.impl.IndexProducer;
import org.apache.usergrid.persistence.queue.LegacyQueueManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.queue.LegacyQueueFig;
import org.apache.usergrid.persistence.queue.LegacyQueueManagerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import static org.apache.usergrid.persistence.queue.LegacyQueueManager.Implementation.LOCAL;


/**
 * A provider to allow users to configure their queue impl via properties
 */
@Singleton
public class AsyncIndexProvider implements Provider<AsyncEventService> {

    private final IndexProcessorFig indexProcessorFig;

    private final LegacyQueueManagerFactory queueManagerFactory;
    private final MetricsFactory metricsFactory;
    private final RxTaskScheduler rxTaskScheduler;
    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final EventBuilder eventBuilder;
    private final IndexLocationStrategyFactory indexLocationStrategyFactory;
    private final EntityIndexFactory entityIndexFactory;
    private final IndexProducer indexProducer;
    private final MapManagerFactory mapManagerFactory;
    private final LegacyQueueFig queueFig;

    private AsyncEventService asyncEventService;


    @Inject
    public AsyncIndexProvider(final IndexProcessorFig indexProcessorFig,
                              final LegacyQueueManagerFactory queueManagerFactory,
                              final MetricsFactory metricsFactory,
                              @EventExecutionScheduler final RxTaskScheduler rxTaskScheduler,
                              final EntityCollectionManagerFactory entityCollectionManagerFactory,
                              final EventBuilder eventBuilder,
                              final IndexLocationStrategyFactory indexLocationStrategyFactory,
                              final EntityIndexFactory entityIndexFactory,
                              final IndexProducer indexProducer,
                              final MapManagerFactory mapManagerFactory,
                              final LegacyQueueFig queueFig) {

        this.indexProcessorFig = indexProcessorFig;
        this.queueManagerFactory = queueManagerFactory;
        this.metricsFactory = metricsFactory;
        this.rxTaskScheduler = rxTaskScheduler;
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.eventBuilder = eventBuilder;
        this.indexLocationStrategyFactory = indexLocationStrategyFactory;
        this.entityIndexFactory = entityIndexFactory;
        this.indexProducer = indexProducer;
        this.mapManagerFactory = mapManagerFactory;
        this.queueFig = queueFig;
    }


    @Override
    @Singleton
    public AsyncEventService get() {
        if (asyncEventService == null) {
            asyncEventService = getIndexService();
        }

        return asyncEventService;
    }


    private AsyncEventService getIndexService() {
        final String value = indexProcessorFig.getQueueImplementation();

        final LegacyQueueManager.Implementation impl = LegacyQueueManager.Implementation.valueOf(value);

        final AsyncEventServiceImpl asyncEventService = new AsyncEventServiceImpl(
            queueManagerFactory,
            indexProcessorFig,
            indexProducer,
            metricsFactory,
            entityCollectionManagerFactory,
            indexLocationStrategyFactory,
            entityIndexFactory,
            eventBuilder,
            mapManagerFactory,
            queueFig,
            rxTaskScheduler );

        if ( impl.equals( LOCAL )) {
            asyncEventService.MAX_TAKE = 1000;
        }

        return asyncEventService;
    }
}
