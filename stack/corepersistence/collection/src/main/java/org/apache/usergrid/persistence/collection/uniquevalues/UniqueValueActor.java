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

import akka.actor.UntypedActor;
import com.google.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.usergrid.persistence.actorsystem.ActorSystemManager;
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

    private final ActorSystemManager actorSystemManager;

    private final UniqueValuesTable table;
    
    private final UniqueValuesFig uniqueValuesFig;

    private int count = 0;


    @Inject
    public UniqueValueActor( UniqueValuesTable table, ActorSystemManager actorSystemManager, UniqueValuesFig uniqueValuesFig) {

        this.uniqueValuesFig = uniqueValuesFig;
    	this.table = table;
        this.actorSystemManager = actorSystemManager;
    }

    @Override
    public void onReceive(Object message) {

        if ( message instanceof Request ) {
            Request req = (Request) message;

            count++;
            if (count % 10 == 0 && logger.isDebugEnabled()) {
                logger.debug( "UniqueValueActor {} processed {} requests", name, count );
            }
        }

        if ( message instanceof Reservation ) {
            Reservation res = (Reservation) message;

            // final Timer.Context context = metricsService.getReservationTimer().time();

            try {
                Id owner = table.lookupOwner( res.getApplicationScope(), res.getOwner().getType(), res.getField() );

                if ( owner != null && owner.equals( res.getOwner() )) {
                    // sender already owns this unique value
                    getSender().tell( new Response( Response.Status.IS_UNIQUE, res.getConsistentHashKey() ),
                        getSender() );
                    return;

                } else if ( owner != null && !owner.equals( res.getOwner() )) {
                    // tell sender value is not unique
                    getSender().tell( new Response( Response.Status.NOT_UNIQUE, res.getConsistentHashKey() ),
                        getSender() );
                    return;
                }

                table.reserve( res.getApplicationScope(), res.getOwner(), res.getOwnerVersion(), res.getField() );

                getSender().tell( new Response( Response.Status.IS_UNIQUE, res.getConsistentHashKey() ),
                    getSender() );
                
                if(uniqueValuesFig.getSkipRemoteRegions()) {
                	actorSystemManager.publishToLocalRegion( "content", new Reservation( res ), getSelf() );
                } else {
                	actorSystemManager.publishToAllRegions( "content", new Reservation( res ), getSelf() );
                }

            } catch (Throwable t) {

                getSender().tell( new Response( Response.Status.ERROR, res.getConsistentHashKey() ), getSender() );
                logger.error( "Error processing request", t );


            } finally {
                // context.stop();
            }

        } else if ( message instanceof Confirmation) {
            Confirmation con = (Confirmation) message;

            // final Timer.Context context = metricsService.getCommitmentTimer().time();

            try {
                Id owner = table.lookupOwner( con.getApplicationScope(), con.getOwner().getType(), con.getField() );

                if ( owner != null && !owner.equals( con.getOwner() )) {
                    // cannot reserve, somebody else owns the unique value
                    Response response  = new Response( Response.Status.NOT_UNIQUE, con.getConsistentHashKey());
                    getSender().tell( response, getSender() );
                    if(uniqueValuesFig.getSkipRemoteRegions()) {
                    	actorSystemManager.publishToLocalRegion( "content", response, getSelf() );
                    } else {
                    	actorSystemManager.publishToAllRegions( "content", response, getSelf() );
                    }
                    return;

                } else if ( owner == null ) {
                    // cannot commit without first reserving
                    Response response  = new Response( Response.Status.BAD_REQUEST, con.getConsistentHashKey());
                    getSender().tell( response, getSender() );
                    
                    if(uniqueValuesFig.getSkipRemoteRegions()) {
                    	actorSystemManager.publishToLocalRegion( "content", response, getSelf() );
                    } else {
                    	actorSystemManager.publishToAllRegions( "content", response, getSelf() );
                    }
                    
                    return;
                }

                table.confirm( con.getApplicationScope(), con.getOwner(), con.getOwnerVersion(), con.getField() );

                Response response = new Response( Response.Status.IS_UNIQUE, con.getConsistentHashKey() );
                getSender().tell( response, getSender() );

                if(uniqueValuesFig.getSkipRemoteRegions()) {
                	actorSystemManager.publishToLocalRegion( "content", response, getSelf() );
                } else {
                	actorSystemManager.publishToAllRegions( "content", response, getSelf() );
                }

            } catch (Throwable t) {
                getSender().tell( new Response( Response.Status.ERROR, con.getConsistentHashKey() ),
                    getSender() );
                logger.error( "Error processing request", t );

            } finally {
                // context.stop();
            }


        } else if ( message instanceof Cancellation ) {
            Cancellation can = (Cancellation) message;

            try {
                Id owner = table.lookupOwner( can.getApplicationScope(), can.getOwner().getType(), can.getField() );

                if ( owner != null && !owner.equals( can.getOwner() )) {
                    // cannot cancel, somebody else owns the unique value
                    getSender().tell( new Response( Response.Status.NOT_UNIQUE, can.getConsistentHashKey() ),
                        getSender() );
                    return;

                } else if ( owner == null ) {

                    // cannot cancel unique value that does not exist
                    getSender().tell( new Response( Response.Status.BAD_REQUEST, can.getConsistentHashKey() ),
                        getSender() );

                    // unique value record may have already been cleaned up, also clear cache
                    if(uniqueValuesFig.getSkipRemoteRegions()) {
                    	actorSystemManager.publishToLocalRegion( "content", new Cancellation( can ), getSelf() );
                    } else {
                    	actorSystemManager.publishToAllRegions( "content", new Cancellation( can ), getSelf() );
                    }

                    return;
                }

                table.cancel( can.getApplicationScope(), can.getOwner(), can.getOwnerVersion(), can.getField() );

                logger.debug("Removing {} from unique values table", can.getConsistentHashKey());

                getSender().tell( new Response( Response.Status.SUCCESS, can.getConsistentHashKey() ),
                    getSender() );

                if(uniqueValuesFig.getSkipRemoteRegions()) {
                	actorSystemManager.publishToLocalRegion( "content", new Cancellation( can ), getSelf() );
                } else {
                	actorSystemManager.publishToAllRegions( "content", new Cancellation( can ), getSelf() );
                }

            } catch (Throwable t) {
                getSender().tell( new Response( Response.Status.ERROR, can.getConsistentHashKey() ),
                    getSender() );
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
            sb.append( applicationScope.getApplication().getUuid() );
            sb.append(":");
            sb.append( owner.getType() );
            sb.append(":");
            sb.append( field.getName() );
            sb.append(":");
            sb.append( field.getValue().toString() );
            this.consistentHashKey = sb.toString();
        }
        public Request( Request req ) {
            this( req.getApplicationScope(), req.getOwner(), req.getOwnerVersion(), req.getField() );
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
        final String consistentHashKey;

        public Response(Status status, String consistentHashKey ) {
            this.status = status;
            this.consistentHashKey = consistentHashKey;
        }
        public Status getStatus() {
            return status;
        }
        public String getConsistentHashKey() {
            return consistentHashKey;
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
