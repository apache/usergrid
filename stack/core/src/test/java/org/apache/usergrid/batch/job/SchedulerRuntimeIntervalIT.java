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


import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.batch.service.JobSchedulerService;
import org.apache.usergrid.persistence.entities.JobData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Class to test job that the run loop executes in the time expected when there's no jobs to run.
 * Tests saturation at each point of the runtime as well
 */

@Ignore("Ignored awaiting fix for USERGRID-267")
public class SchedulerRuntimeIntervalIT extends AbstractSchedulerRuntimeIT {

	private static final Logger logger =
            LoggerFactory.getLogger(SchedulerRuntimeIntervalIT.class.getName());

    private static final long EXPECTED_RUNTIME = 60000;
//    private static final long EXPECTED_RUNTIME = 3000000;


    /**
     * This is a combination of ( count+1 ) * interval*2.  If this test takes longer than this
     * to run, we have a bug in how often the run loop is executing
     */
    @Test(timeout = EXPECTED_RUNTIME)
    public void runLoopTest() throws InterruptedException {

        // the number of iterations we should run
        final int pollCount = 5;
        final int expectedInterval = 5000;

        JobSchedulerService schedulerService = springResource.getBean(JobSchedulerService.class);

        final long interval = schedulerService.getInterval();
        final int numberOfWorkers = schedulerService.getWorkerSize();
        final int expectedExecutions = numberOfWorkers * pollCount;

        assertEquals("Interval must be set to "+ expectedInterval
                + " for test to work properly", expectedInterval, interval);

        CountdownLatchJob counterJob = springResource.getBean( CountdownLatchJob.class );
            // set the counter job latch size
        counterJob.setLatch( expectedExecutions );

        getJobListener().setExpected(expectedExecutions );

        long fireTime = System.currentTimeMillis();

         // We want to space the jobs out so there will most likely be an empty poll phase.
         // For each run where we do get jobs, we want to saturate the worker pool to ensure the
        // semaphore is release properly
        for ( int i = 0; i < pollCount; i++ ) {

            for(int j = 0; j < numberOfWorkers; j ++){
                scheduler.createJob( "countdownLatch", fireTime, new JobData() );
            }
            fireTime += expectedInterval*2;
        }

        boolean waited = counterJob.waitForCount(EXPECTED_RUNTIME, TimeUnit.MILLISECONDS);

        assertTrue( "Ran" + getCount() + " number of jobs", waited);

        while (!getJobListener().blockTilDone(EXPECTED_RUNTIME)) {
        	logger.warn("Jobs not yet finished after waited {}, block again" , waitTime);
        }

        // If we get to here without timing out, the test ran correctly.
        // The assertion is implicit in the timeout
    }
}
