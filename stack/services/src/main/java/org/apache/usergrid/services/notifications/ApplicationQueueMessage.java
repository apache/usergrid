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

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.apache.usergrid.mq.Message;

import java.util.UUID;
import org.elasticsearch.common.primitives.Longs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ApigeeCorporation on 9/4/14.
 */
public class ApplicationQueueMessage implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(ApplicationQueueMessage.class);
    private UUID applicationId;
    private UUID notificationId;
    private UUID deviceId;
    private String notifierKey;
    private String notifierId;


    public ApplicationQueueMessage() {
    }

    public ApplicationQueueMessage(UUID applicationId, UUID notificationId, UUID deviceId, String notifierKey, String notifierId) {
        this.applicationId = applicationId;
        this.notificationId = notificationId;
        this.deviceId = deviceId;
        this.notifierKey = notifierKey;
        this.notifierId = notifierId;
        setNotificationId(notificationId);
        setNotifierKey(notifierKey);
        setNotifierId(notifierId);
    }

    public static UUID bytesToUuid( byte[] sixteenBytes ) {
        byte[] msBytes = Arrays.copyOfRange( sixteenBytes, 0, 8 );
        byte[] lsBytes = Arrays.copyOfRange( sixteenBytes, 8, 16 );
        long msb = Longs.fromByteArray( msBytes ); 
        long lsb = Longs.fromByteArray( lsBytes ); 
        return new UUID( msb, lsb );
    }


    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
       this.applicationId = applicationId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
       this.notificationId = notificationId;
    }

    public String getNotifierId() {
        return notifierId;
    }

    public void setNotifierId(String notifierId) {
         this.notifierId = notifierId;
    }

    public String getNotifierKey() {
        return notifierKey;
    }

    public void setNotifierKey(String name) {
        notifierKey = name;
    }


}
