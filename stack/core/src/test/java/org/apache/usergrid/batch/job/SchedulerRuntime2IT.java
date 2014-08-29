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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.persistence.entities.JobData;


/**
 * Class to test job runtimes
 */
@Concurrent
public class SchedulerRuntime2IT extends AbstractSchedulerRuntimeIT {
	private static final Logger logger = LoggerFactory.getLogger(SchedulerRuntime2IT.class.getName());

    /** Test the scheduler ramps up correctly when there are more jobs to be read after a pause */
    @Test
    public void schedulingWithNoJobs() throws InterruptedException {
        CountdownLatchJob counterJob = cassandraResource.getBean( CountdownLatchJob.class );
        // set the counter job latch size
        counterJob.setLatch( getCount() );

        getJobListener().setExpected( getCount() );

        for ( int i = 0; i < getCount(); i++ ) {
            scheduler.createJob( "countdownLatch", System.currentTimeMillis(), new JobData() );
        }

        scheduler.refreshIndex();

        // previously:
        // now wait until everything fires
        // boolean waited = getJobListener().blockTilDone( getCount(), 15000L );
        // assertTrue( "Jobs ran", waited );
        // assertTrue( getCount() + " successful jobs ran", 
        // getCount() == getJobListener().getSuccessCount() );
        
        // now:
        // note that the waitForCount only wait for job execution. It does NOT wait for job Completion
        boolean waited = counterJob.waitForCount(waitTime, TimeUnit.MILLISECONDS);
        assertTrue( "Failed to run " 
                + getCount() + " number of jobs. Waited " + waitTime + " ms.", waited );
        
        scheduler.refreshIndex();

        // now:
        // blockTilDone look into the JobListener hook and blocked until jobs are completed.
        // TODO : need a retry count so it doesn't reblock forever
        while (!getJobListener().blockTilDone(waitTime)) {
        	logger.warn("Jobs not yet finished after waited {}, block again" , waitTime);
        }
        assertEquals( "Expected success job: " + getCount()+ ". Actual :" 
            + getJobListener().getSuccessCount() + ". Total count: " 
            + getJobListener().getDoneCount() , getCount() , getJobListener().getSuccessCount() );
        
        Thread.sleep( 5000L );

        // set the counter job latch size
        counterJob.setLatch( getCount() );
        getJobListener().setExpected( getCount() );

        for ( int i = 0; i < getCount(); i++ ) {
            scheduler.createJob( "countdownLatch", System.currentTimeMillis(), new JobData() );
        }

        scheduler.refreshIndex();

        // previously: 
        // now wait until everything fires
        // waited = getJobListener().blockTilDone( 2 * getCount(), 15000L );
        // waited = counterJob.waitForCount(waitTime, TimeUnit.MILLISECONDS );
        // assertTrue( "Failed to run " + 2* getCount() + " number of jobs. Success count = " 
        // + getJobListener().getSuccessCount() + ". Waited " + waitTime  + " seconds.", waited );
        // assertTrue( 2 * getCount() + " successful jobs ran",
        //  ( 2 * getCount() ) == getJobListener().getSuccessCount() );
		
		// now: note that the waitForCount only wait for job execution. 
        // It does NOT wait for job Completion
        waited = counterJob.waitForCount(waitTime, TimeUnit.MILLISECONDS);
        assertTrue( "Failed to run " + getCount() + " number of jobs. Waited " + waitTime 
                + " seconds.", waited );
        
        scheduler.refreshIndex();

        // now:
        // blockTilDone look into the JobListener hook and blocked until jobs are completed.
        // TODO : need a retry count so it doesn't reblock forever
        while (!getJobListener().blockTilDone(waitTime)) {
        	logger.warn("Jobs not yet finished after waited {}, block again" , waitTime);
        }
        assertEquals( "Expected success job: " +2 * getCount()+ ". Actual :" 
            + getJobListener().getSuccessCount() + ". Total count: " 
            + getJobListener().getDoneCount(), 2 * getCount() , getJobListener().getSuccessCount());
    }
}
