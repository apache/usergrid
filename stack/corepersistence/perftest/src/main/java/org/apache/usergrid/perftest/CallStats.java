/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.usergrid.perftest;


import com.google.inject.Inject;
import org.apache.usergrid.perftest.rest.CallStatsSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Atomically stores and updates call statistics on tests.
 */
public class CallStats {
    private static final Logger LOG = LoggerFactory.getLogger( CallStats.class );

    private final AtomicInteger callCount = new AtomicInteger();
    private final Object lock = new Object();
    private final TimeUnit units = TimeUnit.NANOSECONDS;
    private ResultsLog log;

    private long maxTime = Long.MIN_VALUE;
    private long minTime = Long.MAX_VALUE;
    private long meanTime = 0;
    private long totalTime = 0;


    @Inject
    public void setResultsLog( ResultsLog log ) {
        this.log = log;
        try {
            log.open();
        }
        catch ( IOException e ) {
            LOG.error( "Failed to open the results log.", e );
        }
    }


    public int getCallCount() {
        synchronized ( lock ) {
            return callCount.get();
        }
    }


    public CallStatsSnapshot getStatsSnapshot( boolean isRunning, long startTime, long stopTime ) {
        synchronized ( lock )
        {
            return new CallStatsSnapshot( callCount.get(), maxTime, minTime, meanTime, isRunning, startTime, stopTime );
        }
    }


    public int callOccurred( Perftest test, long startTime, long endTime, TimeUnit units )
    {
        synchronized ( lock ) {
            if ( callCount.get() >  test.getCallCount() - 1 ) {
                return callCount.get();
            }

            if ( this.units.equals( units ) ) {
                long time = endTime - startTime;

                totalTime += time;
                maxTime = Math.max( maxTime, time );
                minTime = Math.min( minTime, time );
                int numCalls = callCount.incrementAndGet();
                StringBuilder sb = new StringBuilder();
                sb.append( numCalls ).append( " " ).append( startTime ).append( " " ).append( endTime );
                log.write(sb.toString());
                meanTime = totalTime / numCalls;
                return numCalls;
            }
            else {
                throw new RuntimeException( "Time unit corrections have not been implemented." );
            }
        }
    }


    public File getResultsFile() {
        return new File( log.getPath() );
    }

    public void stop() {
        log.close();
    }


    public void reset() {
        try {
            log.truncate();
        }
        catch ( IOException e ) {
            LOG.error( "Failed to truncate the results file.", e );
        }
    }
}
