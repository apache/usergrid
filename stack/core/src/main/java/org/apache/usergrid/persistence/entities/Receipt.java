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
package org.apache.usergrid.persistence.entities;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.usergrid.persistence.TypedEntity;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.UUID;
import org.apache.usergrid.persistence.annotations.EntityProperty;
import org.mortbay.util.ajax.JSON;

@XmlRootElement
public class Receipt extends TypedEntity {

    public static final String ENTITY_TYPE = "receipt";
    public static final String NOTIFICATION_CONNECTION = "notification";

    /** device id **/
    @EntityProperty
    protected UUID deviceId;

    /** data sent to provider */
    @EntityProperty
    protected Object payload;

    /** Time sent to provider */
    @EntityProperty
    protected Long sent;

    /** Error code */
    @EntityProperty
    protected Object errorCode;

    /** Error message */
    @EntityProperty
    protected String errorMessage;

    /** The push token given by the provider */
    @EntityProperty
    protected String notifierId;

    /**
     * UUID of the Notification that sent this - not a Connection for
     * performance reasons
     */
    @EntityProperty
    protected UUID notificationUUID;

    public Receipt() {
    }

    public Receipt(UUID notificationUUID, String notifierId, Object payload,UUID deviceId) {
        this.notificationUUID = notificationUUID;
        this.notifierId = notifierId;
        HashMap receiptPayload;
        if(! (payload instanceof HashMap) ){
            if(payload instanceof String){
                try {
                    receiptPayload = (HashMap) JSON.parse((String) payload);
                }catch (Exception e){
                    receiptPayload = new HashMap<>();
                    receiptPayload.put("payload", payload);
                }
            }else {
                receiptPayload = new HashMap<>();
                receiptPayload.put("payload", payload);
            }
        }else{
            receiptPayload = (HashMap)payload;
        }
        this.payload = receiptPayload;
        this.setDeviceId(deviceId);
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Long getSent() {
        return sent;
    }

    public void setSent(Long sent) {
        this.sent = sent;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Object getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Object errorCode) {
        this.errorCode = errorCode;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getNotifierId() {
        return notifierId;
    }

    public void setNotifierId(String notifierId) {
        this.notifierId = notifierId;
    }

    public UUID getNotificationUUID() {
        return notificationUUID;
    }

    public void setNotificationUUID(UUID notificationUUID) {
        this.notificationUUID = notificationUUID;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }
}
