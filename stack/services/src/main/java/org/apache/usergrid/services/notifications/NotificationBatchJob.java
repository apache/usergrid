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

import org.apache.usergrid.batch.Job;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.metrics.MetricsFactory;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.entities.JobData;
import org.apache.usergrid.services.ServiceManager;
import org.apache.usergrid.services.ServiceManagerFactory;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;


@Component( "notificationBatchJob" )
public class NotificationBatchJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger( NotificationBatchJob.class );
    private Meter postSendMeter;
    private Timer timerMetric;
    private Meter requestMeter;
    private Histogram cycleMetric;

    @Autowired
    private MetricsFactory metricsService;

    @Autowired
    private ServiceManagerFactory smf;

    @Autowired
    private EntityManagerFactory emf;


    public NotificationBatchJob() {

    }


    @PostConstruct
    void init() {
        postSendMeter = metricsService.getMeter( NotificationsService.class, "post-send" );
        requestMeter = metricsService.getMeter( NotificationBatchJob.class, "requests" );
        timerMetric = metricsService.getTimer( NotificationBatchJob.class, "execution" );
        cycleMetric = metricsService.getHistogram( NotificationsService.class, "cycle" );
    }


    public void execute( JobExecution jobExecution ) throws Exception {

        Timer.Context timer = timerMetric.time();
        requestMeter.mark();
        logger.info( "execute NotificationBatchJob {}", jobExecution );

        JobData jobData = jobExecution.getJobData();
        UUID applicationId = ( UUID ) jobData.getProperty( "applicationId" );
        ServiceManager sm = smf.getServiceManager( applicationId );
        NotificationsService notificationsService = ( NotificationsService ) sm.getService( "notifications" );

        EntityManager em = emf.getEntityManager( applicationId );

        try {
            if ( em == null ) {
                logger.info( "no EntityManager for applicationId  {}", applicationId );
                return;
            }

            UUID notificationId = ( UUID ) jobData.getProperty( "notificationId" );
            Notification notification = em.get( notificationId, Notification.class );
            if ( notification == null ) {
                logger.info( "notificationId {} no longer exists", notificationId );
                return;
            }


            try {
                notificationsService.getQueueManager().processBatchAndReschedule( notification, jobExecution );
            }
            catch ( Exception e ) {
                logger.error( "execute NotificationBatchJob failed", e );
                em.setProperty( notification, "errorMessage", e.getMessage() );
                throw e;
            }
            finally {
                long diff = System.currentTimeMillis() - notification.getCreated();
                cycleMetric.update( diff );
                postSendMeter.mark();
            }
        }
        finally {
            timer.stop();
        }

        logger.info( "execute NotificationBatch completed normally" );
    }


    @Override
    public void dead( final JobExecution execution ) throws Exception {

    }
}
