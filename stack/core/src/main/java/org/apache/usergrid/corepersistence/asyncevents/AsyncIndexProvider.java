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
import org.apache.usergrid.persistence.queue.LocalQueueManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.queue.QueueFig;
import org.apache.usergrid.persistence.queue.QueueManagerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;


/**
 * A provider to allow users to configure their queue impl via properties
 */
@Singleton
public class AsyncIndexProvider implements Provider<AsyncEventService> {

    private final IndexProcessorFig indexProcessorFig;

    private final QueueManagerFactory queueManagerFactory;
    private final MetricsFactory metricsFactory;
    private final RxTaskScheduler rxTaskScheduler;
    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final EventBuilder eventBuilder;
    private final IndexLocationStrategyFactory indexLocationStrategyFactory;
    private final EntityIndexFactory entityIndexFactory;
    private final IndexProducer indexProducer;
    private final MapManagerFactory mapManagerFactory;
    private final QueueFig queueFig;

    private AsyncEventService asyncEventService;


    @Inject
    public AsyncIndexProvider(final IndexProcessorFig indexProcessorFig,
                              final QueueManagerFactory queueManagerFactory,
                              final MetricsFactory metricsFactory,
                              final RxTaskScheduler rxTaskScheduler,
                              final EntityCollectionManagerFactory entityCollectionManagerFactory,
                              final EventBuilder eventBuilder,
                              final IndexLocationStrategyFactory indexLocationStrategyFactory,
                              final EntityIndexFactory entityIndexFactory,
                              final IndexProducer indexProducer,
                              final MapManagerFactory mapManagerFactory,
                              final QueueFig queueFig) {

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

        final Implementations impl = Implementations.valueOf(value);

        switch (impl) {
            case LOCAL:
                AmazonAsyncEventService eventService = new AmazonAsyncEventService(scope -> new LocalQueueManager(), indexProcessorFig, indexProducer, metricsFactory,
                    entityCollectionManagerFactory, indexLocationStrategyFactory, entityIndexFactory, eventBuilder,mapManagerFactory, queueFig,rxTaskScheduler);
                eventService.MAX_TAKE = 1000;
                return eventService;
            case SQS:
                throw new IllegalArgumentException("Configuration value of SQS is no longer allowed. Use SNS instead with only a single region");
            case SNS:
                return new AmazonAsyncEventService(queueManagerFactory, indexProcessorFig, indexProducer, metricsFactory,
                    entityCollectionManagerFactory, indexLocationStrategyFactory,entityIndexFactory, eventBuilder, mapManagerFactory, queueFig, rxTaskScheduler );
            default:
                throw new IllegalArgumentException("Configuration value of " + getErrorValues() + " are allowed");
        }
    }


    private String getErrorValues() {
        String values = "";

        for (final Implementations impl : Implementations.values()) {
            values += impl + ", ";
        }

        values = values.substring(0, values.length() - 2);

        return values;
    }


    /**
     * Different implementations
     */
    public static enum Implementations { //TODO see about removing SNS and SQS and use AMZN? - michaelarusso
        TEST,
        LOCAL,
        SQS,
        SNS;


        public String asString() {
            return toString();
        }
    }
}
