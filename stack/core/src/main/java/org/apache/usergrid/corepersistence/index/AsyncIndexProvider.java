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


import org.apache.usergrid.corepersistence.rx.impl.AllEntityIdsObservable;
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
public class AsyncIndexProvider implements Provider<AsyncReIndexService> {

    private final QueryFig queryFig;

    private final QueueManagerFactory queueManagerFactory;
    private final MetricsFactory metricsFactory;
    private final IndexService indexService;
    private final RxTaskScheduler rxTaskScheduler;
    private final AllEntityIdsObservable allEntitiesObservable;
    private final EntityCollectionManagerFactory entityCollectionManagerFactory;

    private AsyncReIndexService asyncIndexService;


    @Inject
    public AsyncIndexProvider( final QueryFig queryFig, final QueueManagerFactory queueManagerFactory,
                               final MetricsFactory metricsFactory, final IndexService indexService,
                               final RxTaskScheduler rxTaskScheduler,
                               final AllEntityIdsObservable allEntitiesObservable,
                               final EntityCollectionManagerFactory entityCollectionManagerFactory ) {
        this.queryFig = queryFig;
        this.queueManagerFactory = queueManagerFactory;
        this.metricsFactory = metricsFactory;
        this.indexService = indexService;
        this.rxTaskScheduler = rxTaskScheduler;
        this.allEntitiesObservable = allEntitiesObservable;
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
    }


    @Override
    @Singleton
    public AsyncReIndexService get() {
        if ( asyncIndexService == null ) {
            asyncIndexService = getIndexService();
        }


        return asyncIndexService;
    }


    private AsyncReIndexService getIndexService() {
        final String value = queryFig.getQueueImplementation();

        final Implementations impl = Implementations.valueOf( value );

        switch ( impl ) {
            case LOCAL:
                return new InMemoryAsyncReIndexService( indexService, rxTaskScheduler,
                    entityCollectionManagerFactory );
            case SQS:
                return new SQSAsyncReIndexService( queueManagerFactory, queryFig, metricsFactory );
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
