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
public class ApplicationQueueMessage extends Message {

    private static final Logger log = LoggerFactory.getLogger(ApplicationQueueMessage.class);

    static final String MESSAGE_PROPERTY_DEVICE_UUID = "deviceUUID";
    static final String MESSAGE_PROPERTY_APPLICATION_UUID = "applicationUUID";
    static final String MESSAGE_PROPERTY_NOTIFIER_ID = "notifierId";
    static final String MESSAGE_PROPERTY_NOTIFICATION_ID = "notificationId";
    static final String MESSAGE_PROPERTY_NOTIFIER_NAME = "notifierName";


    public ApplicationQueueMessage() {
    }

    public ApplicationQueueMessage(UUID applicationId, UUID notificationId, UUID deviceId, String notifierKey, String notifierId) {
        setApplicationId(applicationId);
        setDeviceId(deviceId);
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

    public static ApplicationQueueMessage generate(Message message) {

        // this crazyness may indicate that Core Persistence is not storing UUIDs correctly

        byte[] mpaBytes = (byte[])message.getObjectProperty(MESSAGE_PROPERTY_APPLICATION_UUID);
        UUID mpaUuid = bytesToUuid(mpaBytes);

        byte[] mpnBytes = (byte[])message.getObjectProperty(MESSAGE_PROPERTY_NOTIFICATION_ID);
        UUID mpnUuid = bytesToUuid(mpnBytes);

        final UUID mpdUuid;
        Object o = message.getObjectProperty(MESSAGE_PROPERTY_DEVICE_UUID);
        if ( o instanceof UUID ) {
            mpdUuid = (UUID)message.getObjectProperty(MESSAGE_PROPERTY_DEVICE_UUID);
        } else {
            byte[] mpdBytes = (byte[])o;
            mpdUuid =  bytesToUuid(mpdBytes);
        }

        // end of crazyness

        return new ApplicationQueueMessage(
                mpaUuid, mpnUuid, mpdUuid,
                message.getStringProperty(MESSAGE_PROPERTY_NOTIFIER_NAME), 
                message.getStringProperty(MESSAGE_PROPERTY_NOTIFIER_ID));
    }

    public UUID getApplicationId() {
        return (UUID) this.getObjectProperty(MESSAGE_PROPERTY_APPLICATION_UUID);
    }

    public void setApplicationId(UUID applicationId) {
        this.setProperty(MESSAGE_PROPERTY_APPLICATION_UUID, applicationId);
    }

    public UUID getDeviceId() {
        return (UUID) this.getObjectProperty(MESSAGE_PROPERTY_DEVICE_UUID);
    }

    public void setDeviceId(UUID deviceId) {
        this.setProperty(MESSAGE_PROPERTY_DEVICE_UUID, deviceId);
    }

    public UUID getNotificationId() {
        return (UUID) this.getObjectProperty(MESSAGE_PROPERTY_NOTIFICATION_ID);
    }

    public void setNotificationId(UUID notificationId) {
        this.setProperty(MESSAGE_PROPERTY_NOTIFICATION_ID, notificationId);
    }

    public String getNotifierId() {
        return this.getStringProperty(MESSAGE_PROPERTY_NOTIFIER_ID);
    }

    public void setNotifierId(String notifierId) {
        this.setProperty(MESSAGE_PROPERTY_NOTIFIER_ID, notifierId);
    }

    public String getNotifierKey() {
        return this.getStringProperty(MESSAGE_PROPERTY_NOTIFIER_NAME);
    }

    public void setNotifierKey(String name) {
        this.setProperty(MESSAGE_PROPERTY_NOTIFIER_NAME, name);
    }


}
