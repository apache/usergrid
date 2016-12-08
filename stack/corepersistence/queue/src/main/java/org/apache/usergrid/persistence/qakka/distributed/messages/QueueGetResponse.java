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
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;

import java.util.Collection;
import java.util.Collections;


public class QueueGetResponse implements QakkaMessage {
    private final Collection<DatabaseQueueMessage> queueMessages;
    private final DistributedQueueService.Status status;
    private final String queueName;

    public QueueGetResponse(DistributedQueueService.Status status, String queueName ) {
        this.status = status;
        this.queueMessages = Collections.emptyList();
        this.queueName = queueName;
    }

    public QueueGetResponse(
        DistributedQueueService.Status status, Collection<DatabaseQueueMessage> queueMessages, String queueName) {
        this.status = status;
        this.queueMessages = queueMessages;
        this.queueName = queueName;
    }

    public DistributedQueueService.Status getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status.equals( DistributedQueueService.Status.SUCCESS);
    }

    public Collection<DatabaseQueueMessage> getQueueMessages() {
        return queueMessages;
    }

    public String toString() {
        return new ToStringBuilder( this )
                .append( "messageCount", queueMessages.size() )
                .append( "status", status )
                .toString();
    }

    @Override
    public String getQueueName() {
        return queueName;
    }
}
