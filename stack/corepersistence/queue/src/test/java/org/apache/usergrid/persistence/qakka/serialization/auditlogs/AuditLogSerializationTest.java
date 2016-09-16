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

package org.apache.usergrid.persistence.qakka.serialization.auditlogs;

import org.apache.cassandra.utils.UUIDGen;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.qakka.core.CassandraClientImpl;
import org.apache.usergrid.persistence.qakka.serialization.Result;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLog;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLogSerialization;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;


public class AuditLogSerializationTest extends AbstractTest {

    @Test
    public void testRecordAuditLog() throws Exception {

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );

        AuditLogSerialization logSerialization = getInjector().getInstance( AuditLogSerialization.class );

        // record some audit logs for a message
        UUID messageId = UUIDGen.getTimeUUID();
        String queueName = "alst_queue_" + RandomStringUtils.randomAlphanumeric( 15 );
        String source = RandomStringUtils.randomAlphanumeric( 15 );
        String dest = RandomStringUtils.randomAlphanumeric( 15 );

        logSerialization.recordAuditLog( AuditLog.Action.GET, AuditLog.Status.SUCCESS,
            queueName, dest, messageId, UUIDGen.getTimeUUID() );

        // get audit logs for that message
        Result<AuditLog> result = logSerialization.getAuditLogs( messageId );
        Assert.assertEquals( 1, result.getEntities().size() );
    }

    @Test
    public void testGetAuditLogs() throws Exception {

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );

        AuditLogSerialization logSerialization = getInjector().getInstance( AuditLogSerialization.class );

        // record some audit logs for a message
        UUID messageId = UUIDGen.getTimeUUID();
        String queueName = "alst_queue_" + RandomStringUtils.randomAlphanumeric( 15 );
        String source = RandomStringUtils.randomAlphanumeric( 15 );
        String dest = RandomStringUtils.randomAlphanumeric( 15 );

        int numLogs = 10;

        UUID queueMessageId1 = UUIDGen.getTimeUUID();
        for ( int i=0; i<numLogs; i++ ) {
            logSerialization.recordAuditLog( AuditLog.Action.GET, AuditLog.Status.SUCCESS,
                    queueName, dest, messageId, queueMessageId1 );
            Thread.sleep(5);
        }

        UUID queueMessageId2 = UUIDGen.getTimeUUID();
        for ( int i=0; i<numLogs; i++ ) {
            logSerialization.recordAuditLog( AuditLog.Action.GET, AuditLog.Status.SUCCESS,
                    queueName, dest, messageId, queueMessageId2 );
            Thread.sleep(5);
        }

        UUID queueMessageId3 = UUIDGen.getTimeUUID();
        for ( int i=0; i<numLogs; i++ ) {
            logSerialization.recordAuditLog( AuditLog.Action.GET, AuditLog.Status.SUCCESS,
                    queueName, dest, messageId, queueMessageId3 );
            Thread.sleep(5);
        }

        // test that we have 3 X number of logs for the messageId
        Result<AuditLog> result = logSerialization.getAuditLogs( messageId );
        Assert.assertEquals( numLogs * 3, result.getEntities().size() );
    }
}
