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


import org.junit.Ignore;
import org.junit.Test;

import org.apache.usergrid.persistence.entities.JobData;
import org.apache.usergrid.persistence.entities.JobStat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Class to test job runtimes
 */

@Ignore("These tests no longer work with shared spring context. Need to re-evaluate")
public class SchedulerRuntime4IT extends AbstractSchedulerRuntimeIT {
    /**
     * Test the scheduler ramps up correctly when there are more jobs to be read after a pause when the job specifies
     * the retry time
     */
    @Test
    public void delayExecution() throws Exception {
        long sleepTime = Long.parseLong( props.getProperty( TIMEOUT_PROP ) );

        int delayCount = 2;

        long customRetry = sleepTime * 2;

        DelayExecution job = springResource.getBean( "delayExecution", DelayExecution.class );

        job.setTimeout( customRetry );

        int runCount = delayCount +1;

        job.setLatch( runCount );

        getJobListener().setExpected( runCount );

        JobData returned = scheduler.createJob( "delayExecution", System.currentTimeMillis(), new JobData() );

        scheduler.refreshIndex();

        // sleep until the job should have failed. We sleep 1 extra cycle just to
        // make sure we're not racing the test
        boolean waited = getJobListener().blockTilDone( 50000L + sleepTime * 2 );

        assertTrue( "Job ran to complete", waited );

        scheduler.refreshIndex();

        JobStat stat = scheduler.getStatsForJob( returned.getJobName(), returned.getUuid() );

        // we should have only marked this as run once since we delayed furthur execution
        // we should have only marked this as run once
        assertEquals( 1, stat.getTotalAttempts() );
        assertEquals( runCount, stat.getRunCount() );
        assertEquals( delayCount, stat.getDelayCount() );
    }
}
