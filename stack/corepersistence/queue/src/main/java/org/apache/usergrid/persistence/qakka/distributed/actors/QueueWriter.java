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
import com.google.inject.Injector;
import org.apache.usergrid.persistence.qakka.App;
import org.apache.usergrid.persistence.qakka.MetricsService;
import org.apache.usergrid.persistence.qakka.core.QakkaUtils;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.distributed.messages.QueueWriteRequest;
import org.apache.usergrid.persistence.qakka.distributed.messages.QueueWriteResponse;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLog;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLogSerialization;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.MessageCounterSerialization;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.apache.usergrid.persistence.qakka.serialization.transferlog.TransferLogSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;


public class QueueWriter extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( QueueWriter.class );

    public enum WriteStatus { SUCCESS_XFERLOG_DELETED, SUCCESS_XFERLOG_NOTDELETED, ERROR };

    private final DistributedQueueService   distributedQueueService;
    private final QueueMessageSerialization messageSerialization;
    private final TransferLogSerialization  transferLogSerialization;
    private final AuditLogSerialization     auditLogSerialization;
    private final MetricsService            metricsService;

    private final MessageCounterSerialization messageCounterSerialization;


    @Inject
    public QueueWriter( Injector injector ) {

        messageSerialization     = injector.getInstance( QueueMessageSerialization.class );
        transferLogSerialization = injector.getInstance( TransferLogSerialization.class );
        auditLogSerialization    = injector.getInstance( AuditLogSerialization.class );
        metricsService           = injector.getInstance( MetricsService.class );

        distributedQueueService     = injector.getInstance( DistributedQueueService.class );
        messageCounterSerialization = injector.getInstance( MessageCounterSerialization.class );
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

                try {
                    dbqm = new DatabaseQueueMessage(
                            qa.getMessageId(),
                            DatabaseQueueMessage.Type.DEFAULT,
                            qa.getQueueName(),
                            qa.getDestRegion(),
                            null,
                            currentTime,
                            currentTime,
                            queueMessageId );

                    messageSerialization.writeMessage( dbqm );

                    messageCounterSerialization.incrementCounter(
                        qa.getQueueName(), DatabaseQueueMessage.Type.DEFAULT, 1);

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
                            QueueWriter.WriteStatus.ERROR ), getSender() );

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
                            QueueWriter.WriteStatus.SUCCESS_XFERLOG_DELETED ), getSender() );

                } catch (Throwable e) {
                    logger.debug( "Unable to delete transfer log for {} {} {} {}",
                            qa.getQueueName(),
                            qa.getSourceRegion(),
                            qa.getDestRegion(),
                            qa.getMessageId() );
                    logger.debug("Error deleting transferlog", e);

                    getSender().tell( new QueueWriteResponse(
                            QueueWriter.WriteStatus.SUCCESS_XFERLOG_NOTDELETED ), getSender() );
                }

            } finally {
                timer.close();
            }

        } else {
            unhandled( message );
        }

    }

}
