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

import akka.actor.ActorSystem;

import java.util.Collection;
import java.util.Map;


/**
 * Interface used by ActorSystemManager to configure and create an Akka router.
 */
public interface RouterProducer {

    /**
     * Path to be used to send messages to this router.
     */
    String getRouterPath();

    /**
     * Returns all message types that should be sent to this router for routing.
     */
    Collection<Class> getMessageTypes();

    /**
     * Add configuration for the router to existing ActorSystem configuration.
     * Called before ActorSystem is created.
     */
    void addConfiguration(Map<String, Object> configMap );

    /**
     * Produce router and any supporting objects.
     * Called after ActorSystem is created.
     */
    void produceRouter( ActorSystem system, String role );
}
