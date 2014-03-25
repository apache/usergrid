/*
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
 */
package org.apache.usergrid.persistence.collection.rx;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import rx.Scheduler;
import rx.schedulers.Schedulers;


public class CassandraThreadScheduler implements Provider<Scheduler> {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraThreadScheduler.class);

    private final RxFig rxFig;


    @Inject
    public CassandraThreadScheduler( final RxFig rxFig ) {
        this.rxFig = rxFig;
    }


    @Override
    @Named( "cassandraScheduler" )
    public Scheduler get() {

        //create our thread factory so we can label our threads in case we need to dump them
        final ThreadFactory factory = new ThreadFactory() {

            private final AtomicLong counter = new AtomicLong();

            @Override
            public Thread newThread( final Runnable r ) {

               final String threadName = "RxCassandraIOThreadPool-" + counter.incrementAndGet();

                LOG.debug( "Allocating new IO thread with name {}", threadName );

                Thread t = new Thread( r, threadName );
                t.setDaemon( true );
                return t;
            }
        };


        /**
         * Create a threadpool that will reclaim unused threads after 60 seconds.  
         * It uses the max thread count set here. It intentionally uses the 
         * DynamicProperty, so that when it is updated, the listener updates the 
         * pool size. Additional allocation is trivial.  Shrinking the size 
         * will require all currently executing threads to run to completion, 
         * without allowing additional tasks to be queued.
         */
        final ThreadPoolExecutor pool = new ThreadPoolExecutor( 
                0, rxFig.getMaxThreadCount(), 60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), factory, new ThreadPoolExecutor.AbortPolicy() );


        // if our max thread count is updated, we want to immediately update the pool.  
        // Per the javadoc if the size is smaller, existing threads will continue to run 
        // until they become idle and time out
        rxFig.addPropertyChangeListener( new PropertyChangeListener() {
            @Override
            public void propertyChange( final PropertyChangeEvent evt ) {
                if ( evt.getPropertyName().equals( rxFig.getKeyByMethod( "getMaxThreadCount" ) ) ) {
                    LOG.debug( "Getting update to property: rxFig.getMaxThreadCount() old = {}, new = {} ",
                            evt.getOldValue(), evt.getNewValue() );
                    pool.setMaximumPoolSize( ( Integer ) evt.getNewValue() );
                }
            }
        } );

        return Schedulers.executor( pool );
    }

}
