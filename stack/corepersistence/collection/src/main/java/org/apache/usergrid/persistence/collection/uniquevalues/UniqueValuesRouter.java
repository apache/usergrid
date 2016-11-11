/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.collection.uniquevalues;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.ConsistentHashingRouter;
import akka.routing.FromConfig;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.actorsystem.GuiceActorProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Uses a consistent hash to route Unique Value requests to UniqueValueActors.
 */
public class UniqueValuesRouter extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( UniqueValueActor.class );

    private final ActorRef router;


    @Inject
    public UniqueValuesRouter() {

        router = getContext().actorOf( FromConfig.getInstance().props(
            Props.create( GuiceActorProducer.class, UniqueValueActor.class)
                .withDispatcher("akka.blocking-io-dispatcher")), "router");
    }

    @Override
    public void onReceive(Object message) {

        if ( message instanceof UniqueValueActor.Request) {
            UniqueValueActor.Request request = (UniqueValueActor.Request)message;

            ConsistentHashingRouter.ConsistentHashableEnvelope envelope =
                new ConsistentHashingRouter.ConsistentHashableEnvelope( message, request.getConsistentHashKey() );
            router.tell( envelope, getSender());

        } else {
            unhandled(message);
        }
    }
}
