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
package org.apache.usergrid.services.notifications.gcm;

import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.services.notifications.ConnectionException;
import org.apache.usergrid.services.notifications.NotificationsService;
import org.apache.usergrid.services.notifications.ProviderAdapter;
import org.apache.usergrid.services.notifications.TaskTracker;

import java.util.Date;
import java.util.Map;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.services.ServicePayload;

public class MockSuccessfulProviderAdapter implements ProviderAdapter {

    private static ProviderAdapter realProviderAdapter;



    public MockSuccessfulProviderAdapter() {
    }

    @Override
    public void testConnection() throws ConnectionException {
    }

    @Override
    public String translatePayload(Object payload) throws Exception {
        return payload.toString();
    }



    @Override
    public void validateCreateNotifier(ServicePayload payload) throws Exception {
    }

    @Override
    public void stop() {

    }

    @Override
    public Notifier getNotifier() {
        return null;
    }

    @Override
    public void doneSendingNotifications() throws Exception {
    }

    @Override
    public void removeInactiveDevices() throws Exception {

    }

    @Override
    public void sendNotification(String providerId,
            Object payload, Notification notification, final TaskTracker tracker)
            throws Exception {
        new Thread() {
            @Override
            public void run() {
                try {
                    tracker.completed();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
