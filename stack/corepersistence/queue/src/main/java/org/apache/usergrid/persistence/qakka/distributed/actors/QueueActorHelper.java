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

package org.apache.usergrid.persistence.qakka.distributed.actors;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.qakka.MetricsService;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.core.impl.InMemoryQueue;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.serialization.MultiShardMessageIterator;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLog;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLogSerialization;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;


public class QueueActorHelper {
    private static final Logger logger = LoggerFactory.getLogger( QueueActorHelper.class );

    private final ActorSystemFig            actorSystemFig;
    private final QueueMessageSerialization messageSerialization;
    private final AuditLogSerialization     auditLogSerialization;
    private final InMemoryQueue             inMemoryQueue;
    private final QakkaFig                  qakkaFig;
    private final MetricsService            metricsService;
    private final CassandraClient           cassandraClient;


    @Inject
    public QueueActorHelper(
            QakkaFig                  qakkaFig,
            ActorSystemFig            actorSystemFig,
            QueueMessageSerialization messageSerialization,
            AuditLogSerialization     auditLogSerialization,
            InMemoryQueue             inMemoryQueue,
            MetricsService            metricsService,
            CassandraClient           cassandraClient
            ) {

        this.actorSystemFig        = actorSystemFig;
        this.messageSerialization  = messageSerialization;
        this.auditLogSerialization = auditLogSerialization;
        this.inMemoryQueue         = inMemoryQueue;
        this.qakkaFig              = qakkaFig;
        this.metricsService        = metricsService;
        this.cassandraClient       = cassandraClient;
    }


    DatabaseQueueMessage loadDatabaseQueueMessage(
            String queueName, UUID queueMessageId, DatabaseQueueMessage.Type type ) {

        try {
            return messageSerialization.loadMessage(
                    queueName,
                    actorSystemFig.getRegionLocal(),
                    null,
                    type,
                    queueMessageId );

        } catch (Throwable t) {
            logger.error( "Error reading queueMessage", t );
        }

        return null;
    }


    Collection<DatabaseQueueMessage> getMessages(String queueName, int numRequested ) {

        Collection<DatabaseQueueMessage> queueMessages = new ArrayList<>();

        while (queueMessages.size() < numRequested) {

            DatabaseQueueMessage queueMessage = inMemoryQueue.poll( queueName );

            if (queueMessage != null) {

                if (putInflight( queueMessage )) {
                    queueMessages.add( queueMessage );
                }

            } else {
                //logger.debug("in-memory queue for {} is empty, object is: {}", queueName, inMemoryQueue );
                break;
            }
        }

        logger.debug("{} returning {} for queue {}", this, queueMessages.size(), queueName);
        return queueMessages;

    }


    boolean putInflight( DatabaseQueueMessage queueMessage ) {

        UUID qmid = queueMessage.getQueueMessageId();
        try {

            messageSerialization.putInflight( queueMessage );

        } catch ( Throwable t ) {
            logger.error("Error putting inflight queue message "
                + qmid + " queue name: " + queueMessage.getQueueName(), t);

            auditLogSerialization.recordAuditLog(
                AuditLog.Action.GET,
                AuditLog.Status.ERROR,
                queueMessage.getQueueName(),
                actorSystemFig.getRegionLocal(),
                queueMessage.getMessageId(),
                qmid);

            return false;
        }

        auditLogSerialization.recordAuditLog(
            AuditLog.Action.GET,
            AuditLog.Status.SUCCESS,
            queueMessage.getQueueName(),
            actorSystemFig.getRegionLocal(),
            queueMessage.getMessageId(),
            qmid);

        return true;
    }


    DistributedQueueService.Status ackQueueMessage(String queueName, UUID queueMessageId ) {

        DatabaseQueueMessage queueMessage = messageSerialization.loadMessage(
            queueName,
            actorSystemFig.getRegionLocal(),
            null,
            DatabaseQueueMessage.Type.INFLIGHT,
            queueMessageId );

        if ( queueMessage == null ) {
            logger.error("Queue {} queue message id {} not found in inflight table", queueName, queueMessageId);
            return DistributedQueueService.Status.NOT_INFLIGHT;
        }

        boolean error = false;
        try {
            messageSerialization.deleteMessage(
                    queueName,
                    actorSystemFig.getRegionLocal(),
                    null,
                    DatabaseQueueMessage.Type.INFLIGHT,
                    queueMessageId );

        } catch (Throwable t) {
            logger.error( "Error deleting queueMessage for ack", t );
            error = true;
        }

        if ( !error ) {

            auditLogSerialization.recordAuditLog(
                    AuditLog.Action.ACK,
                    AuditLog.Status.SUCCESS,
                    queueName,
                    actorSystemFig.getRegionLocal(),
                    queueMessage.getMessageId(),
                    queueMessageId );

            return DistributedQueueService.Status.SUCCESS;

        } else {

            auditLogSerialization.recordAuditLog(
                    AuditLog.Action.ACK,
                    AuditLog.Status.ERROR,
                    queueName,
                    actorSystemFig.getRegionLocal(),
                    queueMessage.getMessageId(),
                    queueMessageId );

            return DistributedQueueService.Status.ERROR;
        }
    }
}
