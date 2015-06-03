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


import org.apache.usergrid.corepersistence.index.IndexProcessorFig;
import org.apache.usergrid.corepersistence.index.IndexService;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
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
    private final IndexService indexService;
    private final RxTaskScheduler rxTaskScheduler;
    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final EventBuilder eventBuilder;

    private AsyncEventService asyncEventService;


    @Inject
    public AsyncIndexProvider(final IndexProcessorFig indexProcessorFig,
                              final QueueManagerFactory queueManagerFactory,
                              final MetricsFactory metricsFactory,
                              final IndexService indexService,
                              final RxTaskScheduler rxTaskScheduler,
                              final EntityCollectionManagerFactory entityCollectionManagerFactory,
                              final EventBuilder eventBuilder) {

        this.indexProcessorFig = indexProcessorFig;
        this.queueManagerFactory = queueManagerFactory;
        this.metricsFactory = metricsFactory;
        this.indexService = indexService;
        this.rxTaskScheduler = rxTaskScheduler;
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.eventBuilder = eventBuilder;
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
                return new InMemoryAsyncEventService(eventBuilder, rxTaskScheduler, indexProcessorFig.resolveSynchronously());
            case SQS:
                return new AmazonAsyncEventService(queueManagerFactory, indexProcessorFig, metricsFactory, indexService,
                    entityCollectionManagerFactory, rxTaskScheduler);
            case SNS:
                return new AmazonAsyncEventService(queueManagerFactory, indexProcessorFig, metricsFactory, indexService,
                    entityCollectionManagerFactory, rxTaskScheduler);
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
    public static enum Implementations {
        TEST,
        LOCAL,
        SQS,
        SNS;


        public String asString() {
            return toString();
        }
    }
}
