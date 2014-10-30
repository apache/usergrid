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

import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.services.ServicePayload;
import org.apache.usergrid.services.notifications.apns.APNsAdapter;
import org.apache.usergrid.services.notifications.apns.APNsNotification;

/**
 * Just used for testing. Performance and such.
 */
public class TestAdapter implements ProviderAdapter {

    private static final Logger log = LoggerFactory.getLogger(TestAdapter.class);
    private static final int DELAY = 1; // if delay > 0, uses threadpool
    private final Notifier notifier;

    private ExecutorService pool = null;

    public TestAdapter(Notifier notifier) {
        if (DELAY > 0) {
            pool = Executors
                    .newFixedThreadPool(APNsAdapter.MAX_CONNECTION_POOL_SIZE);
        }
        this.notifier = notifier;
    }

    @Override
    public void testConnection() throws ConnectionException {
    }

    @Override
    public void sendNotification(
            String providerId, 
            final Object payload,
            Notification notification,
            TaskTracker tracker)
            throws Exception {

        final APNsNotification apnsNotification = APNsNotification.create(
                "", payload.toString(), notification, tracker);

       // if (pool == null) {
            apnsNotification.messageSent();

//        } else {
//            pool.submit(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        Thread.sleep(DELAY);
//                        apnsNotification.messageSent();
//                        log.debug("messageSent() - " + payload.toString());
//                    } catch (Exception e) {
//                        log.error("messageSent() returned error", e);
//                    }
//                }
//            });
//        }
    }

    @Override
    public void doneSendingNotifications() throws Exception {
        log.debug("doneSendingNotifications()");
    }

    @Override
    public void removeInactiveDevices() throws Exception {
        log.debug("getInactiveDevices()");
    }

    @Override
    public Object translatePayload(Object payload) throws Exception {
        return payload;
    }

    @Override
    public void validateCreateNotifier(ServicePayload payload) throws Exception {
    }

    @Override
    public void stop() {

    }

    @Override
    public Notifier getNotifier() {
        return notifier;
    }
}
