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
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.entities.JobData;
import org.usergrid.utils.UUIDUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;


/**
 * Class to test job runtimes
 * 
 * @author tnine
 */
@Concurrent()
@Ignore( "TODO: Todd fix. Does not reliably pass on our build server." )
public class SchedulerRuntime8IT extends AbstractSchedulerRuntimeIT
{
  /**
   * Test the scheduler ramps up correctly when there are more jobs to be read
   * after a pause when the job specifies the retry time
   * 
   * @throws Exception
   */
  @Test
  public void queryAndDeleteJobs() throws Exception {

    CountdownLatchJob job = cassandraResource.getBean("countdownLatch", CountdownLatchJob.class);

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
