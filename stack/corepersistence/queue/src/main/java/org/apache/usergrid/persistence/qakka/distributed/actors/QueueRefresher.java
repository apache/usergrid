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
import com.google.inject.Injector;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.qakka.App;
import org.apache.usergrid.persistence.qakka.MetricsService;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.core.CassandraClientImpl;
import org.apache.usergrid.persistence.qakka.core.impl.InMemoryQueue;
import org.apache.usergrid.persistence.qakka.distributed.messages.QueueRefreshRequest;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaRuntimeException;
import org.apache.usergrid.persistence.qakka.serialization.MultiShardMessageIterator;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;


public class QueueRefresher extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( QueueRefresher.class );

    private final String                    queueName;

    private final QueueMessageSerialization serialization;
    private final InMemoryQueue             inMemoryQueue;
    private final QakkaFig qakkaFig;
    private final ActorSystemFig            actorSystemFig;
    private final MetricsService            metricsService;
    private final CassandraClient cassandraClient;

    public QueueRefresher(String queueName ) {
        this.queueName = queueName;

        Injector injector = App.INJECTOR;

        serialization  = injector.getInstance( QueueMessageSerialization.class );
        inMemoryQueue  = injector.getInstance( InMemoryQueue.class );
        qakkaFig       = injector.getInstance( QakkaFig.class );
        actorSystemFig = injector.getInstance( ActorSystemFig.class );
        metricsService = injector.getInstance( MetricsService.class );
        cassandraClient = injector.getInstance( CassandraClientImpl.class );
    }


    @Override
    public void onReceive(Object message) {

        if ( message instanceof QueueRefreshRequest ) {

            QueueRefreshRequest request = (QueueRefreshRequest) message;

            if (!request.getQueueName().equals( queueName )) {
                throw new QakkaRuntimeException(
                        "QueueWriter for " + queueName + ": Incorrect queueName " + request.getQueueName() );
            }

            Timer.Context timer = metricsService.getMetricRegistry().timer( MetricsService.REFRESH_TIME).time();

            try {

                if (inMemoryQueue.size( queueName ) < qakkaFig.getQueueInMemorySize()) {

                    ShardIterator shardIterator = new ShardIterator(
                            cassandraClient, queueName, actorSystemFig.getRegionLocal(),
                            Shard.Type.DEFAULT, Optional.empty() );

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

                    if ( count > 0 ) {
                        logger.info( "Added {} in-memory for queue {}, new size = {}",
                                count, queueName, inMemoryQueue.size( queueName ) );
                    }
                }

            } finally {
                timer.close();
            }

        } else {
            unhandled( message );
        }
    }
}
