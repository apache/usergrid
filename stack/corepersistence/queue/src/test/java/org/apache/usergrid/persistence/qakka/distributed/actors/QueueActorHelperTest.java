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

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.App;
import org.apache.usergrid.persistence.qakka.core.*;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.qakka.serialization.Result;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLog;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLogSerialization;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.apache.usergrid.persistence.queue.TestModule;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;


@NotThreadSafe
public class QueueActorHelperTest extends AbstractTest {


    @Override
    protected Injector getInjector() {
        return Guice.createInjector( new TestModule() );
    }

    @Test
    public void loadDatabaseQueueMessage() throws Exception {

        Injector injector = getInjector();
        CassandraClient cassandraClient = injector.getInstance( CassandraClientImpl.class );

        injector.getInstance( App.class ); // init the INJECTOR

        ActorSystemFig actorSystemFig = injector.getInstance( ActorSystemFig.class );
        QueueMessageSerialization qms = injector.getInstance( QueueMessageSerialization.class );
        QueueManager queueManager     = injector.getInstance( QueueManager.class );

        String region = actorSystemFig.getRegionLocal();
        App app = injector.getInstance( App.class );
        app.start( "localhost", getNextAkkaPort(), region );

        String queueName = "qat_queue_" + RandomStringUtils.randomAlphanumeric( 10 );
        queueManager.createQueue( new Queue( queueName ) );

        UUID queueMessageId = QakkaUtils.getTimeUuid();

        // write message

        DatabaseQueueMessage message = new DatabaseQueueMessage(
                QakkaUtils.getTimeUuid(),
                DatabaseQueueMessage.Type.DEFAULT,
                queueName,
                actorSystemFig.getRegionLocal(),
                null,
                System.currentTimeMillis(),
                null,
                queueMessageId);
        qms.writeMessage( message );

        // load message

        QueueActorHelper helper = injector.getInstance( QueueActorHelper.class );
        DatabaseQueueMessage queueMessage = helper.loadDatabaseQueueMessage(
                queueName, message.getQueueMessageId(), message.getType() );

        Assert.assertNotNull( queueMessage );

        DistributedQueueService distributedQueueService = injector.getInstance( DistributedQueueService.class );
        distributedQueueService.shutdown();
    }


    @Test
    public void loadDatabaseQueueMessageNotFound() throws Exception {

        Injector injector = getInjector();
        CassandraClient cassandraClient = injector.getInstance( CassandraClientImpl.class );


        injector.getInstance( App.class ); // init the INJECTOR
        QueueManager queueManager = injector.getInstance( QueueManager.class );

        ActorSystemFig actorSystemFig = injector.getInstance( ActorSystemFig.class );
        String region = actorSystemFig.getRegionLocal();
        App app = injector.getInstance( App.class );
        app.start( "localhost", getNextAkkaPort(), region );

        String queueName = "qat_queue_" + RandomStringUtils.randomAlphanumeric( 10 );
        queueManager.createQueue( new Queue( queueName ) );

        // don't write any message

        // load message

        QueueActorHelper helper = injector.getInstance( QueueActorHelper.class );
        DatabaseQueueMessage queueMessage = helper.loadDatabaseQueueMessage(
                queueName, QakkaUtils.getTimeUuid(), DatabaseQueueMessage.Type.DEFAULT );

        Assert.assertNull( queueMessage );

        DistributedQueueService distributedQueueService = injector.getInstance( DistributedQueueService.class );
        distributedQueueService.shutdown();
    }


    @Test
    public void putInflight() throws Exception {

        Injector injector = getInjector();
        CassandraClient cassandraClient = injector.getInstance( CassandraClientImpl.class );


        injector.getInstance( App.class ); // init the INJECTOR

        ActorSystemFig actorSystemFig = injector.getInstance( ActorSystemFig.class );
        QueueMessageSerialization qms = injector.getInstance( QueueMessageSerialization.class );
        QueueManager queueManager     = injector.getInstance( QueueManager.class );

        String region = actorSystemFig.getRegionLocal();
        App app = injector.getInstance( App.class );
        app.start( "localhost", getNextAkkaPort(), region );

        // write message to messages_available table

        UUID queueMessageId = QakkaUtils.getTimeUuid();

        String queueName = "qat_queue_" + RandomStringUtils.randomAlphanumeric( 10 );
        queueManager.createQueue( new Queue( queueName ) );

        DatabaseQueueMessage message = new DatabaseQueueMessage(
                QakkaUtils.getTimeUuid(),
                DatabaseQueueMessage.Type.DEFAULT,
                queueName,
                actorSystemFig.getRegionLocal(),
                null,
                System.currentTimeMillis(),
                null,
                queueMessageId);
        qms.writeMessage( message );

        // put message inflight

        QueueActorHelper helper = injector.getInstance( QueueActorHelper.class );
        helper.putInflight( queueName, message );

        // message must be gone from messages_available table

        Assert.assertNull( qms.loadMessage(
                queueName,
                actorSystemFig.getRegionLocal(),
                null,
                DatabaseQueueMessage.Type.DEFAULT,
                message.getQueueMessageId() ) );

        // message must be present in messages_inflight table

        Assert.assertNotNull( qms.loadMessage(
                queueName,
                actorSystemFig.getRegionLocal(),
                null,
                DatabaseQueueMessage.Type.INFLIGHT,
                message.getQueueMessageId() ) );

        // there must be an audit log record of the successful get operation

        AuditLogSerialization auditLogSerialization = injector.getInstance( AuditLogSerialization.class );
        Result<AuditLog> auditLogs = auditLogSerialization.getAuditLogs( message.getMessageId() );
        Assert.assertEquals( 1, auditLogs.getEntities().size() );
        Assert.assertEquals( AuditLog.Status.SUCCESS, auditLogs.getEntities().get(0).getStatus()  );
        Assert.assertEquals( AuditLog.Action.GET,     auditLogs.getEntities().get(0).getAction()  );

        DistributedQueueService distributedQueueService = injector.getInstance( DistributedQueueService.class );
        distributedQueueService.shutdown();
    }


    @Test
    public void ackQueueMessage() throws Exception {

        Injector injector = getInjector();
        CassandraClient cassandraClient = injector.getInstance( CassandraClientImpl.class );


        injector.getInstance( App.class ); // init the INJECTOR

        ActorSystemFig actorSystemFig = injector.getInstance( ActorSystemFig.class );
        QueueMessageSerialization qms = injector.getInstance( QueueMessageSerialization.class );
        QueueManager queueManager     = injector.getInstance( QueueManager.class );

        String region = actorSystemFig.getRegionLocal();
        App app = injector.getInstance( App.class );
        app.start( "localhost", getNextAkkaPort(), region );

        UUID queueMessageId = QakkaUtils.getTimeUuid();

        String queueName = "qat_queue_" + RandomStringUtils.randomAlphanumeric( 10 );
        queueManager.createQueue( new Queue( queueName ) );

        // write message to messages_inflight table

        DatabaseQueueMessage message = new DatabaseQueueMessage(
                QakkaUtils.getTimeUuid(),
                DatabaseQueueMessage.Type.INFLIGHT,
                queueName,
                actorSystemFig.getRegionLocal(),
                null,
                System.currentTimeMillis(),
                null,
                queueMessageId);
        qms.writeMessage( message );

        // ack message

        QueueActorHelper helper = injector.getInstance( QueueActorHelper.class );
        helper.ackQueueMessage( queueName, message.getQueueMessageId() );

        // message must be gone from messages_available table

        Assert.assertNull( helper.loadDatabaseQueueMessage(
                queueName, QakkaUtils.getTimeUuid(), DatabaseQueueMessage.Type.INFLIGHT ));

        // message must be gone from messages_inflight table

        Assert.assertNull( helper.loadDatabaseQueueMessage(
                queueName, QakkaUtils.getTimeUuid(), DatabaseQueueMessage.Type.DEFAULT ));

        // there should be an audit log record of the successful ack operation

        AuditLogSerialization auditLogSerialization = injector.getInstance( AuditLogSerialization.class );
        Result<AuditLog> auditLogs = auditLogSerialization.getAuditLogs( message.getMessageId() );
        Assert.assertEquals( 1, auditLogs.getEntities().size() );
        Assert.assertEquals( AuditLog.Status.SUCCESS, auditLogs.getEntities().get(0).getStatus()  );
        Assert.assertEquals( AuditLog.Action.ACK,     auditLogs.getEntities().get(0).getAction()  );

        DistributedQueueService distributedQueueService = injector.getInstance( DistributedQueueService.class );
        distributedQueueService.shutdown();
    }


    @Test
    public void ackQueueMessageNotFound() throws Exception {

        Injector injector = getInjector();
        CassandraClient cassandraClient = injector.getInstance( CassandraClientImpl.class );


        injector.getInstance( App.class ); // init the INJECTOR
        QueueManager queueManager     = injector.getInstance( QueueManager.class );
        ActorSystemFig actorSystemFig = injector.getInstance( ActorSystemFig.class );

        String region = actorSystemFig.getRegionLocal();
        App app = injector.getInstance( App.class );
        app.start( "localhost", getNextAkkaPort(), region );

        String queueName = "qat_queue_" + RandomStringUtils.randomAlphanumeric( 10 );
        queueManager.createQueue( new Queue( queueName ) );

        // don't write message, just make up some bogus IDs

        UUID queueMessageId = QakkaUtils.getTimeUuid();

        // ack message must fail

        QueueActorHelper helper = injector.getInstance( QueueActorHelper.class );
        Assert.assertEquals( DistributedQueueService.Status.BAD_REQUEST,
            helper.ackQueueMessage( queueName, queueMessageId ));

        DistributedQueueService distributedQueueService = injector.getInstance( DistributedQueueService.class );
        distributedQueueService.shutdown();
    }
}
