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
package org.apache.usergrid.persistence.actorsystem;

import akka.actor.ActorSelection;
import akka.actor.Address;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Once notified of nodes, sends unique propertyValue requests to ClusterSingletonRouter via it's local proxy.
 */
public class ClientActor extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( ClientActor.class );

    private final String name = RandomStringUtils.randomAlphanumeric( 4 );

    private final Set<Address>       nodes = new HashSet<>();
    private final Cluster            cluster = Cluster.get(getContext().system());
    private final Map<Class, String> routersByMessageType;

    private boolean ready = false;


    public ClientActor( Map<Class, String> routersByMessageType ) {
        this.routersByMessageType = routersByMessageType;
    }

    // subscribe to cluster changes, MemberEvent
    @Override
    public void preStart() {
        logger.debug("{} role {} address {}:{} starting up, subscribing to cluster events...", name,
            cluster.getSelfRoles().iterator().next(),
            cluster.readView().selfAddress().host(),
            cluster.readView().selfAddress().hostPort());
        cluster.subscribe(getSelf(), ClusterEvent.MemberEvent.class, ClusterEvent.ReachabilityEvent.class);
    }

    // re-subscribe when restart
    @Override
    public void postStop() {
        cluster.unsubscribe(getSelf());
    }

    @Override
    public void onReceive(Object message) {

        int startSize = nodes.size();

        String routerPath = routersByMessageType.get( message.getClass() );

        if ( routerPath != null && ready ) {

            // just pick any node, the ClusterSingletonRouter will do the consistent hash routing
            List<Address> nodesList = new ArrayList<>( nodes );
            Address address = nodesList.get( ThreadLocalRandom.current().nextInt( nodesList.size() ) );
            ActorSelection service = getContext().actorSelection( address + routerPath );
            service.tell( message, getSender() );

        } else if ( routerPath != null ) {

            logger.debug("{} responding with status unknown", name);
            getSender().tell( new ErrorResponse("ClientActor not ready"), getSender() );

        } else if ( message instanceof StatusRequest ) {
            if ( ready ) {
                getSender().tell( new StatusMessage( name, StatusMessage.Status.READY ), getSender() );
            } else {
                getSender().tell( new StatusMessage( name, StatusMessage.Status.INITIALIZING), getSender() );
            }
            return;

        } else {
            processAsClusterEvent( message );
        }

        if ( logger.isDebugEnabled() && startSize != nodes.size() ) {
            logger.debug( "{} now knows {} nodes", name, nodes.size() );
        }

        if (!nodes.isEmpty() && !ready) {
            logger.debug( name + " is ready" );
            ready = true;

        } else if (nodes.isEmpty() && ready) {
            ready = false;
        }
    }

    /**
     * Process messages about nodes up, down, reachable and unreachable.
     */
    private void processAsClusterEvent(Object message) {

        if (message instanceof ClusterEvent.CurrentClusterState) {
            ClusterEvent.CurrentClusterState state = (ClusterEvent.CurrentClusterState) message;
            nodes.clear();
            for (Member member : state.getMembers()) {
                if (member.hasRole("io") && member.status().equals( MemberStatus.up())) {
                    nodes.add(member.address());
                    logger.debug("RequestActor {} received cluster-state member-up for {}", name, member.address());
                }
            }

        } else if (message instanceof ClusterEvent.MemberUp) {
            ClusterEvent.MemberUp mUp = (ClusterEvent.MemberUp) message;
            if (mUp.member().hasRole("io")) {
                nodes.add( mUp.member().address() );
            }
            logger.debug("{} received member-up for {}", name, mUp.member().address());

        } else if (message instanceof ClusterEvent.MemberEvent) {
            ClusterEvent.MemberEvent other = (ClusterEvent.MemberEvent) message;
            nodes.remove(other.member().address());

        } else if (message instanceof ClusterEvent.UnreachableMember) {
            ClusterEvent.UnreachableMember unreachable = (ClusterEvent.UnreachableMember) message;
            nodes.remove(unreachable.member().address());
            logger.debug("{} received un-reachable for {}", name, unreachable.member().address());

        } else if (message instanceof ClusterEvent.ReachableMember) {
            ClusterEvent.ReachableMember reachable = (ClusterEvent.ReachableMember) message;
            if (reachable.member().hasRole("io")) {
                nodes.add( reachable.member().address() );
            }
            logger.debug("{} received reachable for {}", name, reachable.member().address());

        } else {
            logger.error("{}: unhandled message: {}", name, message.toString());
            unhandled(message);
        }
    }

    /**
     * RequestAction responds to StatusRequests.
     */
    public static class StatusRequest implements Serializable { }

    /**
     * RequestActor responds with, and some times unilaterally sends StatusMessages.
     */
    public static class StatusMessage implements Serializable {
        final String name;

        public Status getStatus() {
            return status;
        }

        public enum Status { INITIALIZING, READY }
        private final Status status;
        public StatusMessage(String name, Status status) {
            this.name = name;
            this.status = status;
        }
        public String getName() {
            return name;
        }
        public boolean isReady() {
            return status.equals( Status.READY );
        }
    }


    public static class ErrorResponse implements Serializable {
        private String message;
        public ErrorResponse( String message ) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

}

