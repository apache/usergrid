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
package org.apache.usergrid.persistence.actorsystem;


import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterListener extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger( ClusterListener.class );

    private final Cluster cluster = Cluster.get(getContext().system());

    private final ListMultimap<String, String> seedsByRegion;

    private final String currentRegion;

    public ClusterListener( ListMultimap<String, String> seedsByRegion, String currentRegion ){

        // providing these to the lister as they may be used in near future to handle custom logic on member events
        this.seedsByRegion = seedsByRegion;
        this.currentRegion = currentRegion;
    }

    @Override
    public void preStart() {
        // subscribe to all cluster events that we might take action/logging on
        cluster.subscribe(getSelf(),
            ClusterEvent.initialStateAsEvents(),
            ClusterEvent.MemberEvent.class,
            ClusterEvent.UnreachableMember.class,
            ClusterEvent.MemberUp.class,
            ClusterEvent.MemberWeaklyUp.class);
    }

    @Override
    public void postStop() {
        // purposely do not unsubscribe so other actors will continue to receive cluster events
        cluster.unsubscribe(getSelf());
    }

    @Override
    public void onReceive(Object message) throws Exception {

        if (message instanceof ClusterEvent.MemberUp) {
                ClusterEvent.MemberUp event = (ClusterEvent.MemberUp) message;
                logger.info("Member is Up: {}, hostname: {}", event.member(),  event.member().address().host().get() );

        } else if (message instanceof ClusterEvent.UnreachableMember) {
            ClusterEvent.UnreachableMember event = (ClusterEvent.UnreachableMember) message;
            logger.info("Member detected as unreachable: {}", event.member());

            String hostname = event.member().address().host().get();

            // invoke a ping because InetAddress requires root privleges which the JVM often does not have
            boolean networkReachable =
                java.lang.Runtime.getRuntime().exec("ping -c 1 "+hostname).waitFor() == 0;
            if(networkReachable){

                logger.info("Unreachable member {} is accessible on the network.", event.member());

//                logger.info("Unreachable member {} is accessible on the network, " +
//                    "application must have died. Removing member ", event.member());
//
//                cluster.leave(event.member().address());
            }else{

                logger.warn("Unreachable member {} is not accessible on the network, " +
                    "there must be a network issue. Not removing member", event.member());

            }

        } else if (message instanceof ClusterEvent.MemberRemoved) {
            ClusterEvent.MemberRemoved event = (ClusterEvent.MemberRemoved) message;
            logger.info("Member is Removed: {}", event.member());

        } else if (message instanceof ClusterEvent.MemberEvent) {
            ClusterEvent.MemberEvent event = (ClusterEvent.MemberEvent) message;
            logger.info("MemberEvent occurred for member: {}, Event: {}", event.member(), event.toString());

        } else if (message instanceof ClusterEvent.LeaderChanged) {
            ClusterEvent.LeaderChanged event = (ClusterEvent.LeaderChanged) message;
            logger.info("LeaderChanged occurred for leader: {}, getLeader: {}, Event: {}",
                event.leader(), event.getLeader(), event.toString());

        } else if (message instanceof ClusterEvent.MemberExited) {
            ClusterEvent.MemberExited event = (ClusterEvent.MemberExited) message;
            logger.info("MemberExited occurred for member: {}, Event: {}", event.member(), event.toString());

        } else {
            unhandled(message);
        }

    }
}
