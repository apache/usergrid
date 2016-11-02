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
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.qakka.MetricsService;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.core.impl.InMemoryQueue;
import org.apache.usergrid.persistence.qakka.distributed.messages.QueueRefreshRequest;
import org.apache.usergrid.persistence.qakka.serialization.MultiShardMessageIterator;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLogSerialization;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


public class QueueRefresher extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( QueueRefresher.class );

    private final ActorSystemFig  actorSystemFig;
    private final InMemoryQueue   inMemoryQueue;
    private final QakkaFig        qakkaFig;
    private final MetricsService  metricsService;
    private final CassandraClient cassandraClient;


    @Inject
    public QueueRefresher(
        ActorSystemFig  actorSystemFig,
        InMemoryQueue   inMemoryQueue,
        QakkaFig        qakkaFig,
        MetricsService  metricsService,
        CassandraClient cassandraClient
    ) {
        this.actorSystemFig  = actorSystemFig;
        this.inMemoryQueue   = inMemoryQueue;
        this.qakkaFig        = qakkaFig;
        this.metricsService  = metricsService;
        this.cassandraClient = cassandraClient;
    }


    @Override
    public void onReceive(Object message) {

        if ( message instanceof QueueRefreshRequest ) {

            QueueRefreshRequest request = (QueueRefreshRequest) message;
            String queueName = request.getQueueName();
            queueRefresh( queueName );

        } else {
            unhandled( message );
        }
    }

    Map<String, Long> startingShards = new HashMap<>();


    void queueRefresh( String queueName ) {

        Timer.Context timer = metricsService.getMetricRegistry().timer( MetricsService.REFRESH_TIME).time();

        try {

            if (inMemoryQueue.size( queueName ) < qakkaFig.getQueueInMemorySize()) {

                final Optional shardIdOptional;
                final String shardKey =
                    createShardKey( queueName, Shard.Type.DEFAULT, actorSystemFig.getRegionLocal() );
                Long shardId = startingShards.get( shardKey );

                if ( shardId != null ) {
                    shardIdOptional = Optional.of( shardId );
                } else {
                    shardIdOptional = Optional.empty();
                }

                ShardIterator shardIterator = new ShardIterator(
                    cassandraClient, queueName, actorSystemFig.getRegionLocal(),
                    Shard.Type.DEFAULT, shardIdOptional );

                UUID since = inMemoryQueue.getNewest( queueName );

                String region = actorSystemFig.getRegionLocal();

                MultiShardMessageIterator multiShardIterator = new MultiShardMessageIterator(
                    cassandraClient, queueName, region, DatabaseQueueMessage.Type.DEFAULT,
                    shardIterator, since);


                int need = qakkaFig.getQueueInMemorySize() - inMemoryQueue.size( queueName );
                int count = 0;

                while ( multiShardIterator.hasNext() && count < need ) {
                    DatabaseQueueMessage queueMessage = multiShardIterator.next();
                    inMemoryQueue.add( queueName, queueMessage );
                    count++;
                }

                startingShards.put( shardKey, shardId );

                logger.debug("Refreshed queue {} region {} shard {} since {} found {}",
                    queueName, region, shardId, since, count );
            }

        } finally {
            timer.close();
        }

    }

    private String createShardKey(String queueName, Shard.Type type, String region ) {
        return queueName + "_" + type + region;
    }

}
