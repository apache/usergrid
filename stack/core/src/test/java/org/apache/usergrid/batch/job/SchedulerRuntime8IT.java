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
package org.apache.usergrid.batch.job;


import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.entities.JobData;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.utils.UUIDUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


/**
 * Class to test job runtimes
 */

@Ignore("These tests no longer work with shared spring context. Need to re-evaluate")
public class SchedulerRuntime8IT extends AbstractSchedulerRuntimeIT {

    /**
     * Test the scheduler ramps up correctly when there are more jobs to be read after a pause
     * when the job specifies the retry time
     */
    @Test
    public void queryAndDeleteJobs() throws Exception {

        CountdownLatchJob job = springResource.getBean( "countdownLatch", CountdownLatchJob.class );

        job.setLatch( 1 );

        // fire the job 30 seconds from now
        long fireTime = System.currentTimeMillis() + 30000;

        UUID notificationId = UUIDUtils.newTimeUUID();

        JobData test = new JobData();
        test.setProperty( "stringprop", "test" );
        test.setProperty( "notificationId", notificationId );

        getJobListener().setExpected( 1 );

        JobData saved = scheduler.createJob( "countdownLatch", fireTime, test );

        scheduler.refreshIndex();

        // now query and make sure it equals the saved value

        Query query = Query.fromQL( "notificationId = " +  notificationId );


        Results r = scheduler.queryJobData( query );

        assertEquals( 1, r.size() );

        assertEquals( saved.getUuid(), r.getEntity().getUuid() );

        // query by uuid
        query = Query.fromQL(  "stringprop = 'test'" );

        r = scheduler.queryJobData( query );

        assertEquals( 1, r.size() );

        assertEquals( saved.getUuid(), r.getEntity().getUuid() );

        // now delete the job

        scheduler.deleteJob( saved.getUuid() );

        scheduler.refreshIndex();

        // sleep until the job should have failed. We sleep 1 extra cycle just to
        // make sure we're not racing the test

        long waitTime = Math.max( 0, fireTime - System.currentTimeMillis() + 1000 );

        boolean waited = getJobListener().blockTilDone( waitTime );

        assertFalse( "Job ran ", waited );
    }
}
