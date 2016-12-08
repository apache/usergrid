/**
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
package org.apache.usergrid.persistence.collection;

import org.apache.usergrid.persistence.actorsystem.ActorSystemManager;
import org.apache.usergrid.persistence.collection.uniquevalues.UniqueValueActor;
import org.apache.usergrid.persistence.collection.uniquevalues.UniqueValuesService;
import java.util.HashMap;
import java.util.Map;


public class AbstractUniqueValueTest {

    private static Map<Integer, Boolean> startedAkka = new HashMap<>();


    protected synchronized void initAkka(
        int port, ActorSystemManager actorSystemManager, UniqueValuesService uniqueValuesService ) {

        if ( startedAkka.get(port) == null ) {

            actorSystemManager.registerRouterProducer( uniqueValuesService );
            actorSystemManager.start( "localhost", port, "us-east" );
            actorSystemManager.waitForClientActor();

            startedAkka.put( port, true );
        }
    }

}
