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

import com.google.inject.Injector;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.queue.QueueManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.batch.job.OnlyOnceJob;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.entities.JobData;
import org.apache.usergrid.services.ServiceManager;
import org.apache.usergrid.services.ServiceManagerFactory;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;


@Component( "queueJob" )
public class QueueJob extends OnlyOnceJob {

    private static final Logger logger = LoggerFactory.getLogger( QueueJob.class );

    private MetricsFactory metricsService;

    @Autowired
    private ServiceManagerFactory smf;

    static final long BATCH_DEATH_PERIOD = 10 * 60 * 1000;

    @Autowired
    private EntityManagerFactory emf;
    private Histogram histogram;
    private Meter requests;
    private Timer execution;


    public QueueJob() {
        logger.info( "QueueJob created: " + this );
    }


    @PostConstruct
    void init() {
        metricsService = this.smf.getApplicationContext().getBean( Injector.class ).getInstance(MetricsFactory.class);
        histogram = metricsService.getHistogram( QueueJob.class, "queuejob.cycle" );
        requests = metricsService.getMeter( QueueJob.class, "queuejob.requests" );
        execution = metricsService.getTimer( QueueJob.class, "queuejob.execution" );
    }


    @Override
    public void doJob( JobExecution jobExecution ) throws Exception {
        Timer.Context timer = execution.time();
        requests.mark();
        logger.info( "execute QueueJob {}", jobExecution );

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
                notificationsService.getQueueManager().queueNotification( notification, jobExecution );
            }
            catch ( Exception e ) {
                logger.error( "execute QueueJob failed", e );
                em.setProperty( notification, "errorMessage", e.getMessage() );
                throw e;
            }
            finally {
                long diff = System.currentTimeMillis() - notification.getCreated();
                histogram.update( diff );
            }
        }
        finally {
            timer.stop();
        }

        logger.info( "execute QueueJob completed normally" );
    }


    @Override
    protected long getDelay( JobExecution execution ) throws Exception {
        return BATCH_DEATH_PERIOD;
    }


    @Override
    public void dead( final JobExecution execution ) throws Exception {

    }
}
