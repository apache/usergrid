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

package org.apache.usergrid.persistence.qakka.serialization.transferlog.impl;

import com.datastax.driver.core.PagingState;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.apache.usergrid.persistence.core.datastax.impl.TableDefinitionStringImpl;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaException;
import org.apache.usergrid.persistence.qakka.serialization.Result;
import org.apache.usergrid.persistence.qakka.serialization.transferlog.TransferLog;
import org.apache.usergrid.persistence.qakka.serialization.transferlog.TransferLogSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class TransferLogSerializationImpl implements TransferLogSerialization {

    private static final Logger logger = LoggerFactory.getLogger( TransferLogSerializationImpl.class );

    private final CassandraClient cassandraClient;

    public final static String TABLE_TRANSFER_LOG   = "transfer_log";

    public final static String COLUMN_QUEUE_NAME    = "queue_name";
    public final static String COLUMN_SOURCE_REGION = "source_region";
    public final static String COLUMN_DEST_REGION   = "dest_region";
    public final static String COLUMN_MESSAGE_ID    = "message_id";
    public final static String COLUMN_TRANSFER_TIME = "transfer_time";

    static final String CQL =
        "CREATE TABLE IF NOT EXISTS transfer_log ( " +
            "queue_name    text, " +
            "source_region text, " +
            "dest_region   text, " +
            "message_id    timeuuid, " +
            "transfer_time bigint, " +
            "PRIMARY KEY ((queue_name, dest_region, message_id)) " +
            ");  ";


    @Inject
    public TransferLogSerializationImpl( CassandraClient cassandraClient ) {
        this.cassandraClient = cassandraClient;
    }


    @Override
    public void recordTransferLog(
            String queueName, String source, String dest, UUID messageId) {

        Statement insert = QueryBuilder.insertInto(TABLE_TRANSFER_LOG)
                .value(COLUMN_QUEUE_NAME, queueName )
                .value(COLUMN_SOURCE_REGION, source )
                .value(COLUMN_DEST_REGION, dest )
                .value(COLUMN_MESSAGE_ID, messageId )
                .value(COLUMN_TRANSFER_TIME, System.currentTimeMillis() );
        cassandraClient.getSession().execute(insert);
    }


    @Override
    public void removeTransferLog(
            String queueName, String source, String dest, UUID messageId ) throws QakkaException {

        Statement query = QueryBuilder.select().all().from(TABLE_TRANSFER_LOG)
            .where(   QueryBuilder.eq( COLUMN_QUEUE_NAME, queueName ))
                .and( QueryBuilder.eq( COLUMN_DEST_REGION, dest ))
                .and( QueryBuilder.eq( COLUMN_MESSAGE_ID, messageId ));
        ResultSet rs = cassandraClient.getSession().execute( query );

        if ( rs.getAvailableWithoutFetching() == 0 ) {
            StringBuilder sb = new StringBuilder();
            sb.append( "Transfer log entry not found for queueName=" ).append( queueName );
            sb.append( " source=" ).append( source );
            sb.append( " dest=" ).append( dest );
            sb.append( " messageId=" ).append( messageId );
            throw new QakkaException( sb.toString() );
        }

        Statement deleteQuery = QueryBuilder.delete().from(TABLE_TRANSFER_LOG)
                .where(   QueryBuilder.eq( COLUMN_QUEUE_NAME, queueName ))
                    .and( QueryBuilder.eq( COLUMN_DEST_REGION, dest ))
                .and( QueryBuilder.eq( COLUMN_MESSAGE_ID, messageId ));
        cassandraClient.getSession().execute( deleteQuery );
    }


    @Override
    public Result<TransferLog> getAllTransferLogs(PagingState pagingState, int fetchSize ) {

        Statement query = QueryBuilder.select().all().from(TABLE_TRANSFER_LOG);

        query.setFetchSize( fetchSize );
        if ( pagingState != null ) {
            query.setPagingState( pagingState );
        }

        ResultSet rs = cassandraClient.getSession().execute( query );
        final PagingState newPagingState = rs.getExecutionInfo().getPagingState();

        final List<TransferLog> transferLogs = new ArrayList<>();
        int numReturned = rs.getAvailableWithoutFetching();
        for ( int i=0; i<numReturned; i++ ) {
            Row row = rs.one();
            TransferLog tlog = new TransferLog(
                    row.getString( COLUMN_QUEUE_NAME ),
                    row.getString( COLUMN_SOURCE_REGION ),
                    row.getString( COLUMN_DEST_REGION ),
                    row.getUUID( COLUMN_MESSAGE_ID ),
                    row.getLong( COLUMN_TRANSFER_TIME ));
            transferLogs.add( tlog );
        }

        return new Result<TransferLog>() {

            @Override
            public PagingState getPagingState() {
                return newPagingState;
            }

            @Override
            public List<TransferLog> getEntities() {
                return transferLogs;
            }
        };
    }

    @Override
    public Collection<MultiTenantColumnFamilyDefinition> getColumnFamilies() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<TableDefinition> getTables() {
        return Collections.singletonList( new TableDefinitionStringImpl( TABLE_TRANSFER_LOG, CQL ) );
    }


}
