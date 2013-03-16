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

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.usergrid.batch.service.JobData;
import org.usergrid.batch.service.JobSchedulerService;
import org.usergrid.batch.service.SchedulerService;
import org.usergrid.cassandra.CassandraRunner;

import com.google.common.util.concurrent.Service.State;

/**
 * Class to test job runtimes
 * 
 * @author tnine
 * 
 */
@RunWith(CassandraRunner.class)
public class SchedulerRuntimeTest {

  private SchedulerService scheduler;

  @Before
  public void setup() {
    scheduler = CassandraRunner.getBean(SchedulerService.class);
    JobSchedulerService jobScheduler = CassandraRunner.getBean(JobSchedulerService.class);
    if(jobScheduler.state() != State.RUNNING){
      jobScheduler.start();
    }
  }

  @Test
  public void basicScheduling() throws InterruptedException {

    int count = 1000;

    CountdownLatchJob counterJob = CassandraRunner.getBean(CountdownLatchJob.class);
    // set the counter job latch size
    counterJob.setLatch(count);

    for (int i = 0; i < count; i++) {
      scheduler.createJob("countdownLatch", System.currentTimeMillis(), new JobData());
    }

    // now wait until everything fires
    boolean waited = counterJob.waitForCount(60, TimeUnit.SECONDS);

    assertTrue("Jobs ran", waited);

  }
  
  /**
   * Test the scheduler ramps up correctly when there are more jobs to be read after a pause
   * @throws InterruptedException
   */
  @Test
  public void schedulingWithNoJobs() throws InterruptedException {

    int count = 200;

    CountdownLatchJob counterJob = CassandraRunner.getBean(CountdownLatchJob.class);
    // set the counter job latch size
    counterJob.setLatch(count);

    for (int i = 0; i < count; i++) {
      scheduler.createJob("countdownLatch", System.currentTimeMillis(), new JobData());
    }
    
    // now wait until everything fires
    boolean waited = counterJob.waitForCount(30000, TimeUnit.SECONDS);

    assertTrue("Jobs ran", waited);
    
    Thread.sleep(5000);
    
    // set the counter job latch size
    counterJob.setLatch(count);

    for (int i = 0; i < count; i++) {
      scheduler.createJob("countdownLatch", System.currentTimeMillis(), new JobData());
    }
    
    // now wait until everything fires
    waited = counterJob.waitForCount(3000000, TimeUnit.SECONDS);

    assertTrue("Jobs ran", waited);
    
    
    
    

  }

}
