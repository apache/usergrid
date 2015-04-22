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

package org.apache.usergrid.corepersistence.index;


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
public class AsyncIndexProvider implements Provider<AsyncIndexService> {

    private final IndexProcessorFig indexProcessorFig;

    private final QueueManagerFactory queueManagerFactory;
    private final MetricsFactory metricsFactory;
    private final IndexService indexService;
    private final RxTaskScheduler rxTaskScheduler;
    private final EntityCollectionManagerFactory entityCollectionManagerFactory;

    private AsyncIndexService asyncIndexService;


    @Inject
    public AsyncIndexProvider( final IndexProcessorFig indexProcessorFig, final QueueManagerFactory queueManagerFactory,
                               final MetricsFactory metricsFactory, final IndexService indexService,
                               final RxTaskScheduler rxTaskScheduler,
                               final EntityCollectionManagerFactory entityCollectionManagerFactory ) {
        this.indexProcessorFig = indexProcessorFig;
        this.queueManagerFactory = queueManagerFactory;
        this.metricsFactory = metricsFactory;
        this.indexService = indexService;
        this.rxTaskScheduler = rxTaskScheduler;
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
    }


    @Override
    @Singleton
    public AsyncIndexService get() {
        if ( asyncIndexService == null ) {
            asyncIndexService = getIndexService();
        }


        return asyncIndexService;
    }


    private AsyncIndexService getIndexService() {
        final String value = indexProcessorFig.getQueueImplementation();

        final Implementations impl = Implementations.valueOf( value );

        switch ( impl ) {
            case LOCAL:
                return new InMemoryAsyncIndexService( indexService, rxTaskScheduler,
                    entityCollectionManagerFactory );
            case SQS:
                return new SQSAsyncIndexService( queueManagerFactory, indexProcessorFig, metricsFactory, indexService,
                    entityCollectionManagerFactory, rxTaskScheduler );
            default:
                throw new IllegalArgumentException( "Configuration value of " + getErrorValues() + " are allowed" );
        }
    }


    private String getErrorValues() {
        String values = "";

        for ( final Implementations impl : Implementations.values() ) {
            values += impl + ", ";
        }

        values = values.substring( 0, values.length() - 2 );

        return values;
    }


    /**
     * Different implementations
     */
    public static enum Implementations {
        LOCAL,
        SQS;


        public String asString() {
            return toString();
        }
    }
}
