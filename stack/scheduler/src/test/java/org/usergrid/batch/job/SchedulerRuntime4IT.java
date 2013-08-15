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


import com.google.common.util.concurrent.Service.State;
import org.junit.*;
import org.usergrid.batch.service.JobSchedulerService;
import org.usergrid.batch.service.SchedulerService;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.persistence.entities.JobData;
import org.usergrid.persistence.entities.JobStat;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;


/**
 * Class to test job runtimes
 * 
 * @author tnine
 */
@Concurrent()
@Ignore( "TODO: Todd fix. Does not reliably pass on our build server." )
public class SchedulerRuntime4IT extends AbstractSchedulerRuntimeIT
{
  private static final String TIMEOUT_PROP = "usergrid.scheduler.job.timeout";

  @Before
  public void setup() {
    scheduler = cassandraResource.getBean(SchedulerService.class);

    props = cassandraResource.getBean("properties", Properties.class);

    // start the scheduler after we're all set up
    JobSchedulerService jobScheduler = cassandraResource.getBean(JobSchedulerService.class);
    if (jobScheduler.state() != State.RUNNING) {
      jobScheduler.startAndWait();
    }

  }


  /**
   * Test the scheduler ramps up correctly when there are more jobs to be read
   * after a pause when the job specifies the retry time
   * 
   * @throws Exception
   */
  @Test
  public void delayExecution() throws Exception {

    long sleepTime = Long.parseLong(props.getProperty(TIMEOUT_PROP));

    int delayCount = 2;

    long customRetry = sleepTime * 2;

    DelayExecution job = cassandraResource.getBean("delayExecution", DelayExecution.class);

    job.setTimeout(customRetry);
    job.setLatch(delayCount+1);

    JobData returned = scheduler.createJob("delayExecution", System.currentTimeMillis(), new JobData());

    // sleep until the job should have failed. We sleep 1 extra cycle just to
    // make sure we're not racing the test
    boolean waited = job.waitForCount(customRetry * (delayCount *2 ), TimeUnit.MILLISECONDS);

    assertTrue("Job ran to complete", waited);

    JobStat stat = scheduler.getStatsForJob(returned.getJobName(), returned.getUuid());

    // we should have only marked this as run once since we delayed furthur execution
    // we should have only marked this as run once
    assertEquals(1, stat.getTotalAttempts());
    assertEquals(delayCount+1, stat.getRunCount());
    assertEquals(delayCount, stat.getDelayCount());
  }
}
