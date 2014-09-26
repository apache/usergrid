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
package org.apache.usergrid.persistence.core.task;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;


/**
 * Implementation of the task executor with a unique name and size
 */
public class NamedTaskExecutorImpl implements TaskExecutor {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( NamedTaskExecutorImpl.class );

    private final NamedForkJoinPool executorService;

    private final String name;
    private final int poolSize;


    /**
     * @param name The name of this instance of the task executor
     * @param poolSize The size of the pool.  This is the number of concurrent tasks that can execute at once.
     */
    public NamedTaskExecutorImpl( final String name, final int poolSize ) {
        Preconditions.checkNotNull( name );
        Preconditions.checkArgument( name.length() > 0, "name must have a length" );
        Preconditions.checkArgument( poolSize > 0, "poolSize must be > than 0" );

        this.name = name;
        this.poolSize = poolSize;

        this.executorService = new NamedForkJoinPool( poolSize );
    }


    @Override
    public <V, I> Task<V, I> submit( final Task<V, I> task ) {

        try {
            executorService.submit( task );
        }
        catch ( RejectedExecutionException ree ) {
            task.rejected();
        }

        return task;
    }


    private final class NamedForkJoinPool extends ForkJoinPool {

        private NamedForkJoinPool( final int workerThreadCount ) {
            //TODO, verify the scheduler at the end
            super( workerThreadCount, defaultForkJoinWorkerThreadFactory, new TaskExceptionHandler(), true );
        }
    }


    private final class TaskExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException( final Thread t, final Throwable e ) {
            LOG.error( "Uncaught exception on thread {} was {}", t, e );
        }
    }
}
