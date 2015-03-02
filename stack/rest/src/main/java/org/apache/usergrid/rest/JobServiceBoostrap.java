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
package org.apache.usergrid.rest;

import org.apache.usergrid.batch.service.JobSchedulerService;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.services.notifications.QueueListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Properties;


/**
 * Simple class that starts the job store after the application context has been fired up. We don't
 * want to start the service until all of spring has been initialized in our webapp context
 */
public class JobServiceBoostrap implements
        ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger( JobServiceBoostrap.class );

    public static final String START_SCHEDULER_PROP = "usergrid.scheduler.enabled";

    @Autowired
    private JobSchedulerService jobScheduler;

    @Autowired
    private Properties properties;

    @Autowired
    private QueueListener notificationsQueueListener;

    public JobServiceBoostrap() {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.context.ApplicationListener#onApplicationEvent(org
     * .springframework.context.ApplicationEvent)
     */
    @Override
    public void onApplicationEvent( ContextRefreshedEvent event ) {
        String start = properties.getProperty( START_SCHEDULER_PROP, "true" );
        if ( Boolean.parseBoolean( start ) ) {
            logger.info( "Starting Scheduler Service..." );
            jobScheduler.startAsync();
            jobScheduler.awaitRunning();

        } else {
            logger.info( "Scheduler Service disabled" );
        }

        boolean shouldRun = new Boolean(properties.getProperty("usergrid.notifications.listener.run","true"));
        if(shouldRun){
            notificationsQueueListener.start();
        }else{
            logger.info("QueueListener: never started due to config value usergrid.notifications.listener.run.");
        }

    }

}
