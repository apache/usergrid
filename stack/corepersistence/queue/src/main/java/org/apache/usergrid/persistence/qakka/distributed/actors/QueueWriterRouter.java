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
import akka.routing.FromConfig;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.usergrid.persistence.actorsystem.GuiceActorProducer;
import org.apache.usergrid.persistence.qakka.distributed.messages.QueueWriteRequest;


/**
 * Route messages to QueueWriters
 */
public class QueueWriterRouter extends UntypedActor {

    private final ActorRef router;

    @Inject
    public QueueWriterRouter( Injector injector ) {

        this.router = getContext().actorOf( FromConfig.getInstance().props(
            Props.create( GuiceActorProducer.class, QueueWriter.class )), "router");
    }

    @Override
    public void onReceive(Object message) {

        if ( message instanceof QueueWriteRequest) {
            router.tell( message, getSender() );

        } else {
            unhandled(message);
        }
    }
}
