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

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.cluster.client.ClusterClient;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.actorsystem.ActorSystemManager;
import org.apache.usergrid.persistence.qakka.MetricsService;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.distributed.messages.*;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaException;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaRuntimeException;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLog;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLogSerialization;
import org.apache.usergrid.persistence.qakka.serialization.transferlog.TransferLogSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class QueueSender extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( QueueSender.class );

    private final String name = RandomStringUtils.randomAlphanumeric( 4 );

    private final ActorSystemManager        actorSystemManager;
    private final TransferLogSerialization  transferLogSerialization;
    private final AuditLogSerialization     auditLogSerialization;
    private final ActorSystemFig            actorSystemFig;
    private final QakkaFig                  qakkaFig;
    private final MetricsService            metricsService;


    @Inject
    public QueueSender(
        ActorSystemManager        actorSystemManager,
        TransferLogSerialization  transferLogSerialization,
        AuditLogSerialization     auditLogSerialization,
        ActorSystemFig            actorSystemFig,
        QakkaFig                  qakkaFig,
        MetricsService            metricsService
    ) {
        this.actorSystemManager = actorSystemManager;
        this.transferLogSerialization = transferLogSerialization;
        this.auditLogSerialization = auditLogSerialization;
        this.actorSystemFig = actorSystemFig;
        this.qakkaFig = qakkaFig;
        this.metricsService = metricsService;
    }

    @Override
    public void onReceive(Object message) {

        if ( message instanceof QueueSendRequest) {
            QueueSendRequest qa =  (QueueSendRequest) message;

            // as far as caller is concerned, we are done.
            getSender().tell( new QueueSendResponse(
                    DistributedQueueService.Status.SUCCESS, qa.getQueueName() ), getSender() );

            final QueueWriter.WriteStatus writeStatus = sendMessageToRegion(
                    qa.getQueueName(),
                    qa.getSourceRegion(),
                    qa.getDestRegion(),
                    qa.getMessageId(),
                    qa.getDeliveryTime(),
                    qa.getExpirationTime() );

            logResponse( writeStatus, qa.getQueueName(), qa.getDestRegion(), qa.getMessageId() );

        } else {
            unhandled( message );
        }
    }


    QueueWriter.WriteStatus sendMessageToRegion(

            String queueName,
            String sourceRegion,
            String destRegion,
            UUID messageId,
            Long deliveryTime,
            Long expirationTime ) {

        Timer.Context timer = metricsService.getMetricRegistry().timer( MetricsService.SEND_TIME_SEND ).time();
        try {

            int maxRetries = qakkaFig.getMaxSendRetries();
            int retries = 0;

            QueueWriteRequest request = new QueueWriteRequest(
                    queueName, sourceRegion, destRegion, messageId, deliveryTime, expirationTime );

            while (retries++ < maxRetries) {
                try {
                    Timeout t = new Timeout( qakkaFig.getSendTimeoutSeconds(), TimeUnit.SECONDS );

                    Future<Object> fut;

                    if (actorSystemManager.getCurrentRegion().equals( destRegion )) {

                        // send to current region via local clientActor
                        ActorRef clientActor = actorSystemManager.getClientActor();
                        fut = Patterns.ask( clientActor, request, t );

                    } else {

                        // send to remote region via cluster client for that region
                        ActorRef clusterClient = actorSystemManager.getClusterClient( destRegion );
                        fut = Patterns.ask(
                                clusterClient, new ClusterClient.Send( "/user/clientActor", request ), t );
                    }

                    // wait for response...
                    final Object response = Await.result( fut, t.duration() );

                    if (response != null && response instanceof QueueWriteResponse) {
                        QueueWriteResponse qarm = (QueueWriteResponse) response;
                        if (!QueueWriter.WriteStatus.ERROR.equals( qarm.getSendStatus() )) {

                            if (retries > 1) {
                                logger.debug( "queueAdd TOTAL_SUCCESS after {} retries", retries );
                            }
                            return qarm.getSendStatus();

                        } else {
                            logger.debug( "ERROR STATUS adding to queue, retrying {}", retries );
                        }

                    } else if (response != null) {
                        logger.debug( "NULL RESPONSE adding to queue, retrying {}", retries );

                    } else {
                        logger.debug( "TIMEOUT adding to queue, retrying {}", retries );
                    }

                } catch (Exception e) {
                    logger.debug( "ERROR adding to queue, retrying " + retries, e );
                }
            }

            throw new QakkaRuntimeException( "Error adding to queue after " + retries + " retries" );

        } finally {
            timer.stop();
        }
    }


    void logResponse( QueueWriter.WriteStatus writeStatus, String queueName, String region, UUID messageId ) {

        if ( writeStatus != null
                && writeStatus.equals( QueueWriter.WriteStatus.ERROR ) ) {

            logger.debug( "ERROR status sending message: {}, {}, {}, {}",
                    new Object[]{queueName, actorSystemFig.getRegionLocal(), region, messageId} );

            auditLogSerialization.recordAuditLog(
                    AuditLog.Action.SEND,
                    AuditLog.Status.ERROR,
                    queueName,
                    region,
                    messageId,
                    null);

        } else if ( writeStatus != null
                && writeStatus.equals( QueueWriter.WriteStatus.SUCCESS_XFERLOG_NOTDELETED ) ) {

            // queue actor failed to clean up transfer log
            try {
                transferLogSerialization.removeTransferLog(
                        queueName, actorSystemFig.getRegionLocal(), region, messageId );

            } catch (QakkaException se) {
                logger.error( "Unable to delete remove transfer log for {}, {}, {}, {}",
                        new Object[]{queueName, actorSystemFig.getRegionLocal(), region, messageId} );
                logger.debug( "Unable to delete remove transfer log exception is:", se );
            }

        } else if ( writeStatus != null
                && writeStatus.equals( QueueWriter.WriteStatus.SUCCESS_XFERLOG_DELETED ) ) {

            //logger.debug( "Delivery Success: {}, {}, {}, {}",
            //        new Object[]{queueName, actorSystemFig.getRegionLocal(), region, messageId} );
        }

    }

}
