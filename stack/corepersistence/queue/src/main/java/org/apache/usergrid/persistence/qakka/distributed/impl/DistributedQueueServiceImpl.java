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

package org.apache.usergrid.persistence.qakka.distributed.impl;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.actorsystem.ActorSystemManager;
import org.apache.usergrid.persistence.actorsystem.ClientActor;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.QueueManager;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.distributed.messages.*;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaRuntimeException;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Singleton
public class DistributedQueueServiceImpl implements DistributedQueueService {

    private static final Logger logger = LoggerFactory.getLogger( DistributedQueueServiceImpl.class );

    private final ActorSystemManager actorSystemManager;
    private final QueueManager queueManager;
    private final QakkaFig qakkaFig;


    @Inject
    public DistributedQueueServiceImpl(
            ActorSystemManager actorSystemManager,
            QueueManager queueManager,
            QakkaFig qakkaFig ) {

        this.actorSystemManager = actorSystemManager;
        this.queueManager = queueManager;
        this.qakkaFig = qakkaFig;
    }


    @Override
    public void init() {

        try {
            List<String> queues = queueManager.getListOfQueues();
            for ( String queueName : queues ) {
                initQueue( queueName );
            }
        }catch (InvalidQueryException e){

            if (e.getMessage().contains("unconfigured columnfamily")){
                logger.info("Unable to initialize queues since system is bootstrapping.  " +
                    "Queues will be initialized when created");
            }else{
                throw e;
            }

        }

    }


    @Override
    public void initQueue(String queueName) {
        logger.info("Initializing queue: {}", queueName);
        QueueInitRequest request = new QueueInitRequest( queueName );
        ActorRef clientActor = actorSystemManager.getClientActor();
        clientActor.tell( request, null );
    }


    @Override
    public void refresh() {
        for ( String queueName : queueManager.getListOfQueues() ) {
            refreshQueue( queueName );
        }
    }


    @Override
    public void refreshQueue(String queueName) {
        logger.info("Refreshing queue: {}", queueName);
        QueueRefreshRequest request = new QueueRefreshRequest( queueName );
        ActorRef clientActor = actorSystemManager.getClientActor();
        clientActor.tell( request, null );
    }


    @Override
    public void processTimeouts() {

        for ( String queueName : queueManager.getListOfQueues() ) {

            QueueTimeoutRequest request = new QueueTimeoutRequest( queueName );

            ActorRef clientActor = actorSystemManager.getClientActor();
            clientActor.tell( request, null );
        }
    }


    @Override
    public DistributedQueueService.Status sendMessageToRegion(
            String queueName, String sourceRegion, String destRegion, UUID messageId,
            Long deliveryTime, Long expirationTime ) {

        List<String> queueNames = queueManager.getListOfQueues();
        if ( !queueNames.contains( queueName ) ) {
            throw new QakkaRuntimeException( "Queue name: " + queueName + " does not exist");
        }

        int maxRetries = qakkaFig.getMaxSendRetries();
        int retries = 0;

        QueueSendRequest request = new QueueSendRequest(
                queueName, sourceRegion, destRegion, messageId, deliveryTime, expirationTime );

        while ( retries++ < maxRetries ) {
            try {
                Timeout t = new Timeout( qakkaFig.getSendTimeoutSeconds(), TimeUnit.SECONDS );

                // send to current region via local clientActor
                ActorRef clientActor = actorSystemManager.getClientActor();
                Future<Object> fut = Patterns.ask( clientActor, request, t );

                // wait for response...
                final Object response = Await.result( fut, t.duration() );

                if ( response != null && response instanceof QueueSendResponse) {
                    QueueSendResponse qarm = (QueueSendResponse)response;

                    if ( !DistributedQueueService.Status.ERROR.equals( qarm.getSendStatus() )) {

                        if ( retries > 1 ) {
                            logger.debug("SUCCESS after {} retries", retries );
                        }
                        return qarm.getSendStatus();

                    } else {
                        logger.debug("ERROR STATUS sending to queue, retrying {}", retries );
                    }

                } else if ( response != null  ) {
                    logger.debug("NULL RESPONSE sending to queue, retrying {}", retries );

                } else {
                    logger.debug("TIMEOUT sending to queue, retrying {}", retries );
                }

            } catch ( Exception e ) {
                logger.debug("ERROR sending to queue, retrying " + retries, e );
            }
        }

        throw new QakkaRuntimeException( "Error sending to queue after " + retries );
    }


    @Override
    public Collection<DatabaseQueueMessage> getNextMessages( String queueName, int count ) {

        List<String> queueNames = queueManager.getListOfQueues();
        if ( !queueNames.contains( queueName ) ) {
            throw new QakkaRuntimeException( "Queue name: " + queueName + " does not exist");
        }

        int maxRetries = qakkaFig.getMaxGetRetries();
        int retries = 0;

        QueueGetRequest request = new QueueGetRequest( queueName, count );
        while ( retries++ < maxRetries ) {
            try {
                Timeout t = new Timeout( qakkaFig.getGetTimeoutSeconds(), TimeUnit.SECONDS );

                // ask ClientActor and wait (up to timeout) for response

                Future<Object> fut = Patterns.ask( actorSystemManager.getClientActor(), request, t );
                Object responseObject = Await.result( fut, t.duration() );

                if ( responseObject instanceof QakkaMessage ) {

                    final QakkaMessage response = (QakkaMessage)Await.result( fut, t.duration() );

                    if ( response != null && response instanceof QueueGetResponse) {
                        QueueGetResponse qprm = (QueueGetResponse)response;
                        if ( qprm.isSuccess() ) {
                            if (retries > 1) {
                                logger.debug( "getNextMessage SUCCESS after {} retries", retries );
                            }
                        }
                        return qprm.getQueueMessages();


                    } else if ( response != null  ) {
                        logger.debug("ERROR RESPONSE (1) popping queue, retrying {}", retries );

                    } else {
                        logger.debug("TIMEOUT popping to queue, retrying {}", retries );
                    }

                } else if ( responseObject instanceof ClientActor.ErrorResponse ) {

                    final ClientActor.ErrorResponse errorResponse = (ClientActor.ErrorResponse)responseObject;
                    logger.debug("ACTORSYSTEM ERROR popping queue: {}, retrying {}",
                        errorResponse.getMessage(), retries );

                } else {
                    logger.debug("UNKNOWN RESPONSE popping queue, retrying {}", retries );
                }

            } catch ( Exception e ) {
                logger.debug("ERROR popping to queue, retrying " + retries, e );
            }
        }

        throw new QakkaRuntimeException(
                "Error getting from queue " + queueName + " after " + retries );
    }


    @Override
    public Status ackMessage(String queueName, UUID queueMessageId ) {

        List<String> queueNames = queueManager.getListOfQueues();
        if ( !queueNames.contains( queueName ) ) {
            return Status.BAD_REQUEST;
        }

        QueueAckRequest message = new QueueAckRequest( queueName, queueMessageId );
        return sendMessageToLocalQueueActors( message );
    }


    @Override
    public Status requeueMessage(String queueName, UUID messageId) {

        List<String> queueNames = queueManager.getListOfQueues();
        if ( !queueNames.contains( queueName ) ) {
            throw new QakkaRuntimeException( "Queue name: " + queueName + " does not exist");
        }

        QueueAckRequest message = new QueueAckRequest( queueName, messageId );
        return sendMessageToLocalQueueActors( message );
    }


    @Override
    public Status clearMessages(String queueName) {

        List<String> queueNames = queueManager.getListOfQueues();
        if ( !queueNames.contains( queueName ) ) {
            throw new QakkaRuntimeException( "Queue name: " + queueName + " does not exist");
        }

        // TODO: implement clear queue
        throw new UnsupportedOperationException();
    }


    private Status sendMessageToLocalQueueActors( QakkaMessage message ) {

        int maxRetries = 5;
        int retries = 0;

        while ( retries++ < maxRetries ) {
            try {
                Timeout t = new Timeout( 1, TimeUnit.SECONDS );

                // ask ClientActor and wait (up to timeout) for response

                Future<Object> fut = Patterns.ask( actorSystemManager.getClientActor(), message, t );
                final QakkaMessage response = (QakkaMessage)Await.result( fut, t.duration() );

                if ( response != null && response instanceof QueueAckResponse) {
                    QueueAckResponse qprm = (QueueAckResponse)response;
                    return qprm.getStatus();

                } else if ( response != null  ) {
                    logger.debug("ERROR RESPONSE sending message, retrying {}", retries );

                } else {
                    logger.debug("TIMEOUT sending message, retrying {}", retries );
                }

            } catch ( Exception e ) {
                logger.debug("ERROR sending message, retrying " + retries, e );
            }
        }

        throw new QakkaRuntimeException(
                "Error sending message " + message + "after " + retries );
    }

    public void shutdown() {
        actorSystemManager.shutdownAll();
    }
}
