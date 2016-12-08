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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.google.inject.Injector;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.actorsystem.GuiceActorProducer;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.App;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.core.CassandraClientImpl;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.distributed.messages.QueueTimeoutRequest;
import org.apache.usergrid.persistence.qakka.serialization.MultiShardMessageIterator;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardIterator;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardSerialization;
import org.apache.usergrid.persistence.qakka.core.QakkaUtils;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class QueueTimeouterTest extends AbstractTest {
    private static final Logger logger = LoggerFactory.getLogger( QueueTimeouterTest.class );


    @Test
    public void testBasicOperation() throws Exception {

        Injector injector = getInjector();

        injector.getInstance( DistributedQueueService.class ); // init the INJECTOR

        CassandraClient cassandraClient = injector.getInstance( CassandraClientImpl.class );
        QakkaFig qakkaFig             = injector.getInstance( QakkaFig.class );
        ActorSystemFig actorSystemFig = injector.getInstance( ActorSystemFig.class );
        QueueMessageSerialization qms = injector.getInstance( QueueMessageSerialization.class );
        ShardSerialization shardSerialization = injector.getInstance( ShardSerialization.class );

        // create records in inflight table, with some being old enough to time out

        int numInflight = 200; // number of messages to be put into timeout table
        int numTimedout = 75;  // number of messages to be timedout

        long timeoutMs = qakkaFig.getQueueTimeoutSeconds()*1000;

        String queueName = "qtt_queue_" + RandomStringUtils.randomAlphanumeric( 20 );

        Shard newShard = new Shard( queueName, actorSystemFig.getRegionLocal(),
                Shard.Type.INFLIGHT, 1L, QakkaUtils.getTimeUuid());
        shardSerialization.createShard( newShard );

        newShard = new Shard( queueName, actorSystemFig.getRegionLocal(),
                Shard.Type.DEFAULT, 1L, QakkaUtils.getTimeUuid());
        shardSerialization.createShard( newShard );

        for ( int i=0; i<numInflight; i++ ) {

            long created = System.currentTimeMillis();
            created = i < numTimedout ? created - timeoutMs: created + timeoutMs;

            UUID queueMessageId = QakkaUtils.getTimeUuid();

            UUID messageId = QakkaUtils.getTimeUuid();
            DatabaseQueueMessage message = new DatabaseQueueMessage(
                    messageId,
                    DatabaseQueueMessage.Type.INFLIGHT,
                    queueName,
                    actorSystemFig.getRegionLocal(),
                    null,
                    created,
                    created,
                    queueMessageId );

            qms.writeMessage( message );
        }

        List<DatabaseQueueMessage> inflightMessages = getDatabaseQueueMessages(
                cassandraClient, queueName, actorSystemFig.getRegionLocal(), Shard.Type.INFLIGHT );
        Assert.assertEquals( numInflight, inflightMessages.size() );

        // run timeouter actor

        ActorSystem system = ActorSystem.create("Test-" + queueName);
        ActorRef timeouterRef = system.actorOf( Props.create(
            GuiceActorProducer.class, QueueTimeouter.class), "timeouter");
        QueueTimeoutRequest qtr = new QueueTimeoutRequest( queueName );
        timeouterRef.tell( qtr, null ); // tell sends message, returns immediately

        Thread.sleep( timeoutMs );

        // timed out messages should have been moved into available (DEFAULT) table

        List<DatabaseQueueMessage> queuedMessages = getDatabaseQueueMessages(
                cassandraClient, queueName, actorSystemFig.getRegionLocal(), Shard.Type.DEFAULT);
        Assert.assertEquals( numTimedout, queuedMessages.size() );

        // and there should still be some messages in the INFLIGHT table

        inflightMessages = getDatabaseQueueMessages(
                cassandraClient, queueName, actorSystemFig.getRegionLocal(), Shard.Type.INFLIGHT );
        Assert.assertEquals( numInflight - numTimedout, inflightMessages.size() );

    }

    private List<DatabaseQueueMessage> getDatabaseQueueMessages(
            CassandraClient cassandraClient, String queueName, String region, Shard.Type type ) {

        ShardIterator shardIterator = new ShardIterator(
                cassandraClient, queueName, region, type, Optional.empty() );

        DatabaseQueueMessage.Type dbqmType = Shard.Type.DEFAULT.equals( type ) ?
                DatabaseQueueMessage.Type.DEFAULT : DatabaseQueueMessage.Type.INFLIGHT;

        MultiShardMessageIterator multiShardIterator = new MultiShardMessageIterator(
                cassandraClient, queueName, region, dbqmType, shardIterator, null);

        List<DatabaseQueueMessage> inflightMessages = new ArrayList<>(2000);
        while ( multiShardIterator.hasNext() && inflightMessages.size() < 2000 ) {
            DatabaseQueueMessage message = multiShardIterator.next();
            inflightMessages.add( message );
        }
        return inflightMessages;
    }
}
