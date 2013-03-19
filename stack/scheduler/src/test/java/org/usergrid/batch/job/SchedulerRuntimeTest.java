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

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.After;
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
  private Properties props;

  @Before
  public void setup() {
    scheduler = CassandraRunner.getBean(SchedulerService.class);

    props = CassandraRunner.getBean("properties", Properties.class);
    
    //start the scheduler after we're all set up
    JobSchedulerService jobScheduler = CassandraRunner.getBean(JobSchedulerService.class);
    if (jobScheduler.state() != State.RUNNING) {
      jobScheduler.startAndWait();
    }

  }
  
  @After
  public void stopScheduler(){
//   We can't stop the scheduler, it won't restart.  This is in the guava code
//    JobSchedulerService jobScheduler = CassandraRunner.getBean(JobSchedulerService.class);
//    jobScheduler.stopAndWait();
    
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
   * Test the scheduler ramps up correctly when there are more jobs to be read
   * after a pause
   * 
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

  /**
   * Test the scheduler ramps up correctly when there are more jobs to be read
   * after a pause
   * 
   * @throws InterruptedException
   */
  @Test
  public void failureCauses() throws InterruptedException {

    long sleepTime = Long.parseLong(props.getProperty("usergrid.scheduler.job.timeout"));

    int failCount = Integer.parseInt(props.getProperty("usergrid.scheduler.job.failure"));
    
    FailureJobExceuction job = CassandraRunner.getBean("failureJobExceuction", FailureJobExceuction.class);
    
    job.reset();

    scheduler.createJob("failureJobExceuction", System.currentTimeMillis(), new JobData());
    
    //sleep until the job should have failed.  We sleep 1 extra cycle just to make sure we're not racing the test
    Thread.sleep(sleepTime*(failCount+1));
    
    assertEquals(failCount+1, job.getCount());
    
  }
  
  /**
   * Test the scheduler ramps up correctly when there are more jobs to be read
   * after a pause when the job specifies the retry time
   * 
   * @throws InterruptedException
   */
  @Test
  public void failureWithRetry() throws InterruptedException {

    long sleepTime = Long.parseLong(props.getProperty("usergrid.scheduler.job.timeout"));

    int failCount = Integer.parseInt(props.getProperty("usergrid.scheduler.job.failure"));
    
    
    long customRetry = sleepTime*2; 
    
    FailureRetryJobExceuction job = CassandraRunner.getBean("failureRetryJobExceuction", FailureRetryJobExceuction.class);
    
    job.setTimeout(customRetry);
    
    scheduler.createJob("failureRetryJobExceuction", System.currentTimeMillis(), new JobData());
    
    //sleep until the job should have failed.  We sleep 1 extra cycle just to make sure we're not racing the test
    Thread.sleep(customRetry*(failCount+1));
    
    assertEquals(failCount+1, job.getCount());
    
  }

}
