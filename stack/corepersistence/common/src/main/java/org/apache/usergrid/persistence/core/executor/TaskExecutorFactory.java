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

package org.apache.usergrid.persistence.core.executor;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


/**
 * A task executor that allows you to submit tasks
 */
public class TaskExecutorFactory {


    /**
     * Create a task executor
     * @param schedulerName
     * @param maxThreadCount
     * @param maxQueueSize
     * @return
     */
    public static ThreadPoolExecutor createTaskExecutor( final String schedulerName, final int maxThreadCount,
                                                         final int maxQueueSize ) {


        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>( maxQueueSize );


        final MaxSizeThreadPool threadPool = new MaxSizeThreadPool( queue, schedulerName, maxThreadCount );


        return threadPool;
    }


    /**
     * Create a thread pool that will reject work if our audit tasks become overwhelmed
     */
    private static final class MaxSizeThreadPool extends ThreadPoolExecutor {

        public MaxSizeThreadPool( final BlockingQueue<Runnable> queue, final String poolName, final int maxPoolSize ) {
            super( maxPoolSize, maxPoolSize, 30, TimeUnit.SECONDS, queue, new CountingThreadFactory( poolName ) );
        }
    }


    /**
     * Thread factory that will name and count threads for easier debugging
     */
    private static final class CountingThreadFactory implements ThreadFactory {

        private final AtomicLong threadCounter = new AtomicLong();
        private final String poolName;


        private CountingThreadFactory( final String poolName ) {this.poolName = poolName;}


        @Override
        public Thread newThread( final Runnable r ) {
            final long newValue = threadCounter.incrementAndGet();

            final String threadName = poolName + "-" + newValue;

            Thread t = new Thread( r, threadName );

            //set it to be a daemon thread so it doesn't block shutdown
            t.setDaemon( true );

            return t;
        }
    }
}
