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


import java.util.Properties;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import org.apache.usergrid.batch.service.JobSchedulerService;
import org.apache.usergrid.batch.service.SchedulerService;
import org.apache.usergrid.cassandra.SchemaManager;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.persistence.index.impl.ElasticSearchResource;

import com.google.common.util.concurrent.Service.State;

import net.jcip.annotations.NotThreadSafe;


/**
 * Class to test job runtimes
 */
@NotThreadSafe
public class AbstractSchedulerRuntimeIT {

    protected static final int DEFAULT_COUNT = 10;
    protected static final String COUNT_PROP = AbstractSchedulerRuntimeIT.class.getCanonicalName();
    protected static final String TIMEOUT_PROP = "usergrid.scheduler.job.timeout";
    protected static final String RUNLOOP_PROP = "usergrid.scheduler.job.interval";
    protected static final String FAIL_PROP = "usergrid.scheduler.job.maxfail";


    @ClassRule
    public static SpringResource springResource = SpringResource.getInstance();


    @ClassRule
    public static ElasticSearchResource elasticSearchResource = new ElasticSearchResource();


    private TestJobListener listener = new TestJobListener();
    protected long waitTime = TestJobListener.WAIT_MAX_MILLIS;

    private int count = DEFAULT_COUNT;

    protected SchedulerService scheduler;
    protected Properties props;


    @BeforeClass
    public static void beforeClass() throws Throwable {

//        elasticSearchResource.before();

        SchemaManager sm = springResource.getBean("coreManager", SchemaManager.class);
        sm.create();
        sm.populateBaseData();
    }

    @AfterClass
    public static void afterClass() throws Throwable {
//        elasticSearchResource.after();
    }


    @Before
    @SuppressWarnings( "all" )
    public void setup() {

        props = springResource.getBean( "properties", Properties.class );
        scheduler = springResource.getBean( SchedulerService.class );

        if ( System.getProperties().containsKey( COUNT_PROP ) ) {
            count = Integer.getInteger( System.getProperty( COUNT_PROP ) );
        }

        // start the scheduler after we're all set up
        JobSchedulerService jobScheduler = springResource.getBean( JobSchedulerService.class );
        jobScheduler.setJobListener( listener );
        if ( jobScheduler.state() != State.RUNNING ) {
            jobScheduler.startAsync();
            jobScheduler.awaitRunning();
        }
    }


    protected int getCount() {
        return count;
    }


    protected TestJobListener getJobListener() {
        return listener;
    }
}
