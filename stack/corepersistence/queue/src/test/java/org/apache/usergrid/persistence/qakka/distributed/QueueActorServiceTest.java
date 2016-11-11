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

package org.apache.usergrid.persistence.qakka.distributed;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.jcip.annotations.NotThreadSafe;
import org.apache.cassandra.utils.UUIDGen;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.App;
import org.apache.usergrid.persistence.qakka.QakkaModule;
import org.apache.usergrid.persistence.qakka.core.*;
import org.apache.usergrid.persistence.qakka.core.impl.InMemoryQueue;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessageBody;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.apache.usergrid.persistence.qakka.serialization.transferlog.TransferLogSerialization;
import org.apache.usergrid.persistence.queue.TestModule;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.UUID;


@NotThreadSafe
public class QueueActorServiceTest extends AbstractTest {
    private static final Logger logger = LoggerFactory.getLogger( QueueActorServiceTest.class );


    @Override
    protected Injector getInjector() {
        return Guice.createInjector( new TestModule() );
    }


    @Test
    public void testBasicOperation() throws Exception {

        Injector injector = getInjector();

        ActorSystemFig actorSystemFig = injector.getInstance( ActorSystemFig.class );
        String region = actorSystemFig.getRegionLocal();

        App app = injector.getInstance( App.class );
        app.start( "localhost", getNextAkkaPort(), region );

        DistributedQueueService distributedQueueService = injector.getInstance( DistributedQueueService.class );
        QueueMessageSerialization serialization = injector.getInstance( QueueMessageSerialization.class );

        String queueName = "testqueue_" + UUID.randomUUID();
        QueueManager queueManager = injector.getInstance( QueueManager.class );
        queueManager.createQueue( new Queue( queueName, "test-type", region, region, 0L, 5, 10, null ));

        try {

            // send 1 queue message, get back one queue message
            UUID messageId = UUIDGen.getTimeUUID();

            final String data = "my test data";
            final DatabaseQueueMessageBody messageBody = new DatabaseQueueMessageBody(
                DataType.serializeValue( data, ProtocolVersion.NEWEST_SUPPORTED ), "text/plain" );
            serialization.writeMessageData( messageId, messageBody );

            distributedQueueService.sendMessageToRegion(
                queueName, region, region, messageId, null, null );

            distributedQueueService.refresh();
            Thread.sleep( 1000 );

            Collection<DatabaseQueueMessage> qmReturned = distributedQueueService.getNextMessages( queueName, 1 );
            Assert.assertEquals( 1, qmReturned.size() );

            DatabaseQueueMessage dqm = qmReturned.iterator().next();
            DatabaseQueueMessageBody dqmb = serialization.loadMessageData( dqm.getMessageId() );
            ByteBuffer blob = dqmb.getBlob();

            String returnedData = new String( blob.array(), "UTF-8" );

            Assert.assertEquals( data, returnedData );

            distributedQueueService.shutdown();

        } finally {
            queueManager.deleteQueue( queueName );
        }

    }


    @Test
    public void testGetMultipleQueueMessages() throws InterruptedException {

        Injector injector = getInjector();

        ActorSystemFig actorSystemFig = injector.getInstance( ActorSystemFig.class );
        String region = actorSystemFig.getRegionLocal();

        App app = injector.getInstance( App.class );
        app.start("localhost", getNextAkkaPort(), region);

        DistributedQueueService distributedQueueService = injector.getInstance( DistributedQueueService.class );
        QueueMessageSerialization serialization         = injector.getInstance( QueueMessageSerialization.class );
        TransferLogSerialization xferLogSerialization   = injector.getInstance( TransferLogSerialization.class );
        QueueMessageManager queueMessageManager         = injector.getInstance( QueueMessageManager.class );

        String queueName = "queue_testGetMultipleQueueMessages_" + UUID.randomUUID();
        QueueManager queueManager = injector.getInstance( QueueManager.class );

        try {

            queueManager.createQueue(
                new Queue( queueName, "test-type", region, region, 0L, 5, 10, null ) );

            for (int i = 0; i < 100; i++) {

                UUID messageId = UUIDGen.getTimeUUID();

                final String data = "my test data";
                final DatabaseQueueMessageBody messageBody = new DatabaseQueueMessageBody(
                    DataType.serializeValue( data, ProtocolVersion.NEWEST_SUPPORTED ), "text/plain" );
                serialization.writeMessageData( messageId, messageBody );

                xferLogSerialization.recordTransferLog(
                    queueName, actorSystemFig.getRegionLocal(), region, messageId );

                distributedQueueService.sendMessageToRegion(
                    queueName, region, region, messageId, null, null );
            }

            DatabaseQueueMessage.Type type = DatabaseQueueMessage.Type.DEFAULT;

            int maxRetries = 10;
            int retries = 0;
            long count = 0;
            while (retries++ < maxRetries) {
                distributedQueueService.refresh();
                count = queueMessageManager.getQueueDepth(  queueName, type );
                if ( count == 100 ) {
                    break;
                }
                Thread.sleep( 1000 );
            }

            Assert.assertEquals( 100, count );

            Assert.assertEquals( 25, distributedQueueService.getNextMessages( queueName, 25 ).size() );
            Assert.assertEquals( 75, queueMessageManager.getQueueDepth(  queueName, type ) );

            Assert.assertEquals( 25, distributedQueueService.getNextMessages( queueName, 25 ).size() );
            Assert.assertEquals( 50, queueMessageManager.getQueueDepth(  queueName, type ) );

            Assert.assertEquals( 25, distributedQueueService.getNextMessages( queueName, 25 ).size() );
            Assert.assertEquals( 25, queueMessageManager.getQueueDepth(  queueName, type ) );

            Assert.assertEquals( 25, distributedQueueService.getNextMessages( queueName, 25 ).size() );
            Assert.assertEquals( 0,  queueMessageManager.getQueueDepth(  queueName, type ) );

            distributedQueueService.shutdown();

        } finally {
            queueManager.deleteQueue( queueName );
        }
    }


    @Test
    public void testQueueMessageCounter() throws InterruptedException {

        Injector injector = getInjector();

        ActorSystemFig actorSystemFig = injector.getInstance( ActorSystemFig.class );
        String region = actorSystemFig.getRegionLocal();

        App app = injector.getInstance( App.class );
        app.start("localhost", getNextAkkaPort(), region);

        DistributedQueueService distributedQueueService = injector.getInstance( DistributedQueueService.class );
        QueueMessageSerialization serialization         = injector.getInstance( QueueMessageSerialization.class );
        TransferLogSerialization xferLogSerialization   = injector.getInstance( TransferLogSerialization.class );
        QueueMessageManager queueMessageManager         = injector.getInstance( QueueMessageManager.class );

        String queueName = "queue_testGetMultipleQueueMessages_" + UUID.randomUUID();
        QueueManager queueManager = injector.getInstance( QueueManager.class );

        try {

            queueManager.createQueue(
                new Queue( queueName, "test-type", region, region, 0L, 5, 10, null ) );

            UUID messageId = UUIDGen.getTimeUUID();

            final String data = "my test data";
            final DatabaseQueueMessageBody messageBody = new DatabaseQueueMessageBody(
                DataType.serializeValue( data, ProtocolVersion.NEWEST_SUPPORTED ), "text/plain" );
            serialization.writeMessageData( messageId, messageBody );

            xferLogSerialization.recordTransferLog(
                queueName, actorSystemFig.getRegionLocal(), region, messageId );

            distributedQueueService.sendMessageToRegion(
                queueName, region, region, messageId, null, null );

            DatabaseQueueMessage.Type type = DatabaseQueueMessage.Type.DEFAULT;

            Thread.sleep(5000);

            int maxRetries = 10;
            int retries = 0;
            long count = 0;
            while (retries++ < maxRetries) {
                distributedQueueService.refresh();
                count = queueMessageManager.getQueueDepth(  queueName, type );
                if ( count > 0 ) {
                    break;
                }
                Thread.sleep( 1000 );
            }

            Thread.sleep( 1000 );

            Assert.assertEquals( 1, queueMessageManager.getQueueDepth( queueName, type ) );

            distributedQueueService.shutdown();

        } finally {
            queueManager.deleteQueue( queueName );
        }
    }

}
