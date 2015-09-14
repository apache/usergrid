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

import com.relayrides.pushy.apns.RejectedNotificationReason;
import com.relayrides.pushy.apns.util.MalformedTokenStringException;
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification;
import com.relayrides.pushy.apns.util.TokenUtil;

import org.apache.usergrid.persistence.entities.Notification;
import org.apache.usergrid.services.notifications.TaskTracker;

import java.util.Date;
/**
 * Standard apigee notificatton
 */
public class APNsNotification extends SimpleApnsPushNotification {


    private TaskTracker tracker;

    /**
     * Factory method
     * @param providerId token for device
     * @param payload body
     * @param notification notification entity
     * @param tracker tracks completion
     * @return
     */
    public static APNsNotification create(String providerId, String payload, Notification notification, TaskTracker tracker) throws RuntimeException {

        // create Date object using milliseconds value
        Date expiryDate = new Date(notification.getExpireTimeMillis());

      try {
          final byte[] token = TokenUtil.tokenStringToByteArray(providerId);

          return new APNsNotification(tracker, expiryDate, token, payload, notification);
      }catch(MalformedTokenStringException mtse){
          throw new RuntimeException("Exception converting token",mtse);
      }
    }

    /**
     * Default constructor
     * @param tracker
     * @param expiryTime
     * @param token
     * @param payload
     */
    public APNsNotification(TaskTracker tracker, Date expiryTime, byte[] token, String payload,Notification notification) {
        super(token, payload, expiryTime);
        this.tracker = tracker;
    }

    /**
     * mark message sent
     * @throws Exception
     */
    public void messageSent() throws Exception {
        if (tracker != null) {
            tracker.completed();
        }
    }

    /**
     * mark message failed
     *
     * @throws Exception
     */
    public void messageSendFailed(RejectedNotificationReason reason) throws Exception {
        if (tracker != null) {
            tracker.failed(reason.name(), "Failed sending notification.");
        }
    }

    /**
     * mark message failed, from exception
     * @param cause
     * @throws Exception
     */
    public void messageSendFailed(Throwable cause) throws Exception {
        if (tracker != null) {
            tracker.failed(cause.getClass().getSimpleName(), cause.getMessage());
        }
    }
}
