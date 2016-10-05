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

package org.apache.usergrid.persistence.qakka.distributed.impl;

import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import akka.cluster.singleton.ClusterSingletonProxy;
import akka.cluster.singleton.ClusterSingletonProxySettings;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.usergrid.persistence.actorsystem.ActorSystemManager;
import org.apache.usergrid.persistence.actorsystem.GuiceActorProducer;
import org.apache.usergrid.persistence.actorsystem.RouterProducer;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.distributed.actors.QueueSenderRouter;
import org.apache.usergrid.persistence.qakka.distributed.messages.QueueSendRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class QueueSenderRouterProducer implements RouterProducer {

    static Injector injector;
    ActorSystemManager actorSystemManager;
    QakkaFig qakkaFig;


    @Inject
    public QueueSenderRouterProducer(
            Injector injector,
            ActorSystemManager actorSystemManager,
            QakkaFig qakkaFig) {

        this.injector = injector;
        this.actorSystemManager = actorSystemManager;
        this.qakkaFig = qakkaFig;
    }


    @Override
    public String getRouterPath() {
        return "/user/queueSenderRouterProxy";
    }


    @Override
    public void produceRouter(ActorSystem system, String role) {

        ClusterSingletonManagerSettings settings =
                ClusterSingletonManagerSettings.create( system ).withRole( "io" );

        system.actorOf( ClusterSingletonManager.props(
                Props.create( GuiceActorProducer.class, QueueSenderRouter.class ),
                PoisonPill.getInstance(), settings ), "queueSenderRouter" );

        ClusterSingletonProxySettings proxySettings =
                ClusterSingletonProxySettings.create( system ).withRole( role );

        system.actorOf(
                ClusterSingletonProxy.props( "/user/queueSenderRouter", proxySettings ), "queueSenderRouterProxy" );
    }


    @Override
    public void addConfiguration(Map<String, Object> configMap) {

        int numInstancesPerNode = qakkaFig.getNumQueueSenderActors();

        Map<String, Object> akka = (Map<String, Object>) configMap.get( "akka" );
        final Map<String, Object> deploymentMap;

        if ( akka.get( "actor" ) == null ) {
            deploymentMap = new HashMap<>();
            akka.put( "actor", new HashMap<String, Object>() {{
                put( "deployment", deploymentMap );
            }} );

        } else if (((Map) akka.get( "actor" )).get( "deployment" ) == null) {
            deploymentMap = new HashMap<>();
            ((Map) akka.get( "actor" )).put( "deployment", deploymentMap );

        } else {
            deploymentMap = (Map<String, Object>) ((Map) akka.get( "actor" )).get( "deployment" );
        }

        deploymentMap.put( "/queueSenderRouter/singleton/router", new HashMap<String, Object>() {{
            put( "router", "round-robin-pool" );
            put( "cluster", new HashMap<String, Object>() {{
                put( "enabled", "on" );
                put( "allow-local-routees", "on" );
                put( "use-role", "io" );
                put( "max-nr-of-instances-per-node", numInstancesPerNode );
                put( "failure-detector", new HashMap<String, Object>() {{
                    put( "threshold", "10" );
                    put( "acceptable-heartbeat-pause", "3 s" );
                    put( "heartbeat-interval", "1 s" );
                    put( "heartbeat-request", new HashMap<String, Object>() {{
                        put( "expected-response-after", "3 s" );
                    }} );
                }} );
            }} );
        }} );

    }

    @Override
    public Collection<Class> getMessageTypes() {
        return Collections.singletonList( QueueSendRequest.class );
    }

}
