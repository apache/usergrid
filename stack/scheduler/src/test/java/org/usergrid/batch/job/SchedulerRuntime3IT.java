/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.batch.job;


import org.junit.*;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.persistence.entities.JobData;
import org.usergrid.persistence.entities.JobStat;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;


/**
 * Class to test job runtimes
 * 
 * @author tnine
 */
@Concurrent()
@Ignore( "TODO: Todd fix. Does not reliably pass on our build server." )
public class SchedulerRuntime3IT extends AbstractSchedulerRuntimeIT
{
  private static final String FAIL_PROP = "usergrid.scheduler.job.maxfail";
  private static final String RUNNLOOP_PROP = "usergrid.scheduler.job.interval";

  /**
   * Test the scheduler ramps up correctly when there are more jobs to be read
   * after a pause
   * 
   * @throws Exception
   */
  @Test
  public void failureCausesJobDeath() throws Exception {

    int failCount = Integer.parseInt(props.getProperty(FAIL_PROP));
    long sleepTime = Long.parseLong(props.getProperty(RUNNLOOP_PROP));

    FailureJobExceuction job = cassandraResource.getBean("failureJobExceuction", FailureJobExceuction.class);

    int latchValue = failCount+1;
    
    job.setLatch(latchValue);

    JobData returned = scheduler.createJob("failureJobExceuction", System.currentTimeMillis(), new JobData());

    // sleep until the job should have failed. We sleep 1 extra cycle just to
    // make sure we're not racing the test
    boolean waited = job.waitForCount((failCount+2)*sleepTime, TimeUnit.MILLISECONDS);

    //we shouldn't trip the latch.  It should fail failCount times, and not run again
    assertFalse("Job ran to failure", waited);
    
    //we shouldn't have run the last time, we should have counted down to it
    assertEquals(1, job.getLatchCount());
    
    JobStat stat = scheduler.getStatsForJob(returned.getJobName(), returned.getUuid());

    // we should have only marked this as run fail+1 times
    assertEquals(latchValue, stat.getTotalAttempts());
    assertEquals(latchValue, stat.getRunCount());
    assertEquals(0, stat.getDelayCount());
    
  }
}
