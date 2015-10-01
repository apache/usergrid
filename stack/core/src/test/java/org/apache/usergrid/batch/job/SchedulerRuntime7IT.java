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

//@org.junit.Ignore( "Todd you need to take a look at this since it's not clear to me what was intended in this test." )
@Ignore("These tests no longer work with shared spring context. Need to re-evaluate")
public class SchedulerRuntime7IT extends AbstractSchedulerRuntimeIT {

    /** Test that we're only running once, even when a job exceeds the heartbeat time */
    @Test
    public void onlyOnceTestOnException() throws Exception {

        long sleepTime = Long.parseLong( props.getProperty( TIMEOUT_PROP ) );

        long runLoop = Long.parseLong( props.getProperty( RUNLOOP_PROP ) );

        long customRetry = sleepTime * 2;
        int numberOfRuns = 2;

        OnlyOnceUnlockOnFailExceution job =
                springResource.getBean( "onlyOnceUnlockOnFailExceution", OnlyOnceUnlockOnFailExceution.class );

        job.setTimeout( customRetry );
        job.setLatch( numberOfRuns );
        job.setDelay( sleepTime );

        getJobListener().setExpected( 2 );

        JobData returned =
                scheduler.createJob( "onlyOnceUnlockOnFailExceution", System.currentTimeMillis(), new JobData() );

        scheduler.refreshIndex();

        // sleep until the job should have failed. We sleep 1 extra cycle just to make sure we're not racing the test

        boolean waited = getJobListener().blockTilDone( runLoop * numberOfRuns * 2 + 5000L );

        scheduler.refreshIndex();

        assertTrue( "Both runs executed" , waited);
        assertTrue( "Job failed", getJobListener().getFailureCount() == 1 );
        assertTrue( "No Job succeeded", getJobListener().getSuccessCount() == 1 );

        JobStat stat = scheduler.getStatsForJob( returned.getJobName(), returned.getUuid() );

        // we should have only marked this as run once since we delayed furthur execution
        // we should have only marked this as run once
        assertEquals( numberOfRuns, stat.getTotalAttempts() );
        assertEquals( numberOfRuns, stat.getRunCount() );
        assertEquals( 0, stat.getDelayCount() );
    }
}
