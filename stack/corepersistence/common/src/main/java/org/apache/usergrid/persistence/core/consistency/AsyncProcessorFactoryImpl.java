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
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Factory for returning and caching AsyncProcessor implementations.
 */
@Singleton
public class AsyncProcessorFactoryImpl implements AsyncProcessorFactory {

    private static final Logger LOG = LoggerFactory.getLogger( AsyncProcessorFactoryImpl.class );

    private final TimeoutQueueFactory queueFactory;
    private final ConsistencyFig consistencyFig;

    private final Map<Class<? extends Serializable>, AsyncProcessor<? extends Serializable>> instances =
            new HashMap( 100 );


    @Inject
    public AsyncProcessorFactoryImpl( final TimeoutQueueFactory queueFactory, final ConsistencyFig consistencyFig ) {
        this.queueFactory = queueFactory;
        this.consistencyFig = consistencyFig;
    }


    @Override
    public <T extends Serializable> AsyncProcessor<T> getProcessor( final Class<T> eventClass ) {

        AsyncProcessor<T> processor = ( AsyncProcessor<T> ) instances.get( eventClass );


        if ( processor != null ) {
            return processor;
        }


        synchronized ( this ) {
            processor = ( AsyncProcessor<T> ) instances.get( eventClass );


            if ( processor != null ) {
                return processor;
            }

            TimeoutQueue queue = queueFactory.getQueue( eventClass );
            AsyncProcessorImpl newProcessor = new AsyncProcessorImpl( queue, consistencyFig );

            instances.put( eventClass, newProcessor );

            newProcessor.start();

            return newProcessor;
        }
    }
}
