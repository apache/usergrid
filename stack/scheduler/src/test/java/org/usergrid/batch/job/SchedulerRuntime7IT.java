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
public class SchedulerRuntime7IT extends AbstractSchedulerRuntimeIT
{
  private static final String TIMEOUT_PROP = "usergrid.scheduler.job.timeout";
  private static final String RUNNLOOP_PROP = "usergrid.scheduler.job.interval";

  /**
   * Test that we're only running once, even when a job exceeds the heartbeat time
   * 
   * @throws Exception
   */
  @Test
  public void onlyOnceTestOnException() throws Exception {

    long sleepTime = Long.parseLong(props.getProperty(TIMEOUT_PROP));
    
    long runLoop = Long.parseLong(props.getProperty(RUNNLOOP_PROP));

    long customRetry = sleepTime * 2;
    int numberOfRuns = 2;

    OnlyOnceUnlockOnFailExceution job = cassandraResource.getBean("onlyOnceUnlockOnFailExceution", OnlyOnceUnlockOnFailExceution.class);

    job.setTimeout(customRetry);
    job.setLatch(numberOfRuns);
    job.setDelay(sleepTime);

    JobData returned = scheduler.createJob("onlyOnceUnlockOnFailExceution", System.currentTimeMillis(), new JobData());

    // sleep until the job should have failed. We sleep 1 extra cycle just to make sure we're not racing the test
    
    boolean waited = job.waitForException(runLoop * numberOfRuns *2 , TimeUnit.MILLISECONDS);

    assertTrue("Job threw exception", waited);
    
    boolean bothAttempted = job.waitForCount(runLoop * numberOfRuns * 2, TimeUnit.MILLISECONDS);
    
    assertTrue("Both jobs tried to run", bothAttempted);

    boolean completed = job.waitForCompletion(runLoop*numberOfRuns*2, TimeUnit.MILLISECONDS);
    
    assertTrue("One completed", completed);
    
    JobStat stat = scheduler.getStatsForJob(returned.getJobName(), returned.getUuid());

   
  
    
    stat = scheduler.getStatsForJob(returned.getJobName(), returned.getUuid());

    // we should have only marked this as run once since we delayed furthur execution
    // we should have only marked this as run once
    assertEquals(numberOfRuns, stat.getTotalAttempts());
    assertEquals(numberOfRuns, stat.getRunCount());
    assertEquals(0, stat.getDelayCount());
  }
}
