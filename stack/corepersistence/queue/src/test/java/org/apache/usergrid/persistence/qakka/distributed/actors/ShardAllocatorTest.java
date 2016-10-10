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
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.actorsystem.GuiceActorProducer;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.App;
import org.apache.usergrid.persistence.qakka.QakkaModule;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.*;
import org.apache.usergrid.persistence.qakka.distributed.messages.ShardCheckRequest;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardCounterSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardIterator;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardSerialization;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.queue.TestModule;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;


public class ShardAllocatorTest extends AbstractTest {
    private static final Logger logger = LoggerFactory.getLogger( QueueReaderTest.class );


    @Override
    protected Injector getInjector() {
        return Guice.createInjector( new TestModule() );
    }


    @Test
    public void testBasicOperation() throws InterruptedException {

        Injector injector = getInjector();

        CassandraClient cassandraClient = injector.getInstance( CassandraClientImpl.class );

        injector.getInstance( DistributedQueueService.class ); // init the INJECTOR

        ShardSerialization shardSer = injector.getInstance( ShardSerialization.class );
        QakkaFig qakkaFig           = injector.getInstance( QakkaFig.class );

        ActorSystemFig            actorSystemFig  = injector.getInstance( ActorSystemFig.class );
        ShardCounterSerialization shardCounterSer = injector.getInstance( ShardCounterSerialization.class );

        String rando = RandomStringUtils.randomAlphanumeric( 20 );

        String queueName = "queue_" + rando;
        String region = actorSystemFig.getRegionLocal();

        // Create a set of shards, each with max count

        Shard lastShard = null;

        int numShards = 4;
        long maxPerShard = qakkaFig.getMaxShardSize();

        for ( long shardId = 1; shardId < numShards + 1; shardId++ ) {

            Shard shard = new Shard( queueName, region, Shard.Type.DEFAULT, shardId, QakkaUtils.getTimeUuid());
            shardSer.createShard( shard );

            if ( shardId != numShards ) {
                shardCounterSer.incrementCounter( queueName, Shard.Type.DEFAULT, shardId, maxPerShard );

            } else {
                // Create last shard with %20 less than max
                shardCounterSer.incrementCounter( queueName, Shard.Type.DEFAULT, shardId, (long)(0.8 * maxPerShard) );
                lastShard = shard;
            }

            Thread.sleep( 10 );
        }

        Assert.assertEquals( numShards, countShards(
                cassandraClient, shardCounterSer, queueName, region, Shard.Type.DEFAULT ));

        // Run shard allocator actor by sending message to it

        ActorSystem system = ActorSystem.create("Test-" + queueName);
        ActorRef shardAllocRef = system.actorOf( Props.create(
            GuiceActorProducer.class, ShardAllocator.class), "shardallocator");

        ShardCheckRequest checkRequest = new ShardCheckRequest( queueName );
        shardAllocRef.tell( checkRequest, null ); // tell sends message, returns immediately
        Thread.sleep(1000);

        // Test that no new shards created

        Assert.assertEquals( numShards, countShards(
                cassandraClient, shardCounterSer, queueName, region, Shard.Type.DEFAULT ));

        // Increment last shard by 20% of max

        shardCounterSer.incrementCounter(
                queueName, Shard.Type.DEFAULT, lastShard.getShardId(), (long)(0.2 * maxPerShard) );

        // Run shard allocator again

        shardAllocRef.tell( checkRequest, null ); // tell sends message, returns immediately
        Thread.sleep(1000);

        // Test that, this time, a new shard was created

        Assert.assertEquals( numShards + 1, countShards(
                cassandraClient, shardCounterSer, queueName, region, Shard.Type.DEFAULT ));
    }


    int countShards(
            CassandraClient cassandraClient,
            ShardCounterSerialization scs,
            String queueName,
            String region,
            Shard.Type type ) {

        ShardIterator shardIterator =
                new ShardIterator( cassandraClient, queueName, region, type, Optional.empty() );

        int shardCount = 0;
        while ( shardIterator.hasNext() ) {
            Shard s = shardIterator.next();
            shardCount++;
            long counterValue = scs.getCounterValue( s.getQueueName(), type, s.getShardId() );
            logger.debug("Shard {} {} is #{} has count={}", type, s.getShardId(), shardCount, counterValue );

        }

        return shardCount;
    }


    @Test
    public void testBasicOperationWithMessages() throws InterruptedException {

        Injector injector = getInjector();

        CassandraClient cassandraClient = injector.getInstance( CassandraClientImpl.class );

        injector.getInstance( App.class ); // init the INJECTOR

        QakkaFig            qakkaFig              = injector.getInstance( QakkaFig.class );
        ActorSystemFig      actorSystemFig        = injector.getInstance( ActorSystemFig.class );
        QueueManager        queueManager          = injector.getInstance( QueueManager.class );
        QueueMessageManager queueMessageManager   = injector.getInstance( QueueMessageManager.class );
        DistributedQueueService distributedQueueService = injector.getInstance( DistributedQueueService.class );
        ShardCounterSerialization shardCounterSer = injector.getInstance( ShardCounterSerialization.class );

        Assert.assertEquals( "test assumes 'queue.shard.max.size' is 15 ", 15, qakkaFig.getMaxShardSize() );

        String region = actorSystemFig.getRegionLocal();
        App app = injector.getInstance( App.class );
        app.start( "localhost", getNextAkkaPort(), region );

        String rando = RandomStringUtils.randomAlphanumeric( 20 );
        String queueName = "queue_" + rando;

        queueManager.createQueue( new Queue( queueName ));

        try {

            // Create number of messages

            int numMessages = 400;

            for (int i = 0; i < numMessages; i++) {
                queueMessageManager.sendMessages(
                    queueName,
                    Collections.singletonList( region ),
                    null, // delay
                    null, // expiration
                    "application/json",
                    DataType.serializeValue( "{}", ProtocolVersion.NEWEST_SUPPORTED ) );
                Thread.sleep( 10 );
            }

            distributedQueueService.refresh();

            // Test that right number of shards created
            int shardCount = countShards( cassandraClient, shardCounterSer, queueName, region, Shard.Type.DEFAULT );
            Assert.assertTrue( "shards > 10", shardCount > 10 );
            Assert.assertTrue( "shards < 20", shardCount < 20 );

        } finally {
            queueManager.deleteQueue( queueName );
        }
    }
}
