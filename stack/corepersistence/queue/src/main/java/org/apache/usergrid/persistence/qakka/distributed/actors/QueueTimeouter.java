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

package org.apache.usergrid.persistence.qakka.distributed.actors;

import akka.actor.UntypedActor;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.qakka.MetricsService;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.core.QakkaUtils;
import org.apache.usergrid.persistence.qakka.distributed.messages.QueueTimeoutRequest;
import org.apache.usergrid.persistence.qakka.serialization.MultiShardMessageIterator;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;


public class QueueTimeouter extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( QueueTimeouter.class );

    private final String name = RandomStringUtils.randomAlphanumeric( 4 );

    private final QueueMessageSerialization messageSerialization;
    private final MetricsService            metricsService;
    private final ActorSystemFig            actorSystemFig;
    private final QakkaFig                  qakkaFig;
    private final CassandraClient           cassandraClient;


    @Inject
    public QueueTimeouter(
        QueueMessageSerialization messageSerialization,
        MetricsService            metricsService,
        ActorSystemFig            actorSystemFig,
        QakkaFig                  qakkaFig,
        CassandraClient           cassandraClient
    ) {
        this.messageSerialization = messageSerialization;
        this.metricsService = metricsService;
        this.actorSystemFig = actorSystemFig;
        this.qakkaFig = qakkaFig;
        this.cassandraClient = cassandraClient;
    }


    @Override
    public void onReceive(Object message) {

        if ( message instanceof QueueTimeoutRequest) {

            Timer.Context timer = metricsService.getMetricRegistry().timer( MetricsService.TIMEOUT_TIME).time();

            try {

                QueueTimeoutRequest request = (QueueTimeoutRequest) message;

                String queueName = request.getQueueName();

                //logger.debug("Processing timeouts for queue {} ", queueName );

                int count = 0;
                String region = actorSystemFig.getRegionLocal();

                ShardIterator shardIterator = new ShardIterator(
                        cassandraClient, queueName, region, Shard.Type.INFLIGHT, Optional.empty());

                MultiShardMessageIterator multiShardIteratorInflight = new MultiShardMessageIterator(
                        cassandraClient, queueName, region, DatabaseQueueMessage.Type.INFLIGHT, shardIterator, null);

                while ( multiShardIteratorInflight.hasNext() ) {

                    DatabaseQueueMessage queueMessage = multiShardIteratorInflight.next();

                    long currentTime = System.currentTimeMillis();

                    if ((currentTime - queueMessage.getInflightAt()) > qakkaFig.getQueueTimeoutSeconds() * 1000) {

                        // put message back in messages_available table as new queue message with new UUID
                        messageSerialization.timeoutInflight( queueMessage );
                        count++;
                    }
                }

                if (count > 0) {
                    logger.debug( "{}: Timed out {} messages for queue {}", name, count, queueName );
                }

            } finally {
                timer.close();
            }

        } else {
            unhandled( message );
        }
    }
}
