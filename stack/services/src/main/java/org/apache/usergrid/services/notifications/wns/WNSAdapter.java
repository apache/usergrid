/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.services.notifications.wns;

import ar.com.fernandospr.wns.WnsService;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.services.ServicePayload;
import org.apache.usergrid.services.notifications.ConnectionException;
import org.apache.usergrid.services.notifications.ProviderAdapter;
import org.apache.usergrid.services.notifications.TaskTracker;

/**
 * Windows Notifications Service adapter to send windows notifications
 */
public class WNSAdapter implements ProviderAdapter {


    private final EntityManager entityManager;


    public WNSAdapter(EntityManager entityManager, Notifier notifier) {
        this.entityManager = entityManager;
        WnsService service = new WnsService(notifier.getSid(), notifier.getApiKey(), notifier.getLogging());
    }

    @Override
    public void testConnection() throws ConnectionException {

    }

    @Override
    public void sendNotification(String providerId, Object payload, Notification notification, TaskTracker tracker) throws Exception {

    }

    @Override
    public void doneSendingNotifications() throws Exception {

    }

    @Override
    public void removeInactiveDevices() throws Exception {

    }

    @Override
    public Object translatePayload(Object payload) throws Exception {
        return null;
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
}
