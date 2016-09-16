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

package org.apache.usergrid.persistence.qakka.serialization;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.core.CassandraFig;
import org.apache.usergrid.persistence.qakka.core.CassandraClientImpl;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.core.QakkaUtils;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardIterator;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.impl.ShardSerializationImpl;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Created by russo on 6/9/16.
 */
public class MultiShardDatabaseQueueMessageIteratorTest extends AbstractTest {
    private static final Logger logger = LoggerFactory.getLogger( MultiShardDatabaseQueueMessageIteratorTest.class );


    @Test
    public void testIterator() throws InterruptedException {

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );
        CassandraFig cassandraFig = getInjector().getInstance( CassandraFig.class );
        ShardSerialization shardSerialization = new ShardSerializationImpl( cassandraFig, cassandraClient );

        QueueMessageSerialization queueMessageSerialization =
                getInjector().getInstance( QueueMessageSerialization.class );

        String queueName = "queue_msit_" + RandomStringUtils.randomAlphanumeric( 10 );

        Shard shard1 = new Shard(queueName, "region", Shard.Type.DEFAULT, 1L, null);
        Shard shard2 = new Shard(queueName, "region", Shard.Type.DEFAULT, 2L, null);
        Shard shard3 = new Shard(queueName, "region", Shard.Type.DEFAULT, 3L, null);
        Shard shard4 = new Shard(queueName, "region", Shard.Type.DEFAULT, 4L, null);

        shardSerialization.createShard(shard1);
        shardSerialization.createShard(shard2);
        shardSerialization.createShard(shard3);
        shardSerialization.createShard(shard4);

        final int numMessagesPerShard = 50;

        // just do these separately to space out the time UUIDs per shard
        for(int i=0; i < numMessagesPerShard; i++){

            queueMessageSerialization.writeMessage( new DatabaseQueueMessage(QakkaUtils.getTimeUuid(),
                    DatabaseQueueMessage.Type.DEFAULT, queueName, "region", shard1.getShardId(),
                    System.currentTimeMillis(), null, null));
            Thread.sleep(3);
        }

        for(int i=0; i < numMessagesPerShard; i++){

            queueMessageSerialization.writeMessage( new DatabaseQueueMessage(QakkaUtils.getTimeUuid(),
                    DatabaseQueueMessage.Type.DEFAULT, queueName, "region", shard2.getShardId(),
                    System.currentTimeMillis(), null, null));
            Thread.sleep(3);
        }

        for(int i=0; i < numMessagesPerShard; i++){

            queueMessageSerialization.writeMessage( new DatabaseQueueMessage(QakkaUtils.getTimeUuid(),
                    DatabaseQueueMessage.Type.DEFAULT, queueName, "region", shard3.getShardId(),
                    System.currentTimeMillis(), null, null));
            Thread.sleep(3);
        }

        for(int i=0; i < numMessagesPerShard; i++){

            queueMessageSerialization.writeMessage( new DatabaseQueueMessage(QakkaUtils.getTimeUuid(),
                    DatabaseQueueMessage.Type.DEFAULT, queueName, "region", shard4.getShardId(),
                    System.currentTimeMillis(), null, null));
            Thread.sleep(3);
        }


        ShardIterator shardIterator = new ShardIterator(
                cassandraClient, queueName, "region", Shard.Type.DEFAULT, Optional.empty());
        MultiShardMessageIterator iterator = new MultiShardMessageIterator(
                cassandraClient, queueName, "region", DatabaseQueueMessage.Type.DEFAULT, shardIterator, null);

        final AtomicInteger[] counts = {
                new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0) };

        iterator.forEachRemaining(message -> {
            //logger.info("Shard ID: {}, DatabaseQueueMessage ID: {}", message.getShardId(), message.getMessageId());
            counts[ (int)(message.getShardId() - 1) ] .incrementAndGet();
        });

        logger.info("Total Count 1: {}", counts[0].get());
        logger.info("Total Count 2: {}", counts[1].get());
        logger.info("Total Count 3: {}", counts[2].get());
        logger.info("Total Count 4: {}", counts[3].get());

        assertEquals(numMessagesPerShard, counts[0].get());
        assertEquals(numMessagesPerShard, counts[1].get());
        assertEquals(numMessagesPerShard, counts[2].get());
        assertEquals(numMessagesPerShard, counts[3].get());
    }
}
