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

import org.apache.usergrid.batch.JobExecution;


/**
 * A job that will sleep for the amount of time specified. Used to check that our counter is only ever run once.  Checks
 * the lock is released on fail
 *
 * @author tnine
 */
@Component("onlyOnceUnlockOnFailExceution")
@Ignore("Not a test")
public class OnlyOnceUnlockOnFailExceution extends OnlyOnceJob {

    private static final Logger logger = LoggerFactory.getLogger( OnlyOnceUnlockOnFailExceution.class );

    private CountDownLatch latch = null;
    private CountDownLatch exception = new CountDownLatch( 1 );
    private CountDownLatch completed = new CountDownLatch( 1 );
    private long timeout;
    private boolean slept = false;
    private long delay;


    /**
     *
     */
    public OnlyOnceUnlockOnFailExceution() {
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.batch.job.OnlyOnceJob#doJob(org.apache.usergrid.batch.JobExecution)
     */
    @Override
    protected void doJob( JobExecution execution ) throws Exception {
        logger.info( "Running only once execution" );


        latch.countDown();

        if ( !slept ) {
            logger.info( "Sleeping in only once execution" );
            Thread.sleep( timeout );
            slept = true;
            exception.countDown();
            throw new RuntimeException( "I failed to run correctly, I should be retried" );
        }

        completed.countDown();
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.batch.job.OnlyOnceJob#getDelay(org.apache.usergrid.batch.JobExecution)
     */
    @Override
    protected long getDelay( JobExecution execution ) throws Exception {
        return delay;
    }


    public void setDelay( long delay ) {
        this.delay = delay;
    }


    public void setLatch( int calls ) {
        latch = new CountDownLatch( calls );
    }


    public boolean waitForCount( long timeout, TimeUnit unit ) throws InterruptedException {
        return latch.await( timeout, unit );
    }


    public boolean waitForException( long timeout, TimeUnit unit ) throws InterruptedException {
        return exception.await( timeout, unit );
    }


    public boolean waitForCompletion( long timeout, TimeUnit unit ) throws InterruptedException {
        return completed.await( timeout, unit );
    }


    /** @return the timeout */
    public long getTimeout() {
        return timeout;
    }


    /** @param timeout the timeout to set */
    public void setTimeout( long timeout ) {
        this.timeout = timeout;
    }


    @Override
    public void dead( final JobExecution execution ) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
