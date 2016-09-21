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
import org.apache.usergrid.persistence.qakka.core.QakkaUtils;
import org.apache.usergrid.persistence.qakka.distributed.messages.QueueTimeoutRequest;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaRuntimeException;
import org.apache.usergrid.persistence.qakka.serialization.MultiShardMessageIterator;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.MessageCounterSerialization;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;


public class QueueTimeouter extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( QueueTimeouter.class );

    private final String                    queueName;

    private final QueueMessageSerialization messageSerialization;
    private final MetricsService            metricsService;
    private final ActorSystemFig            actorSystemFig;
    private final QakkaFig qakkaFig;
    private final CassandraClient           cassandraClient;

    private final MessageCounterSerialization messageCounterSerialization;


    public QueueTimeouter(String queueName ) {
        this.queueName = queueName;

        Injector injector = App.INJECTOR;

        messageSerialization = injector.getInstance( QueueMessageSerialization.class );
        actorSystemFig       = injector.getInstance( ActorSystemFig.class );
        qakkaFig             = injector.getInstance( QakkaFig.class );
        metricsService       = injector.getInstance( MetricsService.class );
        cassandraClient      = injector.getInstance( CassandraClientImpl.class );

        messageCounterSerialization = injector.getInstance( MessageCounterSerialization.class );
    }


    @Override
    public void onReceive(Object message) {

        if ( message instanceof QueueTimeoutRequest) {

            Timer.Context timer = metricsService.getMetricRegistry().timer( MetricsService.TIMEOUT_TIME).time();

            try {

                QueueTimeoutRequest request = (QueueTimeoutRequest) message;

                if (!request.getQueueName().equals( queueName )) {
                    throw new QakkaRuntimeException(
                            "QueueTimeouter for " + queueName + ": Incorrect queueName " + request.getQueueName() );
                }

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

                        UUID newQueueMessageId = QakkaUtils.getTimeUuid();

                        DatabaseQueueMessage newMessage = new DatabaseQueueMessage(
                                queueMessage.getMessageId(),
                                DatabaseQueueMessage.Type.DEFAULT,
                                queueMessage.getQueueName(),
                                queueMessage.getRegion(),
                                null,
                                queueMessage.getQueuedAt(),
                                queueMessage.getInflightAt(),
                                newQueueMessageId );

                        messageSerialization.writeMessage( newMessage );

                        // remove message from inflight table

                        messageSerialization.deleteMessage(
                                queueName,
                                actorSystemFig.getRegionLocal(),
                                null,
                                DatabaseQueueMessage.Type.INFLIGHT,
                                queueMessage.getQueueMessageId() );

                        count++;
                    }
                }

                if (count > 0) {
                    logger.debug( "Timed out {} messages for queue {}", count, queueName );

                    messageCounterSerialization.decrementCounter(
                        queueName, DatabaseQueueMessage.Type.DEFAULT, count);
                }

            } finally {
                timer.close();
            }

        } else {
            unhandled( message );
        }
    }
}
