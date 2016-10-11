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

import akka.actor.UntypedActor;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.qakka.MetricsService;
import org.apache.usergrid.persistence.qakka.core.QakkaUtils;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.distributed.messages.QueueAckRequest;
import org.apache.usergrid.persistence.qakka.distributed.messages.QueueAckResponse;
import org.apache.usergrid.persistence.qakka.distributed.messages.QueueWriteRequest;
import org.apache.usergrid.persistence.qakka.distributed.messages.QueueWriteResponse;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLog;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLogSerialization;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.apache.usergrid.persistence.qakka.serialization.transferlog.TransferLogSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;


public class QueueWriter extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( QueueWriter.class );

    public enum WriteStatus { SUCCESS_XFERLOG_DELETED, SUCCESS_XFERLOG_NOTDELETED, ERROR };

    private final QueueMessageSerialization messageSerialization;
    private final TransferLogSerialization  transferLogSerialization;
    private final AuditLogSerialization     auditLogSerialization;
    private final MetricsService            metricsService;
    private final QueueActorHelper          queueActorHelper;

    @Inject
    public QueueWriter(
        QueueMessageSerialization messageSerialization,
        TransferLogSerialization  transferLogSerialization,
        AuditLogSerialization     auditLogSerialization,
        MetricsService            metricsService,
        QueueActorHelper          queueActorHelper
    ) {
        this.messageSerialization     = messageSerialization;
        this.transferLogSerialization = transferLogSerialization;
        this.auditLogSerialization    = auditLogSerialization;
        this.metricsService           = metricsService;
        this.queueActorHelper         = queueActorHelper;
    }

    @Override
    public void onReceive(Object message) {

        if (message instanceof QueueWriteRequest) {

            Timer.Context timer = metricsService.getMetricRegistry().timer( MetricsService.SEND_TIME_WRITE ).time();

            try {
                QueueWriteRequest qa = (QueueWriteRequest) message;

                UUID queueMessageId = QakkaUtils.getTimeUuid();

                // TODO: implement deliveryTime and expirationTime

                DatabaseQueueMessage dbqm = null;
                long currentTime = System.currentTimeMillis();
                String queueName = qa.getQueueName();

                try {
                    dbqm = new DatabaseQueueMessage(
                            qa.getMessageId(),
                            DatabaseQueueMessage.Type.DEFAULT,
                            qa.getQueueName(),
                            qa.getDestRegion(),
                            null,
                            currentTime,
                            -1L,
                            queueMessageId );

                    messageSerialization.writeMessage( dbqm );

                    //logger.debug("Wrote queue message id {} to queue name {}",
                    //        dbqm.getQueueMessageId(), dbqm.getQueueName());

                } catch (Throwable t) {
                    logger.debug("Error creating database queue message", t);

                    auditLogSerialization.recordAuditLog(
                            AuditLog.Action.SEND,
                            AuditLog.Status.ERROR,
                            qa.getQueueName(),
                            qa.getDestRegion(),
                            qa.getMessageId(),
                            dbqm.getMessageId() );

                    getSender().tell( new QueueWriteResponse(
                            QueueWriter.WriteStatus.ERROR, queueName ), getSender() );

                    return;
                }

                auditLogSerialization.recordAuditLog(
                        AuditLog.Action.SEND,
                        AuditLog.Status.SUCCESS,
                        qa.getQueueName(),
                        qa.getDestRegion(),
                        qa.getMessageId(),
                        dbqm.getQueueMessageId() );

                try {
                    transferLogSerialization.removeTransferLog(
                            qa.getQueueName(),
                            qa.getSourceRegion(),
                            qa.getDestRegion(),
                            qa.getMessageId() );

                    getSender().tell( new QueueWriteResponse(
                            QueueWriter.WriteStatus.SUCCESS_XFERLOG_DELETED, queueName ), getSender() );

                } catch (Throwable e) {
                    logger.debug( "Unable to delete transfer log for {} {} {} {}",
                            qa.getQueueName(),
                            qa.getSourceRegion(),
                            qa.getDestRegion(),
                            qa.getMessageId() );
                    logger.debug("Error deleting transferlog", e);

                    getSender().tell( new QueueWriteResponse(
                            QueueWriter.WriteStatus.SUCCESS_XFERLOG_NOTDELETED, queueName ), getSender() );
                }

            } finally {
                timer.close();
            }

        } else if ( message instanceof QueueAckRequest ){

            Timer.Context timer = metricsService.getMetricRegistry().timer( MetricsService.ACK_TIME_ACK ).time();
            try {

                QueueAckRequest queueAckRequest = (QueueAckRequest) message;

                DistributedQueueService.Status status = queueActorHelper.ackQueueMessage(
                    queueAckRequest.getQueueName(),
                    queueAckRequest.getQueueMessageId() );

                getSender().tell( new QueueAckResponse(
                    queueAckRequest.getQueueName(),
                    queueAckRequest.getQueueMessageId(),
                    status ), getSender() );

            } finally {
                timer.close();
            }

        } else {
            unhandled( message );
        }

    }

}
