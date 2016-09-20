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

package org.apache.usergrid.persistence.qakka.serialization.queues.impl;


import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.core.CassandraConfig;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.apache.usergrid.persistence.core.datastax.impl.TableDefinitionStringImpl;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.impl.QueueMessageSerializationImpl;
import org.apache.usergrid.persistence.qakka.serialization.queues.DatabaseQueue;
import org.apache.usergrid.persistence.qakka.serialization.queues.QueueSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class QueueSerializationImpl implements QueueSerialization {

    private static final Logger logger = LoggerFactory.getLogger( QueueMessageSerializationImpl.class );

    private final CassandraClient cassandraClient;
    private final CassandraConfig cassandraConfig;

    public final static String COLUMN_QUEUE_NAME = "queue_name";
    public final static String COLUMN_REGIONS = "regions";
    public final static String COLUMN_DEFAULT_DESTINATIONS = "default_destinations";
    public final static String COLUMN_DEFAULT_DELAY_MS = "default_delay_ms";
    public final static String COLUMN_RETRY_COUNT = "retry_count";
    public final static String COLUMN_HANDLING_TIMEOUT_SEC = "handling_timeout_sec";
    public final static String COLUMN_DEAD_LETTER_QUEUE = "dead_letter_queue";


    public final static String TABLE_QUEUES = "queues";

    static final String CQL =
        "CREATE TABLE IF NOT EXISTS queues ( " +
            "queue_name           text, " +
            "regions              text, " +
            "default_destinations text, " +
            "default_delay_ms     bigint, " +
            "retry_count          int, " +
            "handling_timeout_sec int, " +
            "dead_letter_queue    text, " +
            "PRIMARY KEY ((queue_name)) " +
            "); ";


    @Inject
    public QueueSerializationImpl( CassandraConfig cassandraConfig,  CassandraClient cassandraClient ) {
        this.cassandraConfig = cassandraConfig;
        this.cassandraClient = cassandraClient;
    }


    @Override
    public void writeQueue(DatabaseQueue queue) {

        Statement insert = QueryBuilder.insertInto(TABLE_QUEUES)
                .value(COLUMN_QUEUE_NAME, queue.getName())
                .value(COLUMN_REGIONS, queue.getRegions())
                .value(COLUMN_DEFAULT_DESTINATIONS, queue.getDefaultDestinations())
                .value(COLUMN_DEFAULT_DELAY_MS, queue.getDefaultDelayMs())
                .value(COLUMN_RETRY_COUNT, queue.getRetryCount())
                .value(COLUMN_HANDLING_TIMEOUT_SEC, queue.getHandlingTimeoutSec())
                .value(COLUMN_DEAD_LETTER_QUEUE, queue.getDeadLetterQueue());


        cassandraClient.getApplicationSession().execute(insert);

    }

    @Override
    public DatabaseQueue getQueue(String name) {

        Clause queueNameClause = QueryBuilder.eq(COLUMN_QUEUE_NAME, name);

        Statement query = QueryBuilder.select().all().from(TABLE_QUEUES)
                .where(queueNameClause);

        Row row = cassandraClient.getApplicationSession().execute(query).one();

        if(row == null){
            return null;
        }

        final String queueName = row.getString(COLUMN_QUEUE_NAME);
        final String regions = row.getString(COLUMN_REGIONS);
        final String defaultDestinations = row.getString(COLUMN_DEFAULT_DESTINATIONS);
        final long defaultDelayMs = row.getLong(COLUMN_DEFAULT_DELAY_MS);
        final int retryCount = row.getInt(COLUMN_RETRY_COUNT);
        final int handlingTimeoutSec = row.getInt(COLUMN_HANDLING_TIMEOUT_SEC);
        final String deadLetterQueue = row.getString(COLUMN_DEAD_LETTER_QUEUE);

        return new DatabaseQueue( queueName, regions, defaultDestinations, defaultDelayMs, retryCount,
                handlingTimeoutSec, deadLetterQueue);

    }

    @Override
    public void deleteQueue(String name) {

        Clause queueNameClause = QueryBuilder.eq(COLUMN_QUEUE_NAME, name);

        Statement delete = QueryBuilder.delete().from(TABLE_QUEUES)
                .where(queueNameClause);

        cassandraClient.getApplicationSession().execute(delete);
    }

    @Override
    public List<String> getListOfQueues() {

        Statement select = QueryBuilder.select().all().from( TABLE_QUEUES );
        ResultSet rs = cassandraClient.getApplicationSession().execute( select );

        return rs.all().stream()
                .map( row -> row.getString( COLUMN_QUEUE_NAME ))
                .collect( Collectors.toList() );
    }


    @Override
    public Collection<MultiTenantColumnFamilyDefinition> getColumnFamilies() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<TableDefinition> getTables() {
        return Collections.singletonList(
            new TableDefinitionStringImpl( cassandraConfig.getApplicationKeyspace(), "queues", CQL ) );
    }

}
