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
import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.actorsystem.ActorSystemManager;
import org.apache.usergrid.persistence.actorsystem.ClientActor;
import org.apache.usergrid.persistence.actorsystem.GuiceActorProducer;
import org.apache.usergrid.persistence.qakka.MetricsService;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.QueueManager;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.distributed.messages.*;
import org.apache.usergrid.persistence.qakka.exceptions.NotFoundException;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaRuntimeException;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Singleton
public class DistributedQueueServiceImpl implements DistributedQueueService {

    private static final Logger logger = LoggerFactory.getLogger( DistributedQueueServiceImpl.class );

    private final ActorSystemManager actorSystemManager;
    private final QueueManager queueManager;
    private final QakkaFig qakkaFig;
    private final MetricsService metricsService;

    @Inject
    public DistributedQueueServiceImpl(
            Injector injector,
            ActorSystemManager actorSystemManager,
            QueueManager queueManager,
            QakkaFig qakkaFig,
            MetricsService metricsService
            ) {

        this.actorSystemManager = actorSystemManager;
        this.queueManager = queueManager;
        this.qakkaFig = qakkaFig;
        this.metricsService = metricsService;

        GuiceActorProducer.INJECTOR = injector;
    }


    @Override
    public void init() {

        StringBuilder logMessage = new StringBuilder();
        logMessage.append( "DistributedQueueServiceImpl initialized with config:\n" );
        Method[] methods = qakkaFig.getClass().getMethods();
        for ( Method method : methods ) {
            if ( method.getName().startsWith("get")) {
                try {
                    logMessage.append("   ")
                        .append( method.getName().substring(3) )
                        .append(" = ")
                        .append( method.invoke( qakkaFig ).toString() ).append("\n");
                } catch (Exception ignored ) {}
            }
        }
        logger.info( logMessage.toString() );
    }


    @Override
    public void refresh() {
        for ( String queueName : queueManager.getListOfQueues() ) {
            refreshQueue( queueName );
        }
    }


    @Override
    public void refreshQueue(String queueName) {
        if ( qakkaFig.getInMemoryCache() ) {
            logger.trace( "{} Requesting refresh for queue: {}", this, queueName );
            QueueRefreshRequest request = new QueueRefreshRequest( queueName, false );
            ActorRef clientActor = actorSystemManager.getClientActor();
            clientActor.tell( request, null );
        }

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

        logger.trace("Sending message to queue {} region {}", queueName, destRegion);

        Timer.Context timer = metricsService.getMetricRegistry().timer( MetricsService.SEND_TIME_TOTAL ).time();
        try {

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

                            if ( qakkaFig.getInMemoryCache() ) {
                                // send refresh-queue-if-empty message
                                QueueRefreshRequest qrr = new QueueRefreshRequest( queueName, false );
                                clientActor.tell( qrr, null );
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

        } finally {
            timer.close();
        }
    }


    @Override
    public Collection<DatabaseQueueMessage> getNextMessages( String queueName, int count ) {
        List<DatabaseQueueMessage> ret = new ArrayList<>();

        com.codahale.metrics.Timer.Context timer =
            metricsService.getMetricRegistry().timer( MetricsService.GET_TIME_TOTAL ).time();

        try {

            long startTime = System.currentTimeMillis();

            while ( ret.size() < count
                && System.currentTimeMillis() - startTime < qakkaFig.getLongPollTimeMillis()) {

                ret.addAll( getNextMessagesInternal( queueName, count ));

                if ( ret.size() < count ) {
                    try { Thread.sleep( qakkaFig.getLongPollTimeMillis() / 2 ); } catch (Exception ignored) {}
                }
            }

//            if ( ret.isEmpty() ) {
//                logger.info( "Requested {} but queue '{}' is empty", count, queueName);
//            }
            return ret;

        } finally {
            timer.close();
        }

    }


    public Collection<DatabaseQueueMessage> getNextMessagesInternal( String queueName, int count ) {

        if ( actorSystemManager.getClientActor() == null || !actorSystemManager.isReady() ) {
            logger.error("Akka Actor System is not ready yet for requests.");
            return Collections.EMPTY_LIST;
        }

        int maxRetries = qakkaFig.getMaxGetRetries();
        int tries = 0;

        QueueGetRequest request = new QueueGetRequest( queueName, count );
        while ( ++tries < maxRetries ) {
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
                            if (tries > 1) {
                                logger.warn( "getNextMessage {} SUCCESS after {} tries", queueName, tries );
                            }
                        }
                        return qprm.getQueueMessages();


                    } else if ( response != null  ) {
                        logger.debug("ERROR RESPONSE (1) popping queue {}, retrying {}", queueName, tries );

                    } else {
                        logger.trace("TIMEOUT popping from queue {}, retrying {}", queueName, tries );
                    }

                } else if ( responseObject instanceof ClientActor.ErrorResponse ) {

                    final ClientActor.ErrorResponse errorResponse = (ClientActor.ErrorResponse)responseObject;
                    logger.debug("ACTORSYSTEM ERROR popping queue: {}, retrying {}",
                        errorResponse.getMessage(), tries );

                } else {
                    logger.debug("UNKNOWN RESPONSE popping queue {}, retrying {}", queueName, tries );
                }

            } catch ( TimeoutException e ) {
                logger.trace("TIMEOUT popping to queue " + queueName + " retrying " + tries, e );

            } catch ( Exception e ) {
                logger.debug("ERROR popping to queue " + queueName + " retrying " + tries, e );
            }
        }

        throw new QakkaRuntimeException(
                "Error getting from queue " + queueName + " after " + tries + " tries");
    }


    @Override
    public Status ackMessage(String queueName, UUID queueMessageId ) {

        Timer.Context timer = metricsService.getMetricRegistry().timer( MetricsService.ACK_TIME_TOTAL ).time();
        try {

            QueueAckRequest message = new QueueAckRequest( queueName, queueMessageId );
            return sendMessageToLocalRouters( message );


        } finally {
            timer.close();
        }
    }


    @Override
    public Status requeueMessage(String queueName, UUID messageId) {

        QueueAckRequest message = new QueueAckRequest( queueName, messageId );
        return sendMessageToLocalRouters( message );
    }


    private Status sendMessageToLocalRouters( QakkaMessage message ) {

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
                    logger.debug("UNKNOWN RESPONSE sending message, retrying {}", retries );

                } else {
                    logger.trace("TIMEOUT sending message, retrying {}", retries );
                }

            } catch ( TimeoutException e ) {
                logger.trace( "TIMEOUT sending message, retrying " + retries, e );

            } catch ( Exception e ) {
                logger.debug("ERROR sending message, retrying " + retries, e );
            }
        }

        throw new QakkaRuntimeException(
                "Error sending message " + message + "after " + retries );
    }

    public void shutdown() {
        // no op
    }
}
