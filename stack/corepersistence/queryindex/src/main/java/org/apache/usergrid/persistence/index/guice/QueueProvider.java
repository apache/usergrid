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

package org.apache.usergrid.persistence.index.guice;


import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.impl.BufferQueue;
import org.apache.usergrid.persistence.index.impl.BufferQueueInMemoryImpl;
import org.apache.usergrid.persistence.index.impl.BufferQueueSQSImpl;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.queue.QueueManagerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;


/**
 * A provider to allow users to configure their queue impl via properties
 */
@Singleton
public class QueueProvider implements Provider<BufferQueue> {

    private final IndexFig indexFig;

    private final QueueManagerFactory queueManagerFactory;
    private final MapManagerFactory mapManagerFactory;
    private final MetricsFactory metricsFactory;

    private BufferQueue bufferQueue;


    @Inject
    public QueueProvider( final IndexFig indexFig, final QueueManagerFactory queueManagerFactory,
                          final MapManagerFactory mapManagerFactory, final MetricsFactory metricsFactory ) {
        this.indexFig = indexFig;


        this.queueManagerFactory = queueManagerFactory;
        this.mapManagerFactory = mapManagerFactory;
        this.metricsFactory = metricsFactory;
    }


    @Override
    @Singleton
    public BufferQueue get() {
        if ( bufferQueue == null ) {
            bufferQueue = getQueue();
        }


        return bufferQueue;
    }


    private BufferQueue getQueue() {
        final String value = indexFig.getQueueImplementation();

        final Implementations impl = Implementations.valueOf( value );

        switch ( impl ) {
            case LOCAL:
                return new BufferQueueInMemoryImpl( indexFig );
            case SQS:
                return new BufferQueueSQSImpl( queueManagerFactory, indexFig, mapManagerFactory, metricsFactory );
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
