/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.usergrid.persistence.index.impl;


import java.util.concurrent.atomic.AtomicInteger;

import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.transport.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.index.IndexFig;


/**
 * Monitors failures
 */
public class FailureMonitorImpl implements FailureMonitor {

    private static final Logger LOG = LoggerFactory.getLogger( FailureMonitorImpl.class );

    /**
     * Exceptions that will cause us to increment our count and restart
     */
    private static final Class[] RESTART_EXCEPTIONS =
            new Class[] { TransportException.class, ClusterBlockException.class };

    /**
     * Number of consecutive failures when connecting to Elastic Search
     */
    private AtomicInteger failCounter = new AtomicInteger();

    private final IndexFig indexFig;
    private final EsProvider esProvider;


    public FailureMonitorImpl( final IndexFig indexFig, final EsProvider esProvider ) {
        this.indexFig = indexFig;
        this.esProvider = esProvider;
    }


    @Override
    public void fail( final String message, final Throwable throwable ) {

        /**
         * Not a network exception we support restart clients on, abort
         */
        if ( !isNetworkException( throwable ) ) {
            return;
        }

        final int fails = failCounter.incrementAndGet();
        final int maxCount = indexFig.getFailRefreshCount();

        if ( fails > maxCount ) {
            LOG.error( "Unable to connect to elasticsearch.  Reason is {}", message, throwable );
            LOG.warn( "We have failed to connect to Elastic Search {} times.  "
                    + "Max allowed is {}.  Resetting connection", fails, maxCount );

            esProvider.releaseClient();
        }
    }


    private boolean isNetworkException( final Throwable throwable ) {
        for ( Class<?> clazz : RESTART_EXCEPTIONS ) {
            if ( clazz.isInstance( throwable ) ) {
                return true;
            }
        }

        return false;
    }


    @Override
    public void success() {
        failCounter.set( 0 );
    }
}
