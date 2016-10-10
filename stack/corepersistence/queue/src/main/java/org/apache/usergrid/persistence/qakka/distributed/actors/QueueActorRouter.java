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
import org.apache.usergrid.persistence.qakka.distributed.impl.QueueActorRouterProducer;
import org.apache.usergrid.persistence.qakka.distributed.messages.*;


/**
 * Use consistent hashing to route messages to QueueActors
 */
public class QueueActorRouter extends UntypedActor {

    private final ActorRef routerRef;
    private final QueueActorRouterProducer queueActorRouterProducer;


    @Inject
    public QueueActorRouter( QueueActorRouterProducer queueActorRouterProducer ) {

        this.queueActorRouterProducer = queueActorRouterProducer;

        this.routerRef = getContext().actorOf( FromConfig.getInstance().props(
            Props.create(GuiceActorProducer.class, QueueActor.class)), "router");
    }

    @Override
    public void onReceive(Object message) {

        if ( queueActorRouterProducer.getMessageTypes().contains( message.getClass() ) ) {
            QakkaMessage qakkaMessage = (QakkaMessage) message;

            ConsistentHashingRouter.ConsistentHashableEnvelope envelope =
                    new ConsistentHashingRouter.ConsistentHashableEnvelope( message, qakkaMessage.getQueueName() );
            routerRef.tell( envelope, getSender() );

        } else {
            unhandled(message);
        }
    }
}
