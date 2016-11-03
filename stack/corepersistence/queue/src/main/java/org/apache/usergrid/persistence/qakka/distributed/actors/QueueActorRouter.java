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
import akka.routing.ConsistentHashingRouter;
import akka.routing.FromConfig;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.usergrid.persistence.actorsystem.GuiceActorProducer;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.impl.InMemoryQueue;
import org.apache.usergrid.persistence.qakka.distributed.impl.QueueActorRouterProducer;
import org.apache.usergrid.persistence.qakka.distributed.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * Use consistent hashing to route messages to QueueActors
 */
public class QueueActorRouter extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( QueueActorRouter.class );

    private final ActorRef routerRef;
    private final QueueActorRouterProducer queueActorRouterProducer;

    private final QakkaFig qakkaFig;
    private final Set<String> queuesSeen = new HashSet<>();

    private final Map<String, Cancellable> refreshSchedulersByQueueName = new HashMap<>();
    private final Map<String, Cancellable> timeoutSchedulersByQueueName = new HashMap<>();
    private final Map<String, Cancellable> shardAllocationSchedulersByQueueName = new HashMap<>();


    @Inject
    public QueueActorRouter( QueueActorRouterProducer queueActorRouterProducer, QakkaFig qakkaFig ) {

        this.queueActorRouterProducer = queueActorRouterProducer;
        this.qakkaFig = qakkaFig;

        this.routerRef = getContext().actorOf( FromConfig.getInstance().props(
            Props.create( GuiceActorProducer.class, QueueActor.class)
                .withDispatcher("akka.blocking-io-dispatcher")), "router");
    }

    @Override
    public void onReceive(Object message) {

        if ( queueActorRouterProducer.getMessageTypes().contains( message.getClass() ) ) {
            QakkaMessage qakkaMessage = (QakkaMessage) message;

            if ( qakkaMessage.getQueueName() != null ) {
                initIfNeeded( qakkaMessage.getQueueName() );
            }

            ConsistentHashingRouter.ConsistentHashableEnvelope envelope =
                    new ConsistentHashingRouter.ConsistentHashableEnvelope( message, qakkaMessage.getQueueName() );
            routerRef.tell( envelope, getSender() );

        } else {
            unhandled(message);
        }
    }


    /**
     * Create scheduled refresh, timeout and shard-allocation tasks just in time.
     */
    private void initIfNeeded( String queueName ) {

        if (!queuesSeen.contains( queueName )) {

            queuesSeen.add( queueName );

            if ( qakkaFig.getInMemoryCache() && refreshSchedulersByQueueName.get( queueName ) == null) {
                Cancellable scheduler = getContext().system().scheduler().schedule(
                    Duration.create( 0, TimeUnit.MILLISECONDS ),
                    Duration.create( qakkaFig.getQueueRefreshMilliseconds(), TimeUnit.MILLISECONDS ),
                    self(),
                    new QueueRefreshRequest( queueName, false ),
                    getContext().dispatcher(),
                    getSelf() );
                refreshSchedulersByQueueName.put( queueName, scheduler );
                logger.debug( "Created refresher for queue {}", queueName );
            }

            if ( timeoutSchedulersByQueueName.get( queueName ) == null) {
                Cancellable scheduler = getContext().system().scheduler().schedule(
                    Duration.create( 0, TimeUnit.MILLISECONDS ),
                    Duration.create( qakkaFig.getQueueTimeoutSeconds() / 2, TimeUnit.SECONDS ),
                    self(),
                    new QueueTimeoutRequest( queueName ),
                    getContext().dispatcher(),
                    getSelf() );
                timeoutSchedulersByQueueName.put( queueName, scheduler );
                logger.debug( "Created timeouter for queue {}", queueName );
            }

            if ( shardAllocationSchedulersByQueueName.get( queueName ) == null) {
                Cancellable scheduler = getContext().system().scheduler().schedule(
                    Duration.create( 0, TimeUnit.MILLISECONDS ),
                    Duration.create( qakkaFig.getShardAllocationCheckFrequencyMillis(), TimeUnit.MILLISECONDS ),
                    self(),
                    new ShardCheckRequest( queueName ),
                    getContext().dispatcher(),
                    getSelf() );
                shardAllocationSchedulersByQueueName.put( queueName, scheduler );
                logger.debug( "Created shard allocater for queue {}", queueName );
            }
        }
    }

}
