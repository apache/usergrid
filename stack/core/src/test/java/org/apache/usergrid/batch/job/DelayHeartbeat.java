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


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.apache.usergrid.batch.Job;
import org.apache.usergrid.batch.JobExecution;


/**
 * A simple job that does nothing but increment an atomic counter and use heartbeat delay
 *
 * @author tnine
 */
@Component("delayHeartbeat")
@Ignore("Not a test")
public class DelayHeartbeat implements Job {

    private static final Logger logger = LoggerFactory.getLogger( DelayHeartbeat.class );

    private CountDownLatch latch = null;
    private long timeout;
    private int delayCount;


    /**
     *
     */
    public DelayHeartbeat() {
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.batch.Job#execute(org.apache.usergrid.batch.JobExecution)
     */
    @Override
    public void execute( JobExecution execution ) throws Exception {


        while ( latch.getCount() > 1 ) {
            logger.info( "Running heartbeat execution" );
            execution.heartbeat( timeout );
            latch.countDown();
            Thread.sleep( timeout - 1 );
        }

        latch.countDown();
    }


    @Override
    public void dead( final JobExecution execution ) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    public void setLatch( int calls ) {
        latch = new CountDownLatch( calls );
    }


    public boolean waitForCount( long timeout, TimeUnit unit ) throws InterruptedException {
        return latch.await( timeout, unit );
    }


    /** @return the timeout */
    public long getTimeout() {
        return timeout;
    }


    /** @param timeout the timeout to set */
    public void setTimeout( long timeout ) {
        this.timeout = timeout;
    }


    /** @return the delayCount */
    public int getDelayCount() {
        return delayCount;
    }


    /** @param delayCount the delayCount to set */
    public void setDelayCount( int delayCount ) {
        this.delayCount = delayCount;
    }
}
