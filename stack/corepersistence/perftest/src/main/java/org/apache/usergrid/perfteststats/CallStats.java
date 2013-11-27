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
package org.apache.usergrid.perfteststats;


import org.apache.usergrid.perftest.Perftest;
import org.apache.usergrid.perftest.logging.Log;
import org.apache.usergrid.perftest.rest.CallStatsSnapshot;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Atomically stores and updates call statistics on tests.
 */
public class CallStats {
    @Log Logger log;

    private final AtomicInteger callCount = new AtomicInteger();
    private final Object lock = new Object();
    private final TimeUnit units = TimeUnit.NANOSECONDS;

    private long maxTime = Long.MIN_VALUE;
    private long minTime = Long.MAX_VALUE;
    private long meanTime = 0;
    private long totalTime = 0;


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
        synchronized ( lock )
        {
            if ( callCount.get() >  test.getCallCount() - 1 )
            {
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
                log.debug(sb.toString());
                meanTime = totalTime / numCalls;
                return numCalls;
            }
            else {
                throw new RuntimeException( "Time unit corrections have not been implemented." );
            }
        }
    }
}
