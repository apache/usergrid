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

import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;


/**
 * Class to test job runtimes
 * 
 * @author tnine
 */
@Concurrent()
@Ignore( "TODO: Todd fix. Does not reliably pass on our build server." )
public class SchedulerRuntime2IT extends AbstractSchedulerRuntimeIT
{
  /**
   * Test the scheduler ramps up correctly when there are more jobs to be read
   * after a pause
   * 
   * @throws InterruptedException
   */
  @Test
  public void schedulingWithNoJobs() throws InterruptedException {

    int count = 200;

    CountdownLatchJob counterJob = cassandraResource.getBean(CountdownLatchJob.class);
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
