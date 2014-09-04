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
package org.apache.usergrid.services.notifications;

import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.usergrid.persistence.entities.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.batch.job.OnlyOnceJob;
import org.apache.usergrid.metrics.MetricsFactory;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.entities.JobData;
import org.apache.usergrid.services.ServiceManager;
import org.apache.usergrid.services.ServiceManagerFactory;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;


@Component( "notificationJob" )
public class NotificationJob extends OnlyOnceJob {


    private static final Logger logger = LoggerFactory.getLogger( NotificationJob.class );

    public NotificationJob() {

    }


    @PostConstruct
    void init() {
    }


    @Override
    public void doJob( JobExecution jobExecution ) throws Exception {

        logger.info( "execute NotificationJob is deprecated " );
    }


    @Override
    protected long getDelay( JobExecution execution ) throws Exception {
        return TaskManager.BATCH_DEATH_PERIOD;
    }


    @Override
    public void dead( final JobExecution execution ) throws Exception {

    }
}
