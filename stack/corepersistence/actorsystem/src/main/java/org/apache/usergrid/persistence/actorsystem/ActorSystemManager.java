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


import akka.actor.ActorRef;

import java.util.Set;

public interface ActorSystemManager {

    /**
     * Start create and start all Akka Actors, ClusterClients Routers and etc.
     */
    void start();

    /**
     * Start method used in JUnit tests.
     */
    void start(String hostname, Integer port, String currentRegion);

    /**
     * Wait until ClientActor has seen some nodes and is ready for use.
     */
    void waitForClientActor();

    /**
     * True if ActorSystem and ClientActor are ready to be used.
     */
    boolean isReady();

    /**
     * MUST be called before start() to register any router producers to be configured.
     */
    void registerRouterProducer( RouterProducer routerProducer );

    /**
     * MUST be called before start() to register any messages to be sent.
     * @param messageType Class of message.
     * @param routerPath Router-path to which such messages are to be sent.
     */
    void registerMessageType( Class messageType, String routerPath );

    /**
     * Local client for ActorSystem, send all local messages here for routing.
     */
    ActorRef getClientActor();

    /**
     * Get ClientClient for specified remote region.
     */
    ActorRef getClusterClient(String region );

    /**
     * Get name of of this, the current region.
     */
    String getCurrentRegion();

    /**
     * Get all regions known to system.
     */
    public Set<String> getRegions();

    /**
     * Publish message to all topic subscribers in all regions.
     */
    void publishToAllRegions( String topic, Object message, ActorRef sender );
}
