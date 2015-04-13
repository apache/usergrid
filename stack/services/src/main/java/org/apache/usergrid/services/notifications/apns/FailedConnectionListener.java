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
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Provides a single listener that basically just delegates back to the
 * APNsNotification for handling.
 */
public class FailedConnectionListener implements com.relayrides.pushy.apns.FailedConnectionListener<SimpleApnsPushNotification> {

    private static final Logger logger = LoggerFactory
            .getLogger(RejectedAPNsListener.class);

    @Override
    public void handleFailedConnection(PushManager<? extends SimpleApnsPushNotification> pushManager, Throwable cause) {
        List<SimpleApnsPushNotification> notifications = new ArrayList<SimpleApnsPushNotification>();
        if (cause instanceof SSLException || cause instanceof SSLHandshakeException || cause instanceof ClosedChannelException) { //cert is probably bad so shut it down.
            if (!pushManager.isShutDown()) {
                pushManager.unregisterFailedConnectionListener(this);

                try {
                    BlockingQueue notificationQueue =  pushManager.getQueue();
                    if(notificationQueue !=null){
                        LinkedBlockingQueue<SimpleApnsPushNotification>  queue =  ( LinkedBlockingQueue<SimpleApnsPushNotification> )notificationQueue;
                        Object[] objectMess = queue.toArray(); //get messages still in queue
                        for(Object o : objectMess){
                            if(o instanceof SimpleApnsPushNotification) {
                                notifications.add((SimpleApnsPushNotification) o);
                            }
                        }
                    }
                    pushManager.shutdown();
                } catch (InterruptedException ie) {
                    logger.error("Failed to stop push services", ie);
                }
            } else {
                return;
            }
        }
        //mark all unsent notifications failed
        if (notifications != null) {
                notifications.forEach(notification -> {
                    if (notification instanceof APNsNotification) {
                        try {
                            ((APNsNotification) notification).messageSendFailed(cause);//mark failed with bad token
                        } catch (Exception e) {
                            logger.error("failed to track notification in failed connection listener", e);
                        }
                    }
                    //if test this is a problem because you can't connect
                    if (notification instanceof TestAPNsNotification) {
                        TestAPNsNotification testAPNsNotification = ((TestAPNsNotification) notification);
                        testAPNsNotification.setReason(cause);
                        testAPNsNotification.countdown();
                    }

                });
            pushManager.getQueue().clear();
        }
        logger.error("Failed to register push connection", cause);
    }
}
