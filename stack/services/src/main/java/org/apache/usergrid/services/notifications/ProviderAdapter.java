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

import java.util.Date;
import java.util.Map;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.persistence.entities.Notifier;
import org.apache.usergrid.services.ServicePayload;

/**
 * To send a Notification, the following methods should be called in this order:
 * 1) testConnection() for each notifier to be used 2) translatePayload() for
 * each payload to be sent 3) sendNotification() for each target 4)
 * doneSendingNotifications() when all #2 have been called If there is an
 * Exception in #1 or #2, you should consider the entire Notification to be
 * invalid. Also, getInactiveDevices() should be called occasionally and may be
 * called at any time.
 */
public interface ProviderAdapter {

    /**
     * test the connection
     * @throws ConnectionException
     */
    public void testConnection() throws ConnectionException;

    /**
     * send a notification
     * @param providerId
     * @param payload
     * @param notification
     * @param tracker
     * @throws Exception
     */
    public void sendNotification(String providerId,  Object payload, Notification notification, TaskTracker tracker)
            throws Exception;

    /**
     * must be called when done calling sendNotification() so any open batches
     * may be closed out
     */
    public void doneSendingNotifications() throws Exception;

    /**
     * remove inactive devices
     * @throws Exception
     */
    public void removeInactiveDevices() throws Exception;

    /**
     * translate payload for each notifier
     * @param payload
     * @return
     * @throws Exception
     */
    public Object translatePayload(Object payload) throws Exception;

    /**
     * Validate payload from services
     * @param payload
     * @throws Exception
     */
    public void validateCreateNotifier(ServicePayload payload) throws Exception;

    /**
     * stop the adapter when you are done, so it can quit processing notifications
     */
    public void stop();

    public Notifier getNotifier();
}
