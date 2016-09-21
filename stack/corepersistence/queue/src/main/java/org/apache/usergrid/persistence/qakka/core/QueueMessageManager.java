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

import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;


public interface QueueMessageManager {

    /**
     * Send Queue Message to one or more destination regions.
     *  @param queueName Name of queue
     * @param destinationRegions List of destination regions
     * @param delayMs Delay before sending queue message
     * @param expirationSecs Time before message expires
     * @param contentType Content type of message data
     * @param messageData Message content
     */
    void sendMessages(String queueName, List<String> destinationRegions,
                      Long delayMs, Long expirationSecs, String contentType, ByteBuffer messageData);

    /**
     * Get next available messages from the specified queue.
     *
     * @param queueName Name of queue
     * @param count Number of messages to get
     * @return List of next messages, empty if non-available
     */
    List<QueueMessage> getNextMessages(String queueName, int count);

    /**
     * Acknowledge that message has been received and is no longer inflight.
     *
     * @param queueName Name of queue
     * @param queueMessageId ID of queue message
     */
    void ackMessage(String queueName, UUID queueMessageId);

    /**
     * Put message back in the queue.
     *
     * @param queueName Name of the queue
     * @param messageId ID of the queue message
     * @param delayMs Delay before re-queueing message
     */
    void requeueMessage(String queueName, UUID messageId, Long delayMs);

    /**
     * Clear all messages from queue
     *
     * @param queueName Name of queue
     */
    void clearMessages(String queueName);

    /**
     * Get message payload data.
     */
    ByteBuffer getMessageData(UUID messageId);

    /**
     * Get message from messages available or messages inflight storage.
     */
    QueueMessage getMessage(String queueName, UUID queueMessageId);

    long getQueueDepth(String queueName);
}
