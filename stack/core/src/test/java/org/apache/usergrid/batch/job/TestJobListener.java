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


import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.batch.service.JobListener;


/**
 * Implementation of the JobListener for tests.
 */
public class TestJobListener implements JobListener {
    // public static final long WAIT_MAX_MILLIS = 250;
	public static final long WAIT_MAX_MILLIS = 60000L; // max wait 1 minutes 
    private static final Logger LOG = LoggerFactory.getLogger( TestJobListener.class );
    private AtomicInteger submittedCounter = new AtomicInteger();
    private AtomicInteger failureCounter = new AtomicInteger();
    private AtomicInteger successCounter = new AtomicInteger();
    private final Object lock = new Object();


    public int getSubmittedCount() {
        return submittedCounter.get();
    }


    public int getFailureCount() {
        return failureCounter.get();
    }


    public int getSuccessCount() {
        return successCounter.get();
    }


    public int getDoneCount() {
        return successCounter.get() + failureCounter.get();
    }


    public void onSubmit( JobExecution execution ) {
        LOG.debug( "Job execution {} submitted with count {}.", execution,
                submittedCounter.incrementAndGet() );
    }


    public void onSuccess( JobExecution execution ) {
        LOG.debug( "Job execution {} succeeded with count {}.", execution,
                successCounter.incrementAndGet() );
    }


    public void onFailure( JobExecution execution ) {
        LOG.debug( "Job execution {} failed with count {}.", execution,
                failureCounter.incrementAndGet() );
    }



    /**
     * block until submitted jobs are all accounted for.
     * 
     * @param jobCount total submitted job
     * @param idleTime idleTime in millisecond.
     * @return true when all jobs are completed (could be succeed or failed) within the given idleTime range, or {@value #WAIT_MAX_MILLIS}, whichever is smaller
     * @throws InterruptedException
     */
    public boolean blockTilDone( int jobCount, long idleTime ) throws InterruptedException
    {
        final long waitTime = Math.min( idleTime, WAIT_MAX_MILLIS );
        long lastChangeTime = System.currentTimeMillis();
        long timeNotChanged = 0;
        int currentCount;
        int startCount = getDoneCount();

        do {
            currentCount = getDoneCount();

            if ( startCount == currentCount ) {
//                if ( timeNotChanged > idleTime ) {
                if ( timeNotChanged > waitTime ) {
                    return false;
                }

                timeNotChanged = System.currentTimeMillis() - lastChangeTime;
            }
            else {
                timeNotChanged = 0;
                startCount = currentCount;
                lastChangeTime = System.currentTimeMillis();
            }

            synchronized ( lock ) {
                lock.wait( waitTime );
            }
        } while ( currentCount < jobCount );

        return true;
    }
}
