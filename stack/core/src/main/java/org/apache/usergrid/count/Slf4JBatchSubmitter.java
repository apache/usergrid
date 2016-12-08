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
package org.apache.usergrid.count;


import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.count.common.Count;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;


/**
 * A BatchSubmitter that prints contents to the configured slf4j logger logger
 *
 * @author zznate
 */
public class Slf4JBatchSubmitter implements BatchSubmitter {

    // TODO custom logger for printing counts
    // - should be configed programatically
    private static final Logger logger = LoggerFactory.getLogger( Slf4JBatchSubmitter.class );

    private int threadCount = 3;

    private ExecutorService executor = Executors.newFixedThreadPool( threadCount );
    private final Timer addTimer =
            Metrics.newTimer( Slf4JBatchSubmitter.class, "submit_invocation", TimeUnit.MICROSECONDS, TimeUnit.SECONDS );


    @Override
    public Future submit( final Collection<Count> counts ) {
        return executor.submit( new Callable<Object>() {
            final TimerContext timer = addTimer.time();


            @Override
            public Object call() throws Exception {
                // TODO perhaps this could be pushed down further into CountProducer Impl?
                // - this would leave generic submitter class
                for ( Count c : counts ) {
                    logger.info( "found count {}", c );
                }
                timer.stop();
                return true;
            }
        } );
    }


    public void shutdown() {
        logger.warn( "Shutdown Slf4jBatchSubmitter" );
        executor.shutdown();
    }
}
