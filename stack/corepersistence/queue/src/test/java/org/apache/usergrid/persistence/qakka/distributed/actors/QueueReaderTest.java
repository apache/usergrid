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
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.CassandraClientImpl;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.App;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.core.QakkaUtils;
import org.apache.usergrid.persistence.qakka.core.impl.InMemoryQueue;
import org.apache.usergrid.persistence.qakka.distributed.messages.QueueRefreshRequest;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardSerialization;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;


public class QueueReaderTest extends AbstractTest {
    private static final Logger logger = LoggerFactory.getLogger( QueueReaderTest.class );



    @Test
    public void testBasicOperation() throws Exception {

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );


        getInjector().getInstance( App.class ); // init the INJECTOR

        QakkaFig qakkaFig = getInjector().getInstance( QakkaFig.class );
        ActorSystemFig actorSystemFig = getInjector().getInstance( ActorSystemFig.class );
        ShardSerialization shardSerialization = getInjector().getInstance( ShardSerialization.class );

        int numMessages = 200;
        // create queue messages, only first lot get queue message data

        QueueMessageSerialization serialization = getInjector().getInstance( QueueMessageSerialization.class );
        String queueName = "qrt_queue_" + RandomStringUtils.randomAlphanumeric( 10 );

        Shard newShard = new Shard( queueName, actorSystemFig.getRegionLocal(),
                Shard.Type.DEFAULT, 1L, QakkaUtils.getTimeUuid());
        shardSerialization.createShard( newShard );

        for ( int i=0; i<numMessages; i++ ) {

            UUID messageId = QakkaUtils.getTimeUuid();
            UUID queueMessageId = QakkaUtils.getTimeUuid();

            DatabaseQueueMessage message = new DatabaseQueueMessage(
                    messageId,
                    DatabaseQueueMessage.Type.DEFAULT,
                    queueName,
                    actorSystemFig.getRegionLocal(),
                    null,
                    System.currentTimeMillis(),
                    null,
                    queueMessageId);
            serialization.writeMessage( message );
        }

        InMemoryQueue inMemoryQueue = getInjector().getInstance( InMemoryQueue.class );
        Assert.assertEquals( 0, inMemoryQueue.size( queueName ) );

        // run the QueueRefresher to fill up the in-memory queue

        ActorSystem system = ActorSystem.create("Test-" + queueName);
        ActorRef queueReaderRef = system.actorOf( Props.create( QueueRefresher.class, queueName ), "queueReader");
        QueueRefreshRequest refreshRequest = new QueueRefreshRequest( queueName );

        // need to wait for refresh to complete
        int maxRetries = 10;
        int retries = 0;
        while ( inMemoryQueue.size( queueName ) < qakkaFig.getQueueInMemorySize() && retries++ < maxRetries ) {
            queueReaderRef.tell( refreshRequest, null ); // tell sends message, returns immediately
            Thread.sleep(1000);
        }

        Assert.assertEquals( numMessages, inMemoryQueue.size( queueName ) );
    }
}
