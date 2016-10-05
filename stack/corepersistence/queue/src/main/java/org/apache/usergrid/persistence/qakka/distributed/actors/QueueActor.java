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
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.usergrid.persistence.actorsystem.GuiceActorProducer;
import org.apache.usergrid.persistence.qakka.App;
import org.apache.usergrid.persistence.qakka.MetricsService;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.impl.InMemoryQueue;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.distributed.messages.*;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.MessageCounterSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class QueueActor extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( QueueActor.class );

    private final QakkaFig         qakkaFig;
    private final InMemoryQueue    inMemoryQueue;
    private final QueueActorHelper queueActorHelper;
    private final MetricsService   metricsService;
    private final MessageCounterSerialization messageCounterSerialization;

    private final Map<String, Cancellable> refreshSchedulersByQueueName = new HashMap<>();
    private final Map<String, Cancellable> timeoutSchedulersByQueueName = new HashMap<>();
    private final Map<String, Cancellable> shardAllocationSchedulersByQueueName = new HashMap<>();

    private final Map<String, ActorRef> queueReadersByQueueName    = new HashMap<>();
    private final Map<String, ActorRef> queueTimeoutersByQueueName = new HashMap<>();
    private final Map<String, ActorRef> shardAllocatorsByQueueName = new HashMap<>();

    private final Set<String> queuesSeen = new HashSet<>();

    //private final Injector injector;


    @Inject
    public QueueActor(
        //Injector         injector,
        QakkaFig         qakkaFig,
        InMemoryQueue    inMemoryQueue,
        QueueActorHelper queueActorHelper,
        MetricsService   metricsService,
        MessageCounterSerialization messageCounterSerialization
    ) {
        //this.injector = injector;
        this.qakkaFig = qakkaFig;
        this.inMemoryQueue = inMemoryQueue;
        this.queueActorHelper = queueActorHelper;
        this.metricsService = metricsService;
        this.messageCounterSerialization = messageCounterSerialization;

//        qakkaFig         = injector.getInstance( QakkaFig.class );
//        inMemoryQueue    = injector.getInstance( InMemoryQueue.class );
//        queueActorHelper = injector.getInstance( QueueActorHelper.class );
//        metricsService   = injector.getInstance( MetricsService.class );
//        messageCounterSerialization = injector.getInstance( MessageCounterSerialization.class );
    }

    @Override
    public void onReceive(Object message) {

        if ( message instanceof QueueInitRequest) {
            QueueInitRequest request = (QueueInitRequest)message;

            queuesSeen.add( request.getQueueName() );

            if ( refreshSchedulersByQueueName.get( request.getQueueName() ) == null ) {
                Cancellable scheduler = getContext().system().scheduler().schedule(
                    Duration.create( 0, TimeUnit.MILLISECONDS),
                    Duration.create( qakkaFig.getQueueRefreshMilliseconds(), TimeUnit.MILLISECONDS),
                    self(),
                    new QueueRefreshRequest( request.getQueueName(), false ),
                    getContext().dispatcher(),
                    getSelf());
                refreshSchedulersByQueueName.put( request.getQueueName(), scheduler );
                logger.debug("Created refresher for queue {}", request.getQueueName() );
            }

            if ( timeoutSchedulersByQueueName.get( request.getQueueName() ) == null ) {
                Cancellable scheduler = getContext().system().scheduler().schedule(
                        Duration.create( 0, TimeUnit.MILLISECONDS),
                        Duration.create( qakkaFig.getQueueTimeoutSeconds()/2, TimeUnit.SECONDS),
                        self(),
                        new QueueTimeoutRequest( request.getQueueName() ),
                        getContext().dispatcher(),
                        getSelf());
                timeoutSchedulersByQueueName.put( request.getQueueName(), scheduler );
                logger.debug("Created timeouter for queue {}", request.getQueueName() );
            }

            if ( shardAllocationSchedulersByQueueName.get( request.getQueueName() ) == null ) {
                Cancellable scheduler = getContext().system().scheduler().schedule(
                        Duration.create( 0, TimeUnit.MILLISECONDS),
                        Duration.create( qakkaFig.getShardAllocationCheckFrequencyMillis(), TimeUnit.MILLISECONDS),
                        self(),
                        new ShardCheckRequest( request.getQueueName() ),
                        getContext().dispatcher(),
                        getSelf());
                shardAllocationSchedulersByQueueName.put( request.getQueueName(), scheduler );
                logger.debug("Created shard allocater for queue {}", request.getQueueName() );
            }

        } else if ( message instanceof QueueRefreshRequest ) {
            QueueRefreshRequest request = (QueueRefreshRequest)message;
            queuesSeen.add( request.getQueueName() );

//            // NOT asynchronous
//            queueActorHelper.queueRefresh( request.getQueueName() );

            if ( queueReadersByQueueName.get( request.getQueueName() ) == null ) {

                if ( !request.isOnlyIfEmpty() || inMemoryQueue.peek( request.getQueueName()) == null ) {
                    ActorRef readerRef = getContext().actorOf(
                        Props.create( GuiceActorProducer.class, QueueRefresher.class ),
                        request.getQueueName() + "_reader");
                    queueReadersByQueueName.put( request.getQueueName(), readerRef );
                }
            }

            // hand-off to queue's reader
            queueReadersByQueueName.get( request.getQueueName() ).tell( request, self() );

        } else if ( message instanceof QueueTimeoutRequest ) {
            QueueTimeoutRequest request = (QueueTimeoutRequest)message;

            queuesSeen.add( request.getQueueName() );

            if ( queueTimeoutersByQueueName.get( request.getQueueName() ) == null ) {
                ActorRef readerRef = getContext().actorOf(
                    Props.create( GuiceActorProducer.class, QueueTimeouter.class),
                    request.getQueueName() + "_timeouter");
                queueTimeoutersByQueueName.put( request.getQueueName(), readerRef );
            }

            // ASYNCHRONOUS -> hand-off to queue's timeouter
            queueTimeoutersByQueueName.get( request.getQueueName() ).tell( request, self() );

        } else if ( message instanceof ShardCheckRequest ) {
            ShardCheckRequest request = (ShardCheckRequest)message;

            queuesSeen.add( request.getQueueName() );

            if ( shardAllocatorsByQueueName.get( request.getQueueName() ) == null ) {
                ActorRef readerRef = getContext().actorOf(
                    Props.create( GuiceActorProducer.class, ShardAllocator.class),
                    request.getQueueName() + "_shard_allocator");
                shardAllocatorsByQueueName.put( request.getQueueName(), readerRef );
            }

            // ASYNCHRONOUS -> hand-off to queue's shard allocator
            shardAllocatorsByQueueName.get( request.getQueueName() ).tell( request, self() );

        } else if ( message instanceof QueueGetRequest) {

            QueueGetRequest queueGetRequest = (QueueGetRequest) message;

            String queueName = queueGetRequest.getQueueName();
            int numRequested = queueGetRequest.getNumRequested();

            queuesSeen.add( queueName );

            Timer.Context timer = metricsService.getMetricRegistry().timer( MetricsService.GET_TIME_GET ).time();
            try {

                Collection<DatabaseQueueMessage> messages = queueActorHelper.getMessages( queueName, numRequested);

                getSender().tell( new QueueGetResponse(
                        DistributedQueueService.Status.SUCCESS, messages ), getSender() );

            } finally {
                timer.close();
            }


        } else if ( message instanceof QueueAckRequest) {

            Timer.Context timer = metricsService.getMetricRegistry().timer( MetricsService.ACK_TIME_ACK ).time();
            try {

                QueueAckRequest queueAckRequest = (QueueAckRequest) message;

                queuesSeen.add( queueAckRequest.getQueueName() );

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
