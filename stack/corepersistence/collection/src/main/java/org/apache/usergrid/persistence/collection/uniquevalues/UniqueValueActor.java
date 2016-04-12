package org.apache.usergrid.persistence.collection.uniquevalues;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.UUID;

public class UniqueValueActor extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( UniqueValueActor.class );

    private final String name = RandomStringUtils.randomAlphanumeric( 4 );

    //private MetricsService metricsService;

    private UniqueValuesTable table = new UniqueValuesTableImpl();

    private ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();

    private int count = 0;


    public UniqueValueActor( String injectorName ) {

//        UniqueValuesService uniqueValuesService =
//                GuiceModule.getInjector( injectorName ).getInstance( UniqueValuesService.class );
//
//        terminateOnError = Boolean.parseBoolean( uniqueValuesService.getProperties()
//                .getProperty( "akka.unique-value-actor-terminate-on-error", "false" ) );
//
//        chaos = Boolean.parseBoolean( uniqueValuesService.getProperties()
//                .getProperty( "akka.test.chaos", "false" ) );

//        metricsService =
//                GuiceModule.getInjector( injectorName ).getInstance( MetricsService.class );
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
                UUID owner = table.lookupOwner( res.getType(), res.getPropertyName(), res.getPropertyValue() );

                if ( owner != null && owner.equals( res.getUuid() )) {
                    // sender already owns this unique value
                    getSender().tell( new Response( Response.Status.IS_UNIQUE ), getSender() );
                    return;

                } else if ( owner != null && !owner.equals( res.getUuid() )) {
                    // tell sender value is not unique
                    getSender().tell( new Response( Response.Status.NOT_UNIQUE ), getSender() );
                    return;
                }

                table.reserve( res.getUuid(), res.getType(), res.getPropertyName(), res.getPropertyValue() );

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
            Confirmation commit = (Confirmation) message;

//            final Timer.Context context = metricsService.getCommitmentTimer().time();

            try {
                UUID owner = table.lookupOwner(  commit.getType(), commit.getPropertyName(), commit.getPropertyValue() );

                if ( owner != null && !owner.equals( commit.getUuid() )) {
                    // cannot reserve, somebody else owns the unique value
                    getSender().tell( new Response( Response.Status.NOT_UNIQUE ), getSender() );
                    return;

                } else if ( owner == null ) {
                    // cannot commit without first reserving
                    getSender().tell( new Response( Response.Status.BAD_REQUEST ), getSender() );
                    return;
                }

                table.commit( commit.getUuid(), commit.getType(), commit.getPropertyName(), commit.getPropertyValue() );

                getSender().tell( new Response( Response.Status.IS_UNIQUE ), getSender() );

                mediator.tell( new DistributedPubSubMediator.Publish( "content",
                        new Reservation( commit ) ), getSelf() );

            } catch (Throwable t) {
                getSender().tell( new Response( Response.Status.ERROR ), getSender() );
                logger.error( "Error processing request", t );

            } finally {
//                context.stop();
            }


        } else if ( message instanceof Cancellation ) {
            Cancellation can = (Cancellation) message;

            try {
                UUID owner = table.lookupOwner(  can.getType(), can.getPropertyName(), can.getPropertyValue() );

                if ( owner != null && !owner.equals( can.getUuid() )) {
                    // cannot cancel, somebody else owns the unique value
                    getSender().tell( new Response( Response.Status.NOT_UNIQUE ), getSender() );
                    return;

                } else if ( owner == null ) {
                    // cannot cancel unique value that does not exist
                    getSender().tell( new Response( Response.Status.BAD_REQUEST ), getSender() );
                    return;
                }

                table.cancel( can.getType(), can.getPropertyName(), can.getPropertyValue() );

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
        final UUID uuid;
        final String type;
        final String propertyName;
        final String propertyValue;
        final String rowKey;

        public Request(UUID uuid, String type, String propertyName, String value) {
            this.uuid = uuid;
            this.type = type;
            this.propertyName = propertyName;
            this.propertyValue = value;
            this.rowKey = getType() + ":" + getPropertyName() + ":" + getPropertyValue();
        }
        public Request( Request req ) {
            this.uuid = req.uuid;
            this.type = req.type;
            this.propertyName = req.propertyName;
            this.propertyValue = req.propertyValue;
            this.rowKey = getType() + ":" + getPropertyName() + ":" + getPropertyValue();

        }
        public String getRowKey() {
            return rowKey;
        }
        public UUID getUuid() {
            return uuid;
        }
        public String getType() {
            return type;
        }
        public String getPropertyName() {
            return propertyName;
        }
        public String getPropertyValue() {
            return propertyValue;
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
        public Reservation(UUID uuid, String type, String username, String value) {
            super( uuid, type, username, value );
        }
    }

    public static class Cancellation extends Request implements Serializable {
        public Cancellation( Request req ) {
            super( req );
        }
        public Cancellation(UUID uuid, String type, String username, String value) {
            super( uuid, type, username, value );
        }
    }

    public static class Confirmation extends Request implements Serializable {
        public Confirmation(Request req ) {
            super( req );
        }
        public Confirmation(UUID uuid, String type, String username, String value) {
            super( uuid, type, username, value );
        }
    }

}
