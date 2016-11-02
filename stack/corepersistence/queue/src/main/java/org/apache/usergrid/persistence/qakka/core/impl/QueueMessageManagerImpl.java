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

package org.apache.usergrid.persistence.qakka.core.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.qakka.api.URIStrategy;
import org.apache.usergrid.persistence.qakka.core.*;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.exceptions.BadRequestException;
import org.apache.usergrid.persistence.qakka.exceptions.NotFoundException;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaRuntimeException;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessageBody;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.MessageCounterSerialization;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardIterator;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardSerialization;
import org.apache.usergrid.persistence.qakka.serialization.transferlog.TransferLogSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.*;


@Singleton
public class QueueMessageManagerImpl implements QueueMessageManager {

    private static final Logger logger = LoggerFactory.getLogger( QueueMessageManagerImpl.class );

    private final ActorSystemFig              actorSystemFig;
    private final QueueManager                queueManager;
    private final QueueMessageSerialization   queueMessageSerialization;
    private final DistributedQueueService     distributedQueueService;
    private final TransferLogSerialization    transferLogSerialization;
    private final URIStrategy                 uriStrategy;
    private final MessageCounterSerialization messageCounterSerialization;
    private final ShardSerialization          shardSerialization;
    private final CassandraClient             cassandraClient;

    @Inject
    public QueueMessageManagerImpl(
        ActorSystemFig              actorSystemFig,
        QueueManager                queueManager,
        QueueMessageSerialization   queueMessageSerialization,
        DistributedQueueService     distributedQueueService,
        TransferLogSerialization    transferLogSerialization,
        URIStrategy                 uriStrategy,
        MessageCounterSerialization messageCounterSerialization,
        ShardSerialization          shardSerialization,
        CassandraClient             cassandraClient ) {

        this.actorSystemFig              = actorSystemFig;
        this.queueManager                = queueManager;
        this.queueMessageSerialization   = queueMessageSerialization;
        this.distributedQueueService     = distributedQueueService;
        this.transferLogSerialization    = transferLogSerialization;
        this.uriStrategy                 = uriStrategy;
        this.messageCounterSerialization = messageCounterSerialization;
        this.shardSerialization          = shardSerialization;
        this.cassandraClient             = cassandraClient;
    }


    @Override
    public void sendMessages(String queueName, List<String> destinationRegions,
            Long delayMs, Long expirationSecs, String contentType, ByteBuffer messageData) {

        if ( queueManager.getQueueConfig( queueName ) == null ) {
            throw new NotFoundException( "Queue " + queueName + " not found" );
        }

        // TODO: implement delay and expiration

//        Preconditions.checkArgument(delayMs == null || delayMs > 0L,
//                "Delay milliseconds must be greater than zero");
//        Preconditions.checkArgument(expirationSecs == null || expirationSecs > 0L,
//                "Expiration seconds must be greater than zero");

        // get current time
        Long currentTimeMs = System.currentTimeMillis();

        // create message id
        UUID messageId = QakkaUtils.getTimeUuid();

        Long deliveryTime = delayMs != null ? currentTimeMs + delayMs : null;
        Long expirationTime = expirationSecs != null ? currentTimeMs + (1000 * expirationSecs) : null;

        // write message data to C*
        queueMessageSerialization.writeMessageData(
                messageId, new DatabaseQueueMessageBody(messageData, contentType));

        for (String region : destinationRegions) {

            transferLogSerialization.recordTransferLog(
                    queueName, actorSystemFig.getRegionLocal(), region, messageId );

            // send message to destination region's queue
            DistributedQueueService.Status status = null;
            try {
                status = distributedQueueService.sendMessageToRegion(
                        queueName,
                        actorSystemFig.getRegionLocal(),
                        region,
                        messageId,
                        deliveryTime,
                        expirationTime );

                //logger.debug("Send message to queueName {} in region {}", queueName, region );

            } catch ( QakkaRuntimeException qae ) {
                logger.error("Error sending message " + messageId + " to " + region, qae);
            }
        }
    }


    @Override
    public List<QueueMessage> getNextMessages(String queueName, int count) {

        Collection<DatabaseQueueMessage> dbMessages = distributedQueueService.getNextMessages( queueName, count );

        List<QueueMessage> queueMessages = joinMessages( queueName, dbMessages );

        if ( queueMessages.size() < count && queueMessages.size() < dbMessages.size() ) {
            logger.debug("Messages failed to join for queue:{}, get more", queueName);

            // some messages failed to join, get more
            dbMessages = distributedQueueService.getNextMessages( queueName, count - queueMessages.size() );
            queueMessages.addAll( joinMessages( queueName, dbMessages ) );
        }

        return queueMessages;
    }


    private List<QueueMessage> joinMessages( String queueName, Collection<DatabaseQueueMessage> dbMessages) {

        List<QueueMessage> queueMessages = new ArrayList<>();

        for (DatabaseQueueMessage dbMessage : dbMessages) {

            DatabaseQueueMessageBody data = queueMessageSerialization.loadMessageData( dbMessage.getMessageId() );

            if ( data != null ) {

                QueueMessage queueMessage = new QueueMessage(
                        dbMessage.getQueueMessageId(),
                        queueName,
                        null,                    // sending region
                        dbMessage.getRegion(),   // receiving region
                        dbMessage.getMessageId(),
                        null,                    // delay until date
                        null,                    // expiration date
                        dbMessage.getQueuedAt(),
                        null,                    // retries
                        true );

                queueMessage.setContentType( data.getContentType() );
                if ( "application/json".equals( data.getContentType() )) {
                    try {
                        String json = new String( data.getBlob().array(), "UTF-8");
                        queueMessage.setData( json );

                    } catch (UnsupportedEncodingException e) {
                        logger.error("Error unencoding data for messageId=" + queueMessage.getMessageId(), e);
                    }
                } else {
                    try {
                        queueMessage.setHref( uriStrategy.queueMessageDataURI(
                                queueName, queueMessage.getQueueMessageId()).toString());

                    } catch (URISyntaxException e) {
                        throw new QakkaRuntimeException( "Error forming URI for message data", e );
                    }
                }

                queueMessages.add( queueMessage );
            }
        }

        return queueMessages;
    }


    @Override
    public void ackMessage(String queueName, UUID queueMessageId) {

        DistributedQueueService.Status status = distributedQueueService.ackMessage( queueName, queueMessageId );

        if ( DistributedQueueService.Status.NOT_INFLIGHT.equals( status )) {
            throw new BadRequestException( "Message not inflight" );

        } else if ( DistributedQueueService.Status.BAD_REQUEST.equals( status )) {
            throw new BadRequestException( "Bad request" );

        } else if ( DistributedQueueService.Status.ERROR.equals( status )) {
            throw new QakkaRuntimeException( "Unable to ack message due to error" );
        }
    }


    @Override
    public void requeueMessage(String queueName, UUID messageId, Long delayMs) {

        // TODO: implement requeueMessage

        throw new UnsupportedOperationException( "requeueMessage not yet implemented" );
    }


    // TODO: implement delete of message data too
//    @Override
//    public void clearMessageData( queueName ) {
//    }


    @Override
    public void clearMessages( String queueName ) {
        queueMessageSerialization.deleteAllMessages( queueName );
        shardSerialization.deleteAllShards( queueName, actorSystemFig.getRegionLocal() );
    }


    @Override
    public ByteBuffer getMessageData( UUID messageId ) {
        DatabaseQueueMessageBody body = queueMessageSerialization.loadMessageData( messageId );
        return body != null ? body.getBlob() : null;
    }


    /**
     * Get but do not put inflight specified queue message, first looking in INFLIGHT table then DEFAULT.
     */
    @Override
    public QueueMessage getMessage( String queueName, UUID queueMessageId ) {

        QueueMessage queueMessage = null;

        // first look in INFLIGHT storage

        DatabaseQueueMessage dbMessage = queueMessageSerialization.loadMessage(
                queueName, actorSystemFig.getRegionLocal(), null,
                DatabaseQueueMessage.Type.INFLIGHT, queueMessageId );

        if ( dbMessage == null ) {

            // not found, so now look in DEFAULT storage

            dbMessage = queueMessageSerialization.loadMessage(
                queueName, actorSystemFig.getRegionLocal(), null,
                DatabaseQueueMessage.Type.DEFAULT, queueMessageId );
        }

        if ( dbMessage != null ) {
            queueMessage = new QueueMessage(
                    dbMessage.getQueueMessageId(),
                    queueName,
                    null,                    // sending region
                    dbMessage.getRegion(),   // receiving region
                    dbMessage.getMessageId(),
                    null,                    // delay until date
                    null,                    // expiration date
                    dbMessage.getQueuedAt(),
                    null,                    // retries
                    true );
        }

        return queueMessage;
    }


    @Override
    public long getQueueDepth( String queueName, DatabaseQueueMessage.Type type ) {
        return messageCounterSerialization.getCounterValue( queueName, type );
    }

}
