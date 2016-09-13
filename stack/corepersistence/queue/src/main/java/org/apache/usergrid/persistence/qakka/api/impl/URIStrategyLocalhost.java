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
package org.apache.usergrid.persistence.qakka.api.impl;

import com.google.inject.Inject;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.qakka.api.URIStrategy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;


/** TODO: implement a "real" URI strategy */
public class URIStrategyLocalhost implements URIStrategy {

    final private String hostname;

    @Inject
    public URIStrategyLocalhost( ActorSystemFig actorSystemFig ) {
        this.hostname = actorSystemFig.getHostname();
    }

    @Override
    public URI queueURI(String queueName) throws URISyntaxException {
        return new URI("http://" + hostname + ":8080/api/queues/" + queueName);
    }

    @Override
    public URI queueMessageDataURI(String queueName, UUID queueMessageId) throws URISyntaxException {
        return new URI("http://" + hostname + ":8080/api/queues/" + queueName + "/data/" + queueMessageId );
    }
}
