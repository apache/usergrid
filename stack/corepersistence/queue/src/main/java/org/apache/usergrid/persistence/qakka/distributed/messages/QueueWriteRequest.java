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

package org.apache.usergrid.persistence.qakka.distributed.messages;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.UUID;


public class QueueWriteRequest implements QakkaMessage {

    private final String queueName;
    private final String sourceRegion;
    private final String destRegion;
    private final UUID messageId;
    private Long deliveryTime;
    private Long expirationTime;


    public QueueWriteRequest(
            String queueName, String sourceRegion, String destRegion, UUID messageId,
            Long deliveryTime, Long expirationTime) {

        this.queueName = queueName;
        this.sourceRegion = sourceRegion;
        this.destRegion = destRegion;
        this.messageId = messageId;
        this.deliveryTime = deliveryTime;
        this.expirationTime = expirationTime;
    }

    public String getQueueName() {
        return queueName;
    }

    public String getSourceRegion() {
        return sourceRegion;
    }

    public String getDestRegion() {
        return destRegion;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public Long getDeliveryTime() {
        return deliveryTime;
    }

    public String toString() {
        return new ToStringBuilder( this )
                .append( "queueName", queueName )
                .append( "sourceRegion", sourceRegion )
                .append( "destRegion", destRegion )
                .append( "messageId", messageId )
                .append( "expirationTime", expirationTime )
                .append( "deliveryTime", deliveryTime )
                .toString();
    }

}
