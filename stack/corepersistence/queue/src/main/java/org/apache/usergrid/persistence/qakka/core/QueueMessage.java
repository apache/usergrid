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

package org.apache.usergrid.persistence.qakka.core;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.UUID;

@XmlRootElement
public class QueueMessage implements Serializable {

    private UUID queueMessageId;
    private UUID messageId;

    private String queueName;
    private String sendingRegion;
    private String receivingRegion;

    private Long delayUntilDate;
    private Long expirationDate;
    private Long createDate;
    private Long retries;

    private Boolean dataReceived;

    /** MIME content type of data */
    private String contentType;

    /** If contentType is application/json then data will be the JSON payload for this queue message */
    private String data;

    /** If contentType is not then href will be the URL where the payload may be fetched */
    private String href;


    public QueueMessage() {} // for Jackson

    public QueueMessage(
            UUID queueMessageId, String queueName, String sendingRegion, String receivingRegion, UUID messageId,
            Long delayUntilDate, Long expirationDate, Long createDate, Long retries, Boolean dataReceived) {

        if (queueMessageId == null) {
            this.queueMessageId = QakkaUtils.getTimeUuid();
        } else {
            this.queueMessageId = queueMessageId;
        }
        this.queueName        = queueName;
        this.sendingRegion    = sendingRegion;
        this.receivingRegion  = receivingRegion;
        this.messageId        = messageId;
        this.delayUntilDate   = delayUntilDate;
        this.expirationDate   = expirationDate;
        this.createDate       = createDate;
        this.retries          = retries;
        this.dataReceived     = dataReceived;
    }

    public UUID getQueueMessageId() {
        return queueMessageId;
    }

    public void setQueueMessageId(UUID queueMessageId) {
        this.queueMessageId = queueMessageId;
    }

    public String getQueueName() {
        return queueName;
    }

    public String getSendingRegion() {
        return sendingRegion;
    }

    public String getReceivingRegion() {
        return receivingRegion;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public Long getDelayUntilDate() {
        return delayUntilDate;
    }

    public Long getDelayUntilMs() {
        if ( delayUntilDate == null ) {
            return null;
        }
        return delayUntilDate - System.currentTimeMillis();
    }

    public void setDelayUntilDate(Long delayUntilDate) {
        this.delayUntilDate = delayUntilDate;
    }

    public void setDelayUntilMs(Long delayMs) {
        this.delayUntilDate = System.currentTimeMillis() + delayMs;
    }

    public Long getExpirationDate() {
        return expirationDate;
    }

    public Long getExpirationMs() {
        if ( expirationDate == null ) {
            return null;
        }
        return expirationDate - System.currentTimeMillis();
    }

    public void setExpirationDate(Long expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setExpirationMs(Long expirationMs) {
        this.expirationDate = System.currentTimeMillis() + expirationMs;
    }

    public Long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Long createDate) {
        this.createDate = createDate;
    }

    public Long getRetries() {
        return retries;
    }

    public void setRetries(Long retries) {
        this.retries = retries;
    }

    public Boolean getDataReceived() {
        return dataReceived;
    }

    public void setDataReceived(Boolean dataReceived) {
        this.dataReceived = dataReceived;
    }


    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

}
