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
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.UUID;

public class UniqueValueActor extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( UniqueValueActor.class );

    private final String name = RandomStringUtils.randomAlphanumeric( 4 );

    //private MetricsService metricsService;

    final private ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();

    private final UniqueValuesTable table;

    private int count = 0;


    public UniqueValueActor() {

        // TODO: is there a way to avoid this ugly kludge? see also: ClusterSingletonRouter
        this.table = UniqueValuesServiceImpl.injector.getInstance( UniqueValuesTable.class );
        logger.info("UniqueValueActor {} is live with table {}", name, table);
    }

    @Override
    public void onReceive(Object message) {

        if ( message instanceof Request ) {
            Request req = (Request) message;

            count++;
            if (count % 10 == 0) {
                logger.debug( "UniqueValueActor {} processed {} requests", name, count );
            }
        }

        if ( message instanceof Reservation ) {
            Reservation res = (Reservation) message;

//            final Timer.Context context = metricsService.getReservationTimer().time();

            try {
                Id owner = table.lookupOwner( res.getApplicationScope(), res.getOwner().getType(), res.getField() );

                if ( owner != null && owner.equals( res.getOwner() )) {
                    // sender already owns this unique value
                    getSender().tell( new Response( Response.Status.IS_UNIQUE ), getSender() );
                    return;

                } else if ( owner != null && !owner.equals( res.getOwner() )) {
                    // tell sender value is not unique
                    getSender().tell( new Response( Response.Status.NOT_UNIQUE ), getSender() );
                    return;
                }

                table.reserve( res.getApplicationScope(), res.getOwner(), res.getOwnerVersion(), res.getField() );

                getSender().tell( new Response( Response.Status.IS_UNIQUE ), getSender() );

                mediator.tell( new DistributedPubSubMediator.Publish( "content",
                        new Reservation( res ) ), getSelf() );

            } catch (Throwable t) {

                getSender().tell( new Response( Response.Status.ERROR ), getSender() );
                logger.error( "Error processing request", t );


            } finally {
//                context.stop();
            }

        } else if ( message instanceof Confirmation) {
            Confirmation con = (Confirmation) message;

//            final Timer.Context context = metricsService.getCommitmentTimer().time();

            try {
                Id owner = table.lookupOwner( con.getApplicationScope(), con.getOwner().getType(), con.getField() );

                if ( owner != null && !owner.equals( con.getOwner() )) {
                    // cannot reserve, somebody else owns the unique value
                    getSender().tell( new Response( Response.Status.NOT_UNIQUE ), getSender() );
                    return;

                } else if ( owner == null ) {
                    // cannot commit without first reserving
                    getSender().tell( new Response( Response.Status.BAD_REQUEST ), getSender() );
                    return;
                }

                table.confirm( con.getApplicationScope(), con.getOwner(), con.getOwnerVersion(), con.getField() );

                getSender().tell( new Response( Response.Status.IS_UNIQUE ), getSender() );

                mediator.tell( new DistributedPubSubMediator.Publish( "content",
                        new Reservation( con ) ), getSelf() );

            } catch (Throwable t) {
                getSender().tell( new Response( Response.Status.ERROR ), getSender() );
                logger.error( "Error processing request", t );

            } finally {
//                context.stop();
            }


        } else if ( message instanceof Cancellation ) {
            Cancellation can = (Cancellation) message;

            try {
                Id owner = table.lookupOwner( can.getApplicationScope(), can.getOwner().getType(), can.getField() );

                if ( owner != null && !owner.equals( can.getOwner() )) {
                    // cannot cancel, somebody else owns the unique value
                    getSender().tell( new Response( Response.Status.NOT_UNIQUE ), getSender() );
                    return;

                } else if ( owner == null ) {
                    // cannot cancel unique value that does not exist
                    getSender().tell( new Response( Response.Status.BAD_REQUEST ), getSender() );
                    return;
                }

                table.confirm( can.getApplicationScope(), can.getOwner(), can.getOwnerVersion(), can.getField() );

                getSender().tell( new Response( Response.Status.SUCCESS ), getSender() );

                mediator.tell( new DistributedPubSubMediator.Publish( "content",
                        new Reservation( can ) ), getSelf() );

            } catch (Throwable t) {
                getSender().tell( new Response( Response.Status.ERROR ), getSender() );
                logger.error( "Error processing request", t );
            }

        } else {
            unhandled( message );
        }
    }


    /**
     * UniqueValue actor receives and processes Requests.
     */
    public abstract static class Request implements Serializable {
        final ApplicationScope applicationScope;
        final Id owner;
        final UUID ownerVersion;
        final Field field;
        final String consistentHashKey;

        public Request( ApplicationScope applicationScope, Id owner, UUID ownerVersion, Field field ) {
            this.applicationScope = applicationScope;
            this.owner = owner;
            this.ownerVersion = ownerVersion;
            this.field = field;
            StringBuilder sb = new StringBuilder();
            sb.append( applicationScope.getApplication() );
            sb.append(":");
            sb.append( owner.getType() );
            sb.append(":");
            sb.append( field.getName() );
            sb.append(":");
            sb.append( field.getValue().toString() );
            this.consistentHashKey = sb.toString();
        }
        public Request( Request req ) {
            this.applicationScope = req.applicationScope;
            this.owner = req.owner;
            this.ownerVersion = req.ownerVersion;
            this.field = req.field;
            StringBuilder sb = new StringBuilder();
            sb.append( req.applicationScope.getApplication() );
            sb.append(":");
            sb.append( req.owner.getType() );
            sb.append(":");
            sb.append( req.field.getName() );
            sb.append(":");
            sb.append( req.field.getValue().toString() );
            this.consistentHashKey = sb.toString();

        }
        public ApplicationScope getApplicationScope() {
            return applicationScope;
        }
        public Id getOwner() {
            return owner;
        }
        public Field getField() {
            return field;
        }
        public String getConsistentHashKey() {
            return consistentHashKey;
        }
        public UUID getOwnerVersion() {
            return ownerVersion;
        }
    }

    /**
     * UniqueValue actor creates and sends Responses.
     */
    public static class Response implements Serializable {
        public enum Status { IS_UNIQUE, NOT_UNIQUE, SUCCESS, ERROR, BAD_REQUEST }
        final Status status;

        public Response(Status status) {
            this.status = status;
        }
        public Status getStatus() {
            return status;
        }
    }

    public static class Reservation extends Request implements Serializable {
        public Reservation( Request req ) {
            super( req );
        }
        public Reservation( ApplicationScope applicationScope, Id owner, UUID ownerVersion, Field field) {
            super( applicationScope, owner, ownerVersion, field );
        }
    }

    public static class Cancellation extends Request implements Serializable {
        public Cancellation( Request req ) {
            super( req );
        }
        public Cancellation( ApplicationScope applicationScope, Id owner, UUID ownerVersion, Field field) {
            super( applicationScope, owner, ownerVersion, field );
        }
    }

    public static class Confirmation extends Request implements Serializable {
        public Confirmation(Request req ) {
            super( req );
        }
        public Confirmation( ApplicationScope applicationScope, Id owner, UUID ownerVersion, Field field) {
            super( applicationScope, owner, ownerVersion, field );
        }
    }

}
