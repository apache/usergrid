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

package org.apache.usergrid.persistence.core.rx;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Scheduler;
import rx.schedulers.Schedulers;


/**
 * An implementation of the task scheduler that allows us to control the number of I/O threads
 */
@Singleton
public class RxTaskSchedulerImpl implements RxTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger( RxTaskSchedulerImpl.class );

    private final Scheduler scheduler;
    private final String poolName;

    @Inject
    public RxTaskSchedulerImpl(final RxSchedulerFig schedulerFig){

        this.poolName = schedulerFig.getIoSchedulerName();

        final int poolSize = schedulerFig.getMaxIoThreads();


        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(poolSize);


        final MaxSizeThreadPool threadPool = new MaxSizeThreadPool( queue, poolSize );


        this.scheduler = Schedulers.from(threadPool);


    }


    @Override
    public Scheduler getAsyncIOScheduler() {
        return scheduler;
    }


    /**
     * Create a thread pool that will reject work if our audit tasks become overwhelmed
     */
    private final class MaxSizeThreadPool extends ThreadPoolExecutor {

        public MaxSizeThreadPool( final BlockingQueue<Runnable> queue, final int maxPoolSize ) {

            super( maxPoolSize, maxPoolSize, 30, TimeUnit.SECONDS, queue, new CountingThreadFactory( ),  new RejectedHandler() );
        }
    }


    /**
     * Thread factory that will name and count threads for easier debugging
     */
    private final class CountingThreadFactory implements ThreadFactory {

        private final AtomicLong threadCounter = new AtomicLong();


        @Override
        public Thread newThread( final Runnable r ) {
            final long newValue = threadCounter.incrementAndGet();

            final String threadName = poolName + "-" + newValue;

            Thread t = new Thread( r, threadName  );

            //set it to be a daemon thread so it doesn't block shutdown
            t.setDaemon( true );

            return t;
        }
    }


    /**
     * The handler that will handle rejected executions and signal the interface
     */
    private final class RejectedHandler implements RejectedExecutionHandler {


        @Override
        public void rejectedExecution( final Runnable r, final ThreadPoolExecutor executor ) {
            log.warn( "{} task queue full, rejecting task {} and running in thread {}", poolName, r, Thread.currentThread().getName() );

            //We've decided we want to have a "caller runs" policy, to just invoke the task when rejected

            r.run();
        }

    }
}
