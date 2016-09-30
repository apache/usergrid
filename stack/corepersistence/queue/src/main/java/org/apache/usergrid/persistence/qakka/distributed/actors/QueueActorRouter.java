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
import akka.routing.ConsistentHashingRouter;
import akka.routing.FromConfig;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.usergrid.persistence.actorsystem.GuiceActorProducer;
import org.apache.usergrid.persistence.qakka.distributed.messages.*;


/**
 * Use consistent hashing to route messages to QueueActors
 */
public class QueueActorRouter extends UntypedActor {

    private final ActorRef routerRef;


    @Inject
    public QueueActorRouter( Injector injector ) {

        this.routerRef = getContext().actorOf( FromConfig.getInstance().props(
            Props.create(GuiceActorProducer.class, injector, QueueActor.class)), "router");
    }

    @Override
    public void onReceive(Object message) {

        // TODO: can we do something smarter than this if-then-else structure
        // e.g. if message is recognized as one of ours, then we just pass it on?

        if ( message instanceof QueueGetRequest) {
            QueueGetRequest qgr = (QueueGetRequest) message;

            ConsistentHashingRouter.ConsistentHashableEnvelope envelope =
                    new ConsistentHashingRouter.ConsistentHashableEnvelope( message, qgr.getQueueName() );
            routerRef.tell( envelope, getSender() );

        } else if ( message instanceof QueueAckRequest) {
            QueueAckRequest qar = (QueueAckRequest)message;

            ConsistentHashingRouter.ConsistentHashableEnvelope envelope =
                    new ConsistentHashingRouter.ConsistentHashableEnvelope( message, qar.getQueueName() );
            routerRef.tell( envelope, getSender());

        } else if ( message instanceof QueueInitRequest) {
            QueueInitRequest qar = (QueueInitRequest)message;

            ConsistentHashingRouter.ConsistentHashableEnvelope envelope =
                    new ConsistentHashingRouter.ConsistentHashableEnvelope( message, qar.getQueueName() );
            routerRef.tell( envelope, getSender());

        } else if ( message instanceof QueueRefreshRequest) {
            QueueRefreshRequest qar = (QueueRefreshRequest)message;

            ConsistentHashingRouter.ConsistentHashableEnvelope envelope =
                    new ConsistentHashingRouter.ConsistentHashableEnvelope( message, qar.getQueueName() );
            routerRef.tell( envelope, getSender());

        } else if ( message instanceof QueueTimeoutRequest) {
            QueueTimeoutRequest qar = (QueueTimeoutRequest)message;

            ConsistentHashingRouter.ConsistentHashableEnvelope envelope =
                    new ConsistentHashingRouter.ConsistentHashableEnvelope( message, qar.getQueueName() );
            routerRef.tell( envelope, getSender());

        } else if ( message instanceof ShardCheckRequest) {
            ShardCheckRequest qar = (ShardCheckRequest)message;

            ConsistentHashingRouter.ConsistentHashableEnvelope envelope =
                    new ConsistentHashingRouter.ConsistentHashableEnvelope( message, qar.getQueueName() );
            routerRef.tell( envelope, getSender());

        } else {
            unhandled(message);
        }
    }
}
