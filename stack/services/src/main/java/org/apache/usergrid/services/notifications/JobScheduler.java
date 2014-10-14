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

import org.apache.usergrid.batch.service.SchedulerService;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.JobData;
import org.apache.usergrid.services.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class JobScheduler{
    public static final long SCHEDULER_GRACE_PERIOD = 250;
    private final EntityManager em;

    protected ServiceManager sm;
    private final Logger LOG = LoggerFactory.getLogger(NotificationsService.class);

    public JobScheduler(ServiceManager sm,EntityManager em){
        this.sm=sm; this.em = em;
    }
    public void scheduleBatchJob(Notification notification, long delay) throws Exception {

        JobData jobData = new JobData();
        jobData.setProperty("applicationId", sm.getApplicationId());
        jobData.setProperty("notificationId", notification.getUuid());
        jobData.setProperty("deliver", notification.getDeliver());

        long soonestPossible = System.currentTimeMillis() + SCHEDULER_GRACE_PERIOD + delay;

        SchedulerService scheduler = getSchedulerService();
        scheduler.createJob("notificationBatchJob", soonestPossible, jobData);

        LOG.info("notification {} batch scheduled for delivery", notification.getUuid());
    }
    public boolean scheduleQueueJob(Notification notification) throws Exception {
        return scheduleQueueJob(notification,false);
    }

    public boolean scheduleQueueJob(Notification notification, boolean forceSchedule) throws Exception {

        boolean scheduleInFuture = notification.getDeliver() != null;
        long scheduleAt = (notification.getDeliver() != null) ? notification.getDeliver() : 0;
        long soonestPossible = System.currentTimeMillis() + SCHEDULER_GRACE_PERIOD;
        if (scheduleAt < soonestPossible) {
            scheduleAt = soonestPossible;
            scheduleInFuture = false;
        }

        boolean scheduled = scheduleInFuture || forceSchedule;
        if(scheduled) {
            JobData jobData = new JobData();
            jobData.setProperty("applicationId", sm.getApplicationId());
            jobData.setProperty("notificationId", notification.getUuid());
            jobData.setProperty("deliver", notification.getDeliver());
            SchedulerService scheduler = getSchedulerService();
            scheduler.createJob("queueJob", scheduleAt, jobData);
            LOG.info("notification {} scheduled for queuing", notification.getUuid());
        }
        return scheduled;
    }
    private SchedulerService getSchedulerService() {
        return sm.getSchedulerService();
    }

}