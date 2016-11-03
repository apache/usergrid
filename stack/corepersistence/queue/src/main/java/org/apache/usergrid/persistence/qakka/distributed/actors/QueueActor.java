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
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.actorsystem.GuiceActorProducer;
import org.apache.usergrid.persistence.qakka.MetricsService;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.distributed.messages.*;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class QueueActor extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( QueueActor.class );

    private final QueueActorHelper queueActorHelper;
    private final MetricsService   metricsService;

    //private final Map<String, ActorRef> queueReadersByQueueName    = new HashMap<>();
    private final Map<String, ActorRef> queueTimeoutersByQueueName = new HashMap<>();
    private final Map<String, ActorRef> shardAllocatorsByQueueName = new HashMap<>();


    @Inject
    public QueueActor(
        QueueActorHelper queueActorHelper,
        MetricsService   metricsService
    ) {
        this.queueActorHelper = queueActorHelper;
        this.metricsService = metricsService;
    }


    @Override
    public void onReceive(Object message) {

        if ( message instanceof QueueRefreshRequest ) {
            QueueRefreshRequest request = (QueueRefreshRequest)message;

            // NOT asynchronous because we want this to happen locally in this JVM
            queueActorHelper.queueRefresh( request.getQueueName() );

            /* if ( queueReadersByQueueName.get( request.getQueueName() ) == null ) {
                if ( !request.isOnlyIfEmpty() || inMemoryQueue.peek( request.getQueueName()) == null ) {
                    ActorRef readerRef = getContext().actorOf(
                        Props.create( GuiceActorProducer.class, QueueRefresher.class ),
                        request.getQueueName() + "_reader");
                    queueReadersByQueueName.put( request.getQueueName(), readerRef );
                }
            }
            // hand-off to queue's reader
            queueReadersByQueueName.get( request.getQueueName() ).tell( request, self() ); */


        } else if ( message instanceof QueueTimeoutRequest ) {
            QueueTimeoutRequest request = (QueueTimeoutRequest)message;

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

            Timer.Context timer = metricsService.getMetricRegistry().timer( MetricsService.GET_TIME_GET ).time();
            try {

                Collection<DatabaseQueueMessage> messages = queueActorHelper.getMessages( queueName, numRequested);
                logger.trace("Returning queue {} messages {}", queueName, messages.size() );

                getSender().tell( new QueueGetResponse(
                        DistributedQueueService.Status.SUCCESS, messages, queueName ), getSender() );

            } finally {
                timer.close();
            }

        } else {
            unhandled( message );
        }

    }



}
