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
public class SchedulerRuntime1IT extends AbstractSchedulerRuntimeIT {
	
	private static final Logger logger = LoggerFactory.getLogger(SchedulerRuntime1IT.class.getName());
    @Test
    public void basicScheduling() throws InterruptedException {
        CountdownLatchJob counterJob = cassandraResource.getBean( CountdownLatchJob.class );
        // set the counter job latch size
        counterJob.setLatch( getCount() );


        getJobListener().setExpected( getCount() );

        for ( int i = 0; i < getCount(); i++ ) {
            scheduler.createJob( "countdownLatch", System.currentTimeMillis(), new JobData() );
        }

        // previously: 
        // now wait until everything fires or no jobs complete in #waitTime seconds
        // boolean waited = getJobListener().blockTilDone( getCount(),  waitTime);
        
        // now:
        // note that the waitForCount only wait for job execution. It does NOT wait for job Completion
        boolean waited = counterJob.waitForCount(waitTime, TimeUnit.MILLISECONDS);
        assertTrue( "Failed to run " + getCount() + " number of jobs. Waited " + waitTime + " seconds.", waited );
        
        // previously:
        // assertTrue( getJobListener().getSuccessCount() + " successful jobs ran, expected " + getCount(), getCount() == getJobListener().getSuccessCount() );
        
        // now:
        // blockTilDone look into the JobListener hook and blocked until jobs are completed.
        // TODO : need a retry count so it doesn't reblock forever
        while (!getJobListener().blockTilDone(waitTime)) {
        	logger.warn("Jobs not yet finished after waited {}, block again" , waitTime);
        }
        assertEquals( "Expected success job: " + getCount()+ ". Actual :" + getJobListener().getSuccessCount() + ". Total count: " + getJobListener().getDoneCount() , getCount() , getJobListener().getSuccessCount() );
        
    }
}
