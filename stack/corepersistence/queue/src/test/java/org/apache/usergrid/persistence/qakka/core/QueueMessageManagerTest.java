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

package org.apache.usergrid.persistence.qakka.core;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.App;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.impl.InMemoryQueue;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaRuntimeException;
import org.apache.usergrid.persistence.qakka.serialization.Result;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLog;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLogSerialization;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessageBody;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.apache.usergrid.persistence.qakka.serialization.transferlog.TransferLog;
import org.apache.usergrid.persistence.qakka.serialization.transferlog.TransferLogSerialization;
import org.apache.usergrid.persistence.queue.TestModule;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@NotThreadSafe
public class QueueMessageManagerTest extends AbstractTest {
    private static final Logger logger = LoggerFactory.getLogger( QueueMessageManagerTest.class );

    // TODO: test that multiple threads pulling from same queue will never pop same item

    @Override
    protected Injector getInjector() {
        return Guice.createInjector( new TestModule() );
    }


    @Test
    public void testBasicOperation() throws Exception {

        String queueName = "qmmt_queue_" + RandomStringUtils.randomAlphanumeric(15);

        Injector injector = getInjector();

        DistributedQueueService distributedQueueService = injector.getInstance( DistributedQueueService.class );
        ActorSystemFig actorSystemFig = injector.getInstance( ActorSystemFig.class );

        String region = actorSystemFig.getRegionLocal();
        App app = injector.getInstance( App.class );
        app.start( "localhost", getNextAkkaPort(), region );

        // create queue and send one message to it
        QueueManager queueManager = injector.getInstance( QueueManager.class );

        try {

            QueueMessageManager qmm = injector.getInstance( QueueMessageManager.class );
            queueManager.createQueue( new Queue( queueName, "test-type", region, region, 0L, 5, 10, null ) );
            String jsonData = "{}";
            qmm.sendMessages( queueName, Collections.singletonList( region ), null, null,
                "application/json", DataType.serializeValue( jsonData, ProtocolVersion.NEWEST_SUPPORTED ) );

            distributedQueueService.refresh();
            Thread.sleep( 1000 );

            // get message from the queue
            List<QueueMessage> messages = qmm.getNextMessages( queueName, 1 );
            Assert.assertEquals( 1, messages.size() );
            QueueMessage message = messages.get( 0 );

            // test that queue message data is present and correct
            QueueMessageSerialization qms = injector.getInstance( QueueMessageSerialization.class );
            DatabaseQueueMessageBody data = qms.loadMessageData( message.getMessageId() );
            Assert.assertNotNull( data );
            Assert.assertEquals( "application/json", data.getContentType() );
            String jsonDataReturned = new String( data.getBlob().array(), Charset.forName( "UTF-8" ) );
            Assert.assertEquals( jsonData, jsonDataReturned );

            // test that transfer log is empty for our queue
            TransferLogSerialization tlogs = injector.getInstance( TransferLogSerialization.class );
            Result<TransferLog> all = tlogs.getAllTransferLogs( null, 1000 );
            List<TransferLog> logs = all.getEntities().stream()
                .filter( log -> log.getQueueName().equals( queueName ) ).collect( Collectors.toList() );
            Assert.assertTrue( logs.isEmpty() );

            // ack the message
            qmm.ackMessage( queueName, message.getQueueMessageId() );

            // test that message is no longer stored in non-replicated keyspace

            Assert.assertNull( qms.loadMessage( queueName, region, null,
                DatabaseQueueMessage.Type.DEFAULT, message.getQueueMessageId() ) );

            Assert.assertNull( qms.loadMessage( queueName, region, null,
                DatabaseQueueMessage.Type.INFLIGHT, message.getQueueMessageId() ) );

            // test that audit log entry was written
            AuditLogSerialization auditLogSerialization = injector.getInstance( AuditLogSerialization.class );
            Result<AuditLog> auditLogs = auditLogSerialization.getAuditLogs( message.getMessageId() );
            Assert.assertEquals( 3, auditLogs.getEntities().size() );

            distributedQueueService.shutdown();

        } finally {
            queueManager.deleteQueue( queueName );
        }
    }


    @Test
    public void testQueueMessageTimeouts() throws Exception {

        Injector injector = getInjector();

        DistributedQueueService distributedQueueService = injector.getInstance( DistributedQueueService.class );
        QakkaFig qakkaFig             = injector.getInstance( QakkaFig.class );
        ActorSystemFig actorSystemFig = injector.getInstance( ActorSystemFig.class );
        InMemoryQueue inMemoryQueue   = injector.getInstance( InMemoryQueue.class );

        String region = actorSystemFig.getRegionLocal();
        App app = injector.getInstance( App.class );
        app.start( "localhost", getNextAkkaPort(), region );

        // create some number of queue messages

        QueueManager queueManager = injector.getInstance( QueueManager.class );

        String queueName = "queue_testQueueMessageTimeouts_" + RandomStringUtils.randomAlphanumeric( 15 );

        try {

            QueueMessageManager qmm = injector.getInstance( QueueMessageManager.class );
            queueManager.createQueue( new Queue( queueName, "test-type", region, region, 0L, 5, 10, null ) );

            int numMessages = 40;

            for (int i = 0; i < numMessages; i++) {
                qmm.sendMessages(
                    queueName,
                    Collections.singletonList( region ),
                    null, // delay
                    null, // expiration
                    "application/json",
                    DataType.serializeValue( "{}", ProtocolVersion.NEWEST_SUPPORTED ) );
            }

            int maxRetries = 15;
            int retries = 0;
            while (retries++ < maxRetries) {
                distributedQueueService.refresh();
                if (qmm.getQueueDepth( queueName ) == 40) {
                    break;
                }
                Thread.sleep( 500 );
            }

            Assert.assertEquals( numMessages, qmm.getQueueDepth( queueName ) );

            // get all messages from queue

            List<QueueMessage> messages = qmm.getNextMessages( queueName, numMessages );
            Assert.assertEquals( numMessages, messages.size() );

            // ack half of the messages

            List<QueueMessage> remove = new ArrayList<>();

            for (int i = 0; i < numMessages / 2; i++) {
                QueueMessage queueMessage = messages.get( i );
                qmm.ackMessage( queueName, queueMessage.getQueueMessageId() );
                remove.add( queueMessage );
            }

            for (QueueMessage message : remove) {
                messages.remove( message );
            }

            // wait for twice timeout period

            Thread.sleep( 2 * qakkaFig.getQueueTimeoutSeconds() * 1000 );

            distributedQueueService.processTimeouts();

            Thread.sleep( qakkaFig.getQueueTimeoutSeconds() * 1000 );

            // attempt to ack other half of messages

            for (QueueMessage message : messages) {
                try {
                    qmm.ackMessage( queueName, message.getQueueMessageId() );
                    Assert.fail( "Message should have timed out by now" );

                } catch (QakkaRuntimeException expected) {
                    // keep on going...
                }
            }

            distributedQueueService.shutdown();

        } finally {
            queueManager.deleteQueue( queueName );
        }
    }


    @Test
    public void testGetWithMissingData() throws InterruptedException {

        Injector injector = getInjector();

        CassandraClient cassandraClient = injector.getInstance( CassandraClientImpl.class );

        injector.getInstance( App.class ); // init the INJECTOR

        ActorSystemFig actorSystemFig = injector.getInstance( ActorSystemFig.class );
        DistributedQueueService qas         = injector.getInstance( DistributedQueueService.class );
        QueueManager qm               = injector.getInstance( QueueManager.class );
        QueueMessageManager qmm       = injector.getInstance( QueueMessageManager.class );
        QueueMessageSerialization qms = injector.getInstance( QueueMessageSerialization.class );

        String region = actorSystemFig.getRegionLocal();
        App app = injector.getInstance( App.class );
        app.start( "localhost", getNextAkkaPort(), region );

        // create queue messages, every other one with missing data

        int numMessages = 100;
        String queueName = "qmmt_queue_" + RandomStringUtils.randomAlphanumeric( 10 );
        qm.createQueue( new Queue( queueName, "test-type", region, region, 0L, 5, 10, null ));

        for ( int i=0; i<numMessages; i++ ) {

            final UUID messageId = QakkaUtils.getTimeUuid();

            if ( i % 2 == 0 ) { // every other it
                final String data = "my test data";
                final DatabaseQueueMessageBody messageBody = new DatabaseQueueMessageBody(
                        DataType.serializeValue( data, ProtocolVersion.NEWEST_SUPPORTED ), "text/plain" );
                qms.writeMessageData( messageId, messageBody );
            }

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
            qms.writeMessage( message );
        }

        qas.refresh();
        Thread.sleep(1000);

        int count = 0;
        while ( count < numMessages / 2 ) {
            List<QueueMessage> messages = qmm.getNextMessages( queueName, 1 );
            Assert.assertTrue( !messages.isEmpty() );
            count += messages.size();
            logger.debug("Got {} messages", ++count);
        }

        DistributedQueueService distributedQueueService = injector.getInstance( DistributedQueueService.class );
        distributedQueueService.shutdown();
    }

}
