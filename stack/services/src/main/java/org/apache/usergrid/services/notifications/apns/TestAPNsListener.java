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
package org.apache.usergrid.services.notifications.apns;

import com.relayrides.pushy.apns.ApnsConnection;
import com.relayrides.pushy.apns.ApnsConnectionListener;
import com.relayrides.pushy.apns.RejectedNotificationReason;
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

public class TestAPNsListener implements ApnsConnectionListener<SimpleApnsPushNotification> {

    private final CountDownLatch latch;

    private boolean connectionFailed = false;
    private boolean connectionClosed = false;

    private Throwable connectionFailureCause;

    private final ArrayList<SimpleApnsPushNotification> writeFailures = new ArrayList<SimpleApnsPushNotification>();

    private SimpleApnsPushNotification rejectedNotification;
    private RejectedNotificationReason rejectionReason;

    private final ArrayList<SimpleApnsPushNotification> unprocessedNotifications = new ArrayList<SimpleApnsPushNotification>();

    public TestAPNsListener() {
        this.latch = new CountDownLatch(1);
    }

    public void handleConnectionSuccess(final ApnsConnection<SimpleApnsPushNotification> connection) {
        latch.countDown();
    }

    public void handleConnectionFailure(final ApnsConnection<SimpleApnsPushNotification> connection, final Throwable cause) {
        this.connectionFailed = true;
        this.connectionFailureCause = cause;
        latch.countDown();
    }

    public void handleConnectionClosure(ApnsConnection<SimpleApnsPushNotification> connection) {
        try {
            connection.waitForPendingWritesToFinish();
        } catch (InterruptedException ignored) {
        }
        this.connectionClosed = true;
        latch.countDown();
    }

    public void handleWriteFailure(ApnsConnection<SimpleApnsPushNotification> connection,
                                   SimpleApnsPushNotification notification, Throwable cause) {

        this.writeFailures.add(notification);
    }

    public void handleRejectedNotification(ApnsConnection<SimpleApnsPushNotification> connection,
                                           SimpleApnsPushNotification rejectedNotification, RejectedNotificationReason reason) {

        this.rejectedNotification = rejectedNotification;
        this.rejectionReason = reason;
    }

    public void handleUnprocessedNotifications(ApnsConnection<SimpleApnsPushNotification> connection,
                                               Collection<SimpleApnsPushNotification> unprocessedNotifications) {

        this.unprocessedNotifications.addAll(unprocessedNotifications);
    }

    public void handleConnectionWritabilityChange(ApnsConnection<SimpleApnsPushNotification> connection, boolean writable) {
    }

    public boolean hasConnectionFailed() {
        return connectionFailed;
    }

    public Throwable getConnectionFailureCause(){
        return connectionFailureCause;
    }

    public CountDownLatch getLatch() {
        return latch;
    }
}