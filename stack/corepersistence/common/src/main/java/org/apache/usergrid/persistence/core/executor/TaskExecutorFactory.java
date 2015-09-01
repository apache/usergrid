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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;


/**
 * A task executor that allows you to submit tasks
 */
public class TaskExecutorFactory {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutorFactory.class);

    public enum RejectionAction {
        ABORT,
        CALLERRUNS
    }
    /**
     * Create a task executor
     * @param schedulerName
     * @param maxThreadCount
     * @param maxQueueSize
     * @return
     */
    public static ThreadPoolExecutor createTaskExecutor( final String schedulerName, final int maxThreadCount,
                                                         final int maxQueueSize, RejectionAction rejectionAction ) {


        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>( maxQueueSize );


        if(rejectionAction.equals(RejectionAction.ABORT)){

            return new MaxSizeThreadPool( queue, schedulerName, maxThreadCount );

        }
        else if(rejectionAction.equals(RejectionAction.CALLERRUNS)){

            return new MaxSizeThreadPoolCallerRuns( queue, schedulerName, maxThreadCount );

        }else{
            //default to the thread pool with ABORT policy
            return new MaxSizeThreadPool( queue, schedulerName, maxThreadCount );
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

        public MaxSizeThreadPoolCallerRuns( final BlockingQueue<Runnable> queue, final String poolName, final int maxPoolSize ) {
            super( maxPoolSize, maxPoolSize, 30, TimeUnit.SECONDS, queue,
                new CountingThreadFactory( poolName ), new RejectedHandler(poolName) );
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
            t.setDaemon(true);

            return t;
        }
    }

    /**
     * The handler that will handle rejected executions and signal the interface
     */
    private static final class RejectedHandler implements RejectedExecutionHandler {

        private final String poolName;

        private RejectedHandler (final String poolName) {this.poolName = poolName;}

        @Override
        public void rejectedExecution( final Runnable r, final ThreadPoolExecutor executor ) {
            log.warn( "{} task queue full, rejecting task {} and running in thread {}", poolName, r, Thread.currentThread().getName() );

            //We've decided we want to have a "caller runs" policy, to just invoke the task when rejected

            r.run();
        }

    }
}
