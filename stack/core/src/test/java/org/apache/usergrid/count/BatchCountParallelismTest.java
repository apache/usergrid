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


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.count.common.Count;

import static org.junit.Assert.assertEquals;


/** @author zznate */
@net.jcip.annotations.NotThreadSafe
public class BatchCountParallelismTest {

	private static final Logger LOG = LoggerFactory.getLogger( BatchCountParallelismTest.class );
    private ExecutorService exec = Executors.newFixedThreadPool( 24 );
    private SimpleBatcher batcher;
    private StubSubmitter submitter = new StubSubmitter();

    private AtomicLong submits;


    @Before
    public void setupLocal() {
        submits = new AtomicLong();

        batcher = new SimpleBatcher();
        batcher.setBatchSize( 10 );
        batcher.setBatchSubmitter( submitter );
    }


    @Test
    @Ignore("This test causes the build to hang when all stack tests are run")
    public void verifyConcurrentAdd() throws Exception {

        final long startCount = batcher.invocationCounter.count();

        List<Future<Boolean>> calls = new ArrayList<Future<Boolean>>();
        // create 10 tasks
        // submit should be invoked 10 times
        final CountDownLatch cdl = new CountDownLatch( 10 );
        for ( int x = 0; x < 10; x++ ) {
            final int c = x;

            // each task should increment the counter 10 times

            calls.add( exec.submit( new Callable<Boolean>() {

                @Override
                public Boolean call() throws Exception {
                    // should increment this counter to 10 for this thread, 100 overall
                    // this is invoked 10 times
                    for ( int y = 0; y < 10; y++ ) {
                        Count count = new Count( "Counter", "k1", "counter1", 1 );
                        batcher.add( count );
                    }
                    LOG.info( "Task iteration # {} : ", c );
                    cdl.countDown();
                    return true;
                }
            } ) );
        }
        batcher.add( new Count( "Counter", "k1", "counter1", 1 ) );
        LOG.info( "size: " + calls.size() );

        cdl.await();
        //    exec.awaitTermination(2,TimeUnit.SECONDS);

        exec.shutdown();
        while  (! exec.awaitTermination( 3, TimeUnit.SECONDS ) ) {
        	LOG.warn("jobs not yet finished, wait again");
        }
        // we should have 100 total invocations of AbstractBatcher#add

        final long currentCount = batcher.invocationCounter.count();

        final long delta = currentCount - startCount;

        assertEquals( 101, delta );
        // we should have submitted 10 batches

        // jobs can finished executed, but the batcher may not have flush and so the batchSubmissionCount may not reach the total submitted yet"
        //TODO beautify the following hack?
        int iteration =0 ;
        int total_retry = 10;
        while (batcher.getBatchSubmissionCount() != 10 || iteration < total_retry) {
        	Thread.sleep(3000L);
        	iteration++;
        }
        assertEquals( 10, batcher.getBatchSubmissionCount() );

        // the first batch should have a size 10 TODO currently 11 though :(


    }


    class StubSubmitter implements BatchSubmitter {

        AtomicLong counted = new AtomicLong();
        AtomicLong submit = new AtomicLong();


        @Override
        public Future<?> submit( Collection<Count> counts ) {
            LOG.info( "submitted: " + counts.size() );
            counted.addAndGet( counts.size() );
            submit.incrementAndGet();
            return null;
        }


        @Override
        public void shutdown() {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
