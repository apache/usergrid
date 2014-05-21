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
package org.apache.usergrid.persistence.core.consistency;


import java.io.Serializable;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Factory for returning and caching AsyncProcessor implementations.
 */
@Singleton
public class AsyncProcessorFactoryImpl implements AsyncProcessorFactory {

    private final TimeoutQueueFactory queueFactory;
    private final ConsistencyFig consistencyFig;

    private final LoadingCache<Class<? extends Serializable>, AsyncProcessor<? extends Serializable>> loadedProcessors = CacheBuilder.newBuilder()
           .maximumSize( 1000 )
           .build( new CacheLoader<Class<? extends Serializable>, AsyncProcessor<? extends Serializable>>() {

               @Override
               public AsyncProcessor<? extends Serializable> load( final Class<? extends Serializable> key ) throws Exception {
                   final TimeoutQueue queue = queueFactory.getQueue( key );

                   return  new AsyncProcessorImpl( queue, consistencyFig );
               }
           } );


    @Inject
    public AsyncProcessorFactoryImpl( final TimeoutQueueFactory queueFactory, final ConsistencyFig consistencyFig ) {
        this.queueFactory = queueFactory;
        this.consistencyFig = consistencyFig;
    }


    @Override
    public <T extends Serializable> AsyncProcessor<T> getProcessor( final Class<T> eventClass ) {
        try {
            return ( AsyncProcessor<T> ) loadedProcessors.get( eventClass );
        }
        catch ( ExecutionException e ) {
            throw new RuntimeException( "Unable to load from cache", e );
        }
    }
}
