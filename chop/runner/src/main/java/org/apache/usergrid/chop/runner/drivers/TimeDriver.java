/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.runner.drivers;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.usergrid.chop.api.Signal;
import org.apache.usergrid.chop.api.TimeChop;


/**
 * Runs a time constrained chop performance test.
 */
public class TimeDriver extends Driver<TimeTracker> {
    private final CountDownLatch latch;


    public TimeDriver( Class<?> testClass ) {
        super( new TimeTracker( testClass ) );
        latch = new CountDownLatch( getTracker().getThreads() );
    }


    @Override
    public void start() {
        synchronized ( lock ) {
            if ( state == State.READY ) {
                state = state.next( Signal.START );

                executorService.submit( new Runnable() {
                    @Override
                    public void run() {
                        LOG.info( "Started completion detection job." );

                        try {
                            while ( latch.getCount() > 0 ) {
                                latch.await( getTimeout(), TimeUnit.MILLISECONDS );
                            }
                        }
                        catch ( InterruptedException e ) {
                            LOG.warn( "Awe snap! Someone woke me up early!", e );
                        }

                        LOG.info( "All threads stopped processing. Time to stop tracker and complete." );
                        getTracker().stop();
                        state = state.next( Signal.COMPLETED );
                        lock.notifyAll();
                    }
                } );

                final TimeChop timeChop = getTracker().getTimeChop();
                for ( int ii = 0; ii < getTracker().getThreads(); ii++ ) {
                    final int id = ii;
                    executorService.submit( new Runnable() {
                        @Override
                        public void run() {
                            long runTime;

                            do {
                                runTime = System.currentTimeMillis() - getTracker().getStartTime();
                                LOG.info( "Running for {} ms, will stop in {} ms", runTime, timeChop.time() - runTime );

                                // execute the tests and capture tracker
                                getTracker().execute();

                                // if a delay between runs is requested apply it
                                if ( timeChop.delay() > 0 ) {
                                    try {
                                        Thread.sleep( timeChop.delay() );
                                    }
                                    catch ( InterruptedException e ) {
                                        LOG.warn( "Awe snap, someone woke me up early!" );
                                    }
                                }
                            }
                            while ( runTime < timeChop.time() && isRunning() );

                            latch.countDown();
                            LOG.info( "Thread {} completed, count down latch value = {}", id, latch.getCount() );
                        }
                    } );
                }
            }
        }
    }
}
