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

import com.relayrides.pushy.apns.*;
import com.relayrides.pushy.apns.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a single listener that basically just delegates back to the
 * APNsNotification for handling.
 */
public class RejectedAPNsListener implements RejectedNotificationListener<SimpleApnsPushNotification>{

    @Override
    public void handleRejectedNotification(PushManager<? extends SimpleApnsPushNotification> pushManager, SimpleApnsPushNotification notification, RejectedNotificationReason rejectionReason) {
        try {
            //mark failed for standard notification
            if (notification instanceof APNsNotification) {
                ((APNsNotification) notification).messageSendFailed(rejectionReason);
            }
            //if test getting here means it worked
            if(notification instanceof TestAPNsNotification){
                TestAPNsNotification testAPNsNotification = (TestAPNsNotification) notification;
                testAPNsNotification.setReason(rejectionReason);
                testAPNsNotification.countdown();
                logger.error("Failed to connect to APN's service",testAPNsNotification);
            }

        } catch (Exception e) {
            logger.error("Failed to track rejected listener", e);
        }
        System.out.format("%s was rejected with rejection reason %s\n", notification, rejectionReason);
    }

    private static final Logger logger = LoggerFactory.getLogger(RejectedAPNsListener.class);
}
