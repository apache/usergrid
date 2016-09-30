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

package org.apache.usergrid.persistence.qakka.distributed;

import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;

import java.util.Collection;
import java.util.UUID;


/**
 * Interface to distributed part of Qakka queue implementation.
 */
public interface DistributedQueueService {

    enum Status { SUCCESS, ERROR, BAD_REQUEST, NOT_INFLIGHT };

    void init();

    void initQueue(String queueName);

    void refresh();

    void shutdown();

    void refreshQueue(String queueName);

    void processTimeouts();

    Status sendMessageToRegion(
        String queueName,
        String sourceRegion,
        String destRegion,
        UUID messageId,
        Long deliveryTime,
        Long expirationTime);

    Collection<DatabaseQueueMessage> getNextMessages(String queueName, int numMessages);

    Status ackMessage(String queueName, UUID messageId);

    Status requeueMessage(String queueName, UUID messageId);

    Status clearMessages(String queueName);
}
