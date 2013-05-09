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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.usergrid.batch.service.JobSchedulerService;
import org.usergrid.batch.service.SchedulerService;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.entities.JobData;
import org.usergrid.persistence.entities.JobStat;
import org.usergrid.utils.UUIDUtils;

import com.google.common.util.concurrent.Service.State;

/**
 * Class to test job runtimes
 * 
 * @author tnine
 * 
 */
@RunWith(CassandraRunner.class)
public class SchedulerRuntimeTest {

  /**
   * 
   */
  private static final String FAIL_PROP = "usergrid.scheduler.job.maxfail";
  /**
   * 
   */
  private static final String TIMEOUT_PROP = "usergrid.scheduler.job.timeout";

  private SchedulerService scheduler;
  private Properties props;
  private EntityManagerFactory emf;

  @Before
  public void setup() {
    scheduler = CassandraRunner.getBean(SchedulerService.class);

    props = CassandraRunner.getBean("properties", Properties.class);

    emf = CassandraRunner.getBean(EntityManagerFactory.class);

    // start the scheduler after we're all set up
    JobSchedulerService jobScheduler = CassandraRunner.getBean(JobSchedulerService.class);
    if (jobScheduler.state() != State.RUNNING) {
      jobScheduler.startAndWait();
    }

  }

  @After
  public void stopScheduler() {
    // We can't stop the scheduler, it won't restart. This is in the guava code
    // JobSchedulerService jobScheduler =
    // CassandraRunner.getBean(JobSchedulerService.class);
    // jobScheduler.stopAndWait();

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
   * @throws Exception
   */
  @Test
  public void failureCausesJobDeath() throws Exception {

    int failCount = Integer.parseInt(props.getProperty(FAIL_PROP));

    FailureJobExceuction job = CassandraRunner.getBean("failureJobExceuction", FailureJobExceuction.class);

    job.setLatch(failCount + 1);

    JobData returned = scheduler.createJob("failureJobExceuction", System.currentTimeMillis(), new JobData());

    // sleep until the job should have failed. We sleep 1 extra cycle just to
    // make sure we're not racing the test
    boolean waited = job.waitForCount(60, TimeUnit.SECONDS);

    assertTrue("Job ran to failure", waited);

    JobStat stat = scheduler.getStatsForJob(returned.getJobName(), returned.getUuid());

    // we should have only marked this as run fail+1 times
    assertEquals(failCount + 1, stat.getTotalAttempts());
    assertEquals(failCount+1, stat.getRunCount());
    assertEquals(0, stat.getDelayCount());
    
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

    DelayExecution job = CassandraRunner.getBean("delayExecution", DelayExecution.class);

    job.setTimeout(customRetry);
    job.setLatch(delayCount+1);

    JobData returned = scheduler.createJob("delayExecution", System.currentTimeMillis(), new JobData());

    // sleep until the job should have failed. We sleep 1 extra cycle just to
    // make sure we're not racing the test
    boolean waited = job.waitForCount(customRetry * (delayCount *2 ), TimeUnit.MILLISECONDS);

    assertTrue("Job ran to failure", waited);

    JobStat stat = scheduler.getStatsForJob(returned.getJobName(), returned.getUuid());

    // we should have only marked this as run once since we delayed furthur execution
    // we should have only marked this as run once
    assertEquals(1, stat.getTotalAttempts());
    assertEquals(delayCount+1, stat.getRunCount());
    assertEquals(delayCount, stat.getDelayCount());
  }
  
  
  
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

    OnlyOnceExceution job = CassandraRunner.getBean("onlyOnceExceution", OnlyOnceExceution.class);

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
  

  /**
   * Test the scheduler ramps up correctly when there are more jobs to be read
   * after a pause when the job specifies the retry time
   * 
   * @throws Exception
   */
  @Test
  public void onlyOnceTestOnException() throws Exception {

    long sleepTime = Long.parseLong(props.getProperty(TIMEOUT_PROP));

    long customRetry = sleepTime * 2;
    int numberOfRuns = 2;

    OnlyOnceUnlockOnFailExceution job = CassandraRunner.getBean("onlyOnceUnlockOnFailExceution", OnlyOnceUnlockOnFailExceution.class);

    job.setTimeout(customRetry);
    job.setLatch(numberOfRuns);
    job.setDelay(sleepTime);

    JobData returned = scheduler.createJob("onlyOnceUnlockOnFailExceution", System.currentTimeMillis(), new JobData());

    // sleep until the job should have failed. We sleep 1 extra cycle just to
    // make sure we're not racing the test
    boolean waited = job.waitForException(customRetry * numberOfRuns *2 , TimeUnit.MILLISECONDS);

    assertTrue("Job threw exception", waited);
    
    
    //wait for the persistence to store the failure
   
    Thread.sleep(3000);
    
    JobStat stat = scheduler.getStatsForJob(returned.getJobName(), returned.getUuid());

    // we should have only marked this as run once since we delayed furthur execution
    // we should have only marked this as run once
    assertNotNull(stat);
    assertEquals(1, stat.getTotalAttempts());
    assertEquals(1, stat.getRunCount());
    assertEquals(0, stat.getDelayCount());
    
    
    
    //now wait again to see if the job fires one more time, it shouldn't 
    waited = job.waitForCount(customRetry * numberOfRuns *2,TimeUnit.MILLISECONDS);
    
    assertTrue("Job ran twice", waited);
    
    stat = scheduler.getStatsForJob(returned.getJobName(), returned.getUuid());

    // we should have only marked this as run once since we delayed furthur execution
    // we should have only marked this as run once
    assertEquals(numberOfRuns, stat.getTotalAttempts());
    assertEquals(numberOfRuns, stat.getRunCount());
    assertEquals(0, stat.getDelayCount());
    
  }

  /**
   * Test the scheduler ramps up correctly when there are more jobs to be read
   * after a pause when the job specifies the retry time
   * 
   * @throws Exception
   */
  @Test
  public void queryAndDeleteJobs() throws Exception {

    CountdownLatchJob job = CassandraRunner.getBean("countdownLatch", CountdownLatchJob.class);

    job.setLatch(1);

    // fire the job 30 seconds from now
    long fireTime = System.currentTimeMillis() + 30000;

    UUID notificationId = UUIDUtils.newTimeUUID();

    JobData test = new JobData();
    test.setProperty("stringprop", "test");
    test.setProperty("notificationId", notificationId);

    JobData saved = scheduler.createJob("countdownLatch", fireTime, test);

    // now query and make sure it equals the saved value

    Query query = new Query();
    query.addEqualityFilter("notificationId", notificationId);

    Results r = scheduler.queryJobData(query);

    assertEquals(1, r.size());

    assertEquals(saved.getUuid(), r.getEntity().getUuid());

    // query by uuid
    query = new Query();
    query.addEqualityFilter("stringprop", "test");

    r = scheduler.queryJobData(query);

    assertEquals(1, r.size());

    assertEquals(saved.getUuid(), r.getEntity().getUuid());

    // now delete the job

    scheduler.deleteJob(saved.getUuid());

    // sleep until the job should have failed. We sleep 1 extra cycle just to
    // make sure we're not racing the test

    long waitTime = Math.max(0, fireTime - System.currentTimeMillis() + 1000);

    boolean waited = job.waitForCount(waitTime, TimeUnit.MILLISECONDS);

    assertFalse("Job ran ", waited);

  }

}
