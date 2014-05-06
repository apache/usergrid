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
import org.apache.usergrid.chop.api.IterationChop;


/**
 * Runs an iteration count constrained chop performance test.
 */
public class IterationDriver extends Driver<IterationTracker> {
    private final CountDownLatch latch;

    public IterationDriver( Class<?> testClass ) {
        super( new IterationTracker( testClass ) );
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

                final IterationChop iterationChop = getTracker().getIterationChop();
                for ( int ii = 0; ii < getTracker().getThreads(); ii++ ) {
                    executorService.submit( new Runnable() {
                        @Override
                        public void run() {
                            for ( int ii = 0; ii < iterationChop.iterations() && isRunning(); ii++ ) {
                                LOG.info( "Starting {}-th iteration", ii );

                                // execute the tests and capture tracker
                                getTracker().execute();

                                // if a delay between runs is requested apply it
                                if ( iterationChop.delay() > 0 ) {
                                    try {
                                        Thread.sleep( iterationChop.delay() );
                                    }
                                    catch ( InterruptedException e ) {
                                        LOG.warn( "Awe snap, someone woke me up early!" );
                                    }
                                }
                            }

                            latch.countDown();
                        }
                    } );
                }
            }
        }
    }
}
