package org.apache.usergrid.persistence.collection.uniquevalues;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.ConsistentHashingRouter;
import akka.routing.FromConfig;


/**
 * Uses a consistent hash to route Unique Value requests to UniqueValueActors.
 */
public class ClusterSingletonRouter extends UntypedActor {

    private final ActorRef router;


    public ClusterSingletonRouter( String injectorName ) {
        router = getContext().actorOf(
                FromConfig.getInstance().props(Props.create(UniqueValueActor.class, injectorName )), "router");
    }

    @Override
    public void onReceive(Object message) {

        if ( message instanceof UniqueValueActor.Request) {
            UniqueValueActor.Request request = (UniqueValueActor.Request)message;

            ConsistentHashingRouter.ConsistentHashableEnvelope envelope =
                    new ConsistentHashingRouter.ConsistentHashableEnvelope( message, request.getRowKey() );
            router.tell( envelope, getSender());

        } else {
            unhandled(message);
        }
    }
}
