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
import org.junit.Before;
import org.usergrid.SchedulerITSuite;
import org.usergrid.batch.service.JobSchedulerService;
import org.usergrid.batch.service.SchedulerService;
import org.usergrid.cassandra.CassandraResource;

import java.util.Properties;


/**
 * Class to test job runtimes
 * 
 * @author tnine
 */
public class AbstractSchedulerRuntimeIT
{
  public static CassandraResource cassandraResource = SchedulerITSuite.cassandraResource;
  protected SchedulerService scheduler;
  protected Properties props;

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
}
