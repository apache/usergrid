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
public class SchedulerRuntime6IT extends AbstractSchedulerRuntimeIT
{
  private static final String TIMEOUT_PROP = "usergrid.scheduler.job.timeout";


  /**
   * Test the scheduler ramps up correctly when there are more jobs to be read
   * after a pause when the job specifies the retry time
   * 
   * @throws Exception
   */
  @Test
  public void onlyOnceTest() throws Exception {

    long sleepTime = Long.parseLong(props.getProperty(TIMEOUT_PROP));

    long customRetry = sleepTime + 1000;
    int numberOfRuns = 1;

    OnlyOnceExceution job = cassandraResource.getBean("onlyOnceExceution", OnlyOnceExceution.class);

    job.setTimeout(customRetry);
    job.setLatch(numberOfRuns);
    job.setDelay(sleepTime);

    JobData returned = scheduler.createJob("onlyOnceExceution", System.currentTimeMillis(), new JobData());

    // sleep until the job should have failed. We sleep 1 extra cycle just to
    // make sure we're not racing the test
    boolean waited = job.waitForCount(customRetry * numberOfRuns *2 , TimeUnit.MILLISECONDS);

    assertTrue("Job ran twice", waited);
    
   
    //reset our latch immediately for further tests
    job.setLatch(numberOfRuns);

    JobStat stat = scheduler.getStatsForJob(returned.getJobName(), returned.getUuid());

    // we should have only marked this as run once since we delayed furthur execution
    // we should have only marked this as run once
    assertNotNull(stat);
    assertEquals(numberOfRuns, stat.getTotalAttempts());
    assertEquals(numberOfRuns, stat.getRunCount());
    assertEquals(0, stat.getDelayCount());
    
    
    
    boolean slept = job.waitForSleep(customRetry*numberOfRuns*2, TimeUnit.MILLISECONDS);
    
    assertTrue("Job slept", slept);
    
    
    //now wait again to see if the job fires one more time, it shouldn't 
    waited = job.waitForCount(customRetry * numberOfRuns *2,TimeUnit.MILLISECONDS);
    
    assertFalse("Job ran twice", waited);
    
    stat = scheduler.getStatsForJob(returned.getJobName(), returned.getUuid());

    // we should have only marked this as run once since we delayed furthur execution
    // we should have only marked this as run once
    assertEquals(numberOfRuns, stat.getTotalAttempts());
    assertEquals(numberOfRuns, stat.getRunCount());
    assertEquals(0, stat.getDelayCount());
    
  }
}
