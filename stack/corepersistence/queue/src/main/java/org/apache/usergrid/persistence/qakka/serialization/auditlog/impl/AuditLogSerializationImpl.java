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

package org.apache.usergrid.persistence.qakka.serialization.auditlog.impl;

import com.datastax.driver.core.PagingState;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.apache.usergrid.persistence.core.datastax.impl.TableDefinitionStringImpl;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.serialization.Result;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLog;
import org.apache.usergrid.persistence.qakka.serialization.auditlog.AuditLogSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


public class AuditLogSerializationImpl implements AuditLogSerialization {

    private static final Logger logger = LoggerFactory.getLogger( AuditLogSerializationImpl.class );

    private final CassandraClient cassandraClient;

    public final static String TABLE_AUDIT_LOG   = "audit_log";

    public final static String COLUMN_ACTION           = "action";
    public final static String COLUMN_STATUS           = "status";
    public final static String COLUMN_QUEUE_NAME       = "queue_name";
    public final static String COLUMN_REGION           = "region";
    public final static String COLUMN_MESSAGE_ID       = "message_id";
    public final static String COLUMN_QUEUE_MESSAGE_ID = "queue_message_id";
    public final static String COLUMN_TRANSFER_TIME    = "transfer_time";


    // design note: want to be able to query this by message_id, so we can do "garbage collection"
    // of message data items that have been processed in all target regions

    static final String CQL =
        "CREATE TABLE IF NOT EXISTS audit_log ( " +
                "action           text, " +
                "status           text, " +
                "queue_name       text, " +
                "region           text, " +
                "message_id       timeuuid, " +
                "queue_message_id timeuuid, " +
                "transfer_time    bigint, " +
                "PRIMARY KEY (message_id, transfer_time) " +
        ") WITH CLUSTERING ORDER BY (transfer_time ASC); ";


    @Inject
    public AuditLogSerializationImpl( CassandraClient cassandraClient ) {
        this.cassandraClient = cassandraClient;
    }


    @Override
    public void recordAuditLog(
            AuditLog.Action action,
            AuditLog.Status status,
            String queueName,
            String region,
            UUID messageId,
            UUID queueMessageId ) {

        Statement insert = QueryBuilder.insertInto(TABLE_AUDIT_LOG)
                .value(COLUMN_ACTION, action.toString() )
                .value(COLUMN_STATUS, status.toString() )
                .value(COLUMN_QUEUE_NAME, queueName )
                .value(COLUMN_REGION, region )
                .value(COLUMN_MESSAGE_ID, messageId )
                .value(COLUMN_QUEUE_MESSAGE_ID, queueMessageId )
                .value(COLUMN_TRANSFER_TIME, System.currentTimeMillis() );
        cassandraClient.getSession().execute(insert);
    }


    @Override
    public Result<AuditLog> getAuditLogs( UUID messageId ) {

        Statement query = QueryBuilder.select().all().from(TABLE_AUDIT_LOG)
            .where( QueryBuilder.eq( COLUMN_MESSAGE_ID, messageId ) );

        ResultSet rs = cassandraClient.getSession().execute( query );

        final List<AuditLog> auditLogs = rs.all().stream().map( row ->
            new AuditLog(
                AuditLog.Action.valueOf( row.getString( COLUMN_ACTION )),
                AuditLog.Status.valueOf( row.getString( COLUMN_STATUS )),
                row.getString( COLUMN_QUEUE_NAME ),
                row.getString( COLUMN_REGION ),
                row.getUUID( COLUMN_MESSAGE_ID ),
                row.getUUID( COLUMN_QUEUE_MESSAGE_ID ),
                row.getLong( COLUMN_TRANSFER_TIME ) )
        ).collect( Collectors.toList() );

        return new Result<AuditLog>() {

            @Override
            public PagingState getPagingState() {
                return null; // no paging
            }

            @Override
            public List<AuditLog> getEntities() {
                return auditLogs;
            }
        };

    }


    @Override
    public Collection<MultiTenantColumnFamilyDefinition> getColumnFamilies() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<TableDefinition> getTables() {
        return Collections.singletonList( new TableDefinitionStringImpl( TABLE_AUDIT_LOG, CQL ) );
    }
}
