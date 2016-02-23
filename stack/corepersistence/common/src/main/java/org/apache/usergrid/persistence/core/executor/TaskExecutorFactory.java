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
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A task executor that allows you to submit tasks
 */
public class TaskExecutorFactory {

    private static final Logger logger = LoggerFactory.getLogger( TaskExecutorFactory.class );


    public enum RejectionAction {
        /**
         * If there is no capacity left, throw an exception
         */
        ABORT,
        /**
         * If there is no capacity left, the caller runs the callable
         */
        CALLERRUNS,

        /**
         * If there is no capacity left, the request is logged and then silently dropped
         */
        DROP
    }


    /**
     * Create a task executor
     */
    public static ThreadPoolExecutor createTaskExecutor( final String schedulerName, final int maxThreadCount,
                                                         final int maxQueueSize, RejectionAction rejectionAction ) {


        final BlockingQueue<Runnable> queue;

        if(maxQueueSize == 0){
            queue = new SynchronousQueue();
        }else{
            queue = new ArrayBlockingQueue<>( maxQueueSize );
        }


        if ( rejectionAction == RejectionAction.ABORT ) {
            return new MaxSizeThreadPool( queue, schedulerName, maxThreadCount );
        }
        else if ( rejectionAction == RejectionAction.CALLERRUNS ) {

            return new MaxSizeThreadPoolCallerRuns( queue, schedulerName, maxThreadCount );
        }
        else if ( rejectionAction == RejectionAction.DROP ) {
            return new MaxSizeThreadPoolDrops( queue, schedulerName, maxThreadCount );
        }
        else {
            throw new IllegalArgumentException( "Unable to create a scheduler with the arguments provided" );
        }
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
     * Create a thread pool that will implement CallerRunsPolicy if our tasks become overwhelmed
     */
    private static final class MaxSizeThreadPoolCallerRuns extends ThreadPoolExecutor {

        public MaxSizeThreadPoolCallerRuns( final BlockingQueue<Runnable> queue, final String poolName,
                                            final int maxPoolSize ) {
            super( maxPoolSize, maxPoolSize, 30, TimeUnit.SECONDS, queue, new CountingThreadFactory( poolName ),
                new CallerRunsHandler( poolName ) );
        }
    }


    /**
     * Create a thread pool that will implement CallerRunsPolicy if our tasks become overwhelmed
     */
    private static final class MaxSizeThreadPoolDrops extends ThreadPoolExecutor {

        public MaxSizeThreadPoolDrops( final BlockingQueue<Runnable> queue, final String poolName,
                                       final int maxPoolSize ) {
            super( maxPoolSize, maxPoolSize, 30, TimeUnit.SECONDS, queue, new CountingThreadFactory( poolName ),
                new DropHandler( poolName ) );
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


    /**
     * The handler that will handle rejected executions and signal the interface
     */
    private static final class CallerRunsHandler implements RejectedExecutionHandler {

        private final String poolName;


        private CallerRunsHandler( final String poolName ) {this.poolName = poolName;}


        @Override
        public void rejectedExecution( final Runnable r, final ThreadPoolExecutor executor ) {
            logger.warn( "{} task queue full, rejecting task {} and running in thread {}", poolName, r,
                Thread.currentThread().getName() );

            //We've decided we want to have a "caller runs" policy, to just invoke the task when rejected

            r.run();
        }
    }


    /**
     * The handler that will handle rejected executions and signal the interface
     */
    private static final class DropHandler implements RejectedExecutionHandler {

        private final String poolName;


        private DropHandler( final String poolName ) {this.poolName = poolName;}


        @Override
        public void rejectedExecution( final Runnable r, final ThreadPoolExecutor executor ) {
            logger.warn( "{} task queue full, dropping task {}", poolName, r );
        }
    }
}
