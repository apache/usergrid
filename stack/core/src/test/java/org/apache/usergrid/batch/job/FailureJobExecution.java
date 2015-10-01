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
import org.springframework.stereotype.Component;

import org.apache.usergrid.batch.Job;
import org.apache.usergrid.batch.JobExecution;


/**
 * A simple job that does nothing but increment an atomic counter
 *
 * @author tnine
 */
@Component("failureJobExceuction")
@Ignore("Not a test")
public class FailureJobExecution implements Job {

    private CountDownLatch latch = null;
    private CountDownLatch deadLatch = null;


    /**
     *
     */
    public FailureJobExecution() {
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.batch.Job#execute(org.apache.usergrid.batch.JobExecution)
     */
    @Override
    public void execute( JobExecution execution ) throws Exception {
        latch.countDown();

        throw new RuntimeException( "Job Failed" );
    }


    @Override
    public void dead( final JobExecution execution ) throws Exception {
        deadLatch.countDown();;
    }


    public void setLatch( int calls ) {
        latch = new CountDownLatch( calls );
        deadLatch = new CountDownLatch( 1 );
    }



    public boolean waitForCount( long timeout, TimeUnit unit ) throws InterruptedException {
        return latch.await( timeout, unit );
    }

    public boolean waitForDead(long timeout, TimeUnit unit) throws InterruptedException {
        return deadLatch.await( timeout, unit );
    }


    public long getLatchCount() {
        return latch.getCount();
    }
}
