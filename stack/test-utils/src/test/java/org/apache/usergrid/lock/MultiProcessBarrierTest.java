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

package org.apache.usergrid.lock;


import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import sun.security.action.GetPropertyAction;

import static org.apache.usergrid.TestHelper.newUUIDString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * A test that tests our multiprocesses play nice across threads
 */
public class MultiProcessBarrierTest {


    @Test
    public void singleBarrierTest() throws IOException, InterruptedException, TimeoutException {
        final String file = newFileName();

        final MultiProcessBarrier barrier = new MultiProcessBarrier( file );


        try {
            barrier.await( 500 );
            fail( "I should timeout" );
        }
        catch ( TimeoutException te ) {
            //swallow, should timeout
        }

        //now proceed then away
        barrier.proceed();
        barrier.await( 100 );
    }


    @Test
    public void barrierTest() throws IOException, InterruptedException {
        final String file = newFileName();

        //create 2 threads
        final CountDownLatch latch = new CountDownLatch( 2 );

        //create 2 worker threads.  We need to run them, and ensure that they don't countdown.

        new BarrierThread( file, latch ).start();
        new BarrierThread( file, latch ).start();

        assertEquals(2, latch.getCount());

        //now create the barrier and execute it

        MultiProcessBarrier barrier = new MultiProcessBarrier( file );
        barrier.proceed();

        assertTrue( "other barriers proceeded", latch.await( 1000, TimeUnit.MILLISECONDS ) );
    }






    /**
     * Generate and delt
     */
    private String newFileName() throws IOException {
        final File tmpdir = new File( AccessController.doPrivileged( new GetPropertyAction( "java.io.tmpdir" ) ) );

        return tmpdir.getAbsoluteFile().toString() + "/" + newUUIDString();
    }


    /**
     * A simple inner thread that tests we block until proceeding
     */
    private final class BarrierThread extends Thread{

        private final String fileName;

        private final CountDownLatch completeLatch;


        private BarrierThread( final String fileName, final CountDownLatch completeLatch) {
            this.fileName = fileName;
            this.completeLatch = completeLatch;
        }


        @Override
        public void run() {

            MultiProcessBarrier barrier = new MultiProcessBarrier( fileName );


            try {
                barrier.await( 10000 );
            }
            catch ( Exception e ) {
                throw new RuntimeException( e );
            }

            completeLatch.countDown();
        }
    }
}
