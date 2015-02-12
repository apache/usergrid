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
package org.apache.usergrid.batch.job;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.persistence.entities.JobData;
import org.apache.usergrid.persistence.entities.JobStat;

import static org.junit.Assert.assertTrue;


/**
 * Tests the TestJobListener.
 */
public class TestJobListenerTest {
    private static final Logger LOG = LoggerFactory.getLogger( TestJobListenerTest.class );
    JobExecution jobExecution = new JobExecution() {
        @Override
        public JobData getJobData() {
            return null;
        }


        @Override
        public JobStat getJobStats() {
            return null;
        }


        @Override
        public void start( final int maxFailures ) {
        }


        @Override
        public void completed() {
        }


        @Override
        public void failed() {
        }


        @Override
        public void killed() {
        }


        @Override
        public void heartbeat() {
        }


        @Override
        public void heartbeat( final long milliseconds ) {
        }


        @Override
        public void delay( final long milliseconds ) {
        }


        @Override
        public JobExecution.Status getStatus() {
            return null;
        }


        @Override
        public String getJobName() {
            return "mockJob";
        }


        @Override
        public java.util.UUID getJobId() {
            return null;
        }


        @Override
        public java.util.UUID getTransactionId() {
            return null;
        }
    };

    @Test
    public void testIdleOut() throws InterruptedException {
        TestJobListener listener = new TestJobListener();
        long waitTime = 1000L;
        long startTime = System.currentTimeMillis();
        listener.setExpected( 100 );
		listener.blockTilDone( waitTime );
        long elapsedTime = System.currentTimeMillis() - startTime;
        LOG.info( "IdleOut in {} millis", elapsedTime );
        // assertTrue( elapsedTime >= ( 1000L + TestJobListener.WAIT_MAX_MILLIS ) );
        assertTrue("Elapsed time: " + elapsedTime + " fails to be greater than idle wait time: " + waitTime,  elapsedTime>= waitTime );
    }


    @Test
    public void testHitCount() throws InterruptedException {
        final TestJobListener listener = new TestJobListener();

        listener.setExpected( 1000 );

        Thread t = new Thread( new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep( 100 );
                }
                catch ( InterruptedException e ) {
                    LOG.warn( "Thread got interrupted", e );
                }
                for ( int ii = 0; ii < 1000; ii++ ) {
                    listener.onSuccess( jobExecution );
                }
            }
        });
        t.start();

        assertTrue( listener.blockTilDone( 1000L ) );
    }
}
