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
package org.apache.usergrid.persistence.collection.uniquevalues;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Updates local unique values cache based on reservations and cancellations.
 */
public class ReservationCacheActor extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( ReservationCacheActor.class );

    int reservationCount = 0;
    int cancellationCount = 0;

    public ReservationCacheActor() {

        // subscribe to the topic named "content"
        ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();
        mediator.tell(new DistributedPubSubMediator.Subscribe("content", getSelf()), getSelf());
    }

    public void onReceive( Object msg ) {

        if ( msg instanceof UniqueValueActor.Reservation ) {
            UniqueValueActor.Reservation res = (UniqueValueActor.Reservation)msg;
            ReservationCache.getInstance().cacheReservation( res );

            if ( ++reservationCount % 10 == 0 ) {
                logger.info("Received {} reservations cache size {}",
                        reservationCount, ReservationCache.getInstance().getSize());
            }

        } else if ( msg instanceof UniqueValueActor.Cancellation ) {
            UniqueValueActor.Cancellation can = (UniqueValueActor.Cancellation) msg;
            ReservationCache.getInstance().cancelReservation( can );

            if (++cancellationCount % 10 == 0) {
                logger.info( "Received {} cancellations", cancellationCount );
            }
            logger.debug("Removing cancelled {} from reservation cache", can.getConsistentHashKey());

        } else if ( msg instanceof UniqueValueActor.Response ) {
            UniqueValueActor.Response response = (UniqueValueActor.Response) msg;
            ReservationCache.getInstance().cancelReservation( response );

            logger.info("Removing completed {} from reservation cache", response.getConsistentHashKey());

        } else if (msg instanceof DistributedPubSubMediator.SubscribeAck) {
            logger.debug( "subscribing" );

        } else {
            unhandled( msg );
        }
    }
}
