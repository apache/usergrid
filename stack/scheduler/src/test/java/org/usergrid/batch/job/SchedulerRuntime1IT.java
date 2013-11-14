/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.usergrid.batch.job;


import org.usergrid.cassandra.Concurrent;
import org.usergrid.persistence.entities.JobData;

import org.junit.Test;

import static org.junit.Assert.assertTrue;


/**
 * Class to test job runtimes
 */
@Concurrent
public class SchedulerRuntime1IT extends AbstractSchedulerRuntimeIT {
    @Test
    public void basicScheduling() throws InterruptedException {
        CountdownLatchJob counterJob = cassandraResource.getBean( CountdownLatchJob.class );
        // set the counter job latch size
        counterJob.setLatch( getCount() );

        for ( int i = 0; i < getCount(); i++ ) {
            scheduler.createJob( "countdownLatch", System.currentTimeMillis(), new JobData() );
        }

        // now wait until everything fires or no jobs complete in 5 seconds
        boolean waited = getJobListener().blockTilDone( getCount(), 5000L );
        assertTrue( "Jobs ran", waited );
        assertTrue( getCount() + " successful jobs ran", getCount() == getJobListener().getSuccessCount() );
    }
}
