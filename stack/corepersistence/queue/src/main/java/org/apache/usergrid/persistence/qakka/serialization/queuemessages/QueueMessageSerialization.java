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

package org.apache.usergrid.persistence.qakka.serialization.queuemessages;

import org.apache.usergrid.persistence.core.migration.schema.Migration;

import java.util.UUID;


public interface QueueMessageSerialization extends Migration {

    /**
     * Write message to storage.
     * If queueMessageId or createdTime are null, then values will be generated.
     */
    UUID writeMessage(final DatabaseQueueMessage message);

    DatabaseQueueMessage loadMessage(
        final String queueName,
        final String region,
        final Long shardIdOrNull,
        final DatabaseQueueMessage.Type type,
        final UUID queueMessageId);

    void deleteMessage(
        final String queueName,
        final String region,
        final Long shardIdOrNull,
        final DatabaseQueueMessage.Type type,
        final UUID queueMessageId);

    void writeMessageData(final UUID messageId, final DatabaseQueueMessageBody messageBody);

    DatabaseQueueMessageBody loadMessageData(final UUID messageId);

    void deleteMessageData(final UUID messageId);

    /**
     * Write message to inflight table and remove from available table
     */
    void putInflight( DatabaseQueueMessage queueMessage );

    /**
     * Delete all queue messages in the specified queue and in the current "local" region.
     * Impacts messages available and messages inflight.
     * @param queueName Name of queue to clear.
     */
    void deleteAllMessages( String queueName );

    /**
     * Remove message from inflight table, write message to available table.
     */
    void timeoutInflight( DatabaseQueueMessage queueMessage );
}
