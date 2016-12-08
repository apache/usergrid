/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.qakka.serialization.auditlog;

import java.util.UUID;


public class AuditLog {

    public enum Status { SUCCESS, ERROR };

    public enum Action { SEND, ACK, GET, REQUEUE };

    Action action;
    Status status;
    String queueName;
    String region;
    UUID messageId;

    UUID queueMessageId;
    long transfer_time;

    public AuditLog(
            Action action,
            Status status,
            String queueName,
            String region,
            UUID messageId,
            UUID queueMessageId,
            long transfer_time) {

        this.action = action;
        this.status = status;
        this.queueName = queueName;
        this.region = region;
        this.messageId = messageId;
        this.queueMessageId = queueMessageId;
        this.transfer_time = transfer_time;
    }

    public Action getAction() {
        return action;
    }

    public Status getStatus() {
        return status;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public long getTransfer_time() {
        return transfer_time;
    }

    public void setTransfer_time(long transfer_time) {
        this.transfer_time = transfer_time;
    }

    public UUID getQueueMessageId() {
        return queueMessageId;
    }
}
