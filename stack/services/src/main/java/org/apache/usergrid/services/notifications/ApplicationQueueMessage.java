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

import org.apache.usergrid.mq.Message;

import java.util.UUID;

/**
 * Created by ApigeeCorporation on 9/4/14.
 */
public class ApplicationQueueMessage extends Message {


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


    public static ApplicationQueueMessage generate(Message message) {
        return new ApplicationQueueMessage(UUID.fromString(message.getStringProperty(MESSAGE_PROPERTY_APPLICATION_UUID)), UUID.fromString(message.getStringProperty(MESSAGE_PROPERTY_NOTIFICATION_ID)), UUID.fromString(message.getStringProperty(MESSAGE_PROPERTY_DEVICE_UUID)), message.getStringProperty(MESSAGE_PROPERTY_NOTIFIER_NAME), message.getStringProperty(MESSAGE_PROPERTY_NOTIFIER_ID));
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
