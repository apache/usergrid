/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.persistence.cassandra;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class QueryExecutorServiceImpl implements QueryExecutorService {


    //we set up a thread pool with 100 execution threads.  We purposefully use a synchronous queue and a caller runs so that we simply reject and immediately execute tasks if all 100 threads are occupied.



    //we make this static, we only want 1 instance of this service in the JVM.  Otherwise tests fail

    private int threadCount;

    private static ExecutorService executorService;


    @Override
    public ExecutorService getExecutor() {
        if(executorService == null){
            return getExecutorService();
        }

        return executorService;
    }


    public void setThreadCount( final int threadCount ) {
        this.threadCount = threadCount;
    }


    /**
     * Internally synchronized creation
     * @return
     */
    private synchronized ExecutorService getExecutorService(){
       if(executorService != null){
           return executorService;
       }


        executorService = new ThreadPoolExecutor( threadCount, threadCount, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>( ), new CallerRunsExecutionHandler() );
        return executorService;
    }


    /**
     * Execution handler that will run threads in the caller
     */
    private static class CallerRunsExecutionHandler implements RejectedExecutionHandler {

        private static final Logger logger = LoggerFactory.getLogger( CallerRunsExecutionHandler.class );

        @Override
        public void rejectedExecution( final Runnable r, final ThreadPoolExecutor executor ) {
            //when a task is rejected due to back pressure, we just want to run it in the caller.

            logger.warn( "Concurrent shard execution rejected the task in executor {}, running it in the caller thread", executor );

            r.run();
        }
    }

}
