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

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.impl.QueueMessageSerializationImpl;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.apache.usergrid.persistence.qakka.serialization.queuemessages.impl.QueueMessageSerializationImpl.*;


public class MultiShardMessageIterator implements Iterator<DatabaseQueueMessage> {

    private static final Logger logger = LoggerFactory.getLogger( MultiShardMessageIterator.class );

    private final CassandraClient cassandraClient;


    private final int PAGE_SIZE = 100;
    private final String queueName;
    private final String region;
    private final DatabaseQueueMessage.Type messageType;
    private final Iterator<Shard> shardIterator;

    private Iterator<DatabaseQueueMessage> currentIterator;

    private Shard currentShard;
    private UUID nextStart;


    public MultiShardMessageIterator(
            final CassandraClient cassandraClient,
            final String queueName,
            final String region,
            final DatabaseQueueMessage.Type messageType,
            final Iterator<Shard> shardIterator,
            final UUID nextStart) {

        this.queueName = queueName;
        this.region = region;
        this.messageType = messageType;
        this.shardIterator = shardIterator;
        this.nextStart = nextStart;
        this.cassandraClient = cassandraClient;

        if (shardIterator == null) {
            throw new RuntimeException("shardIterator cannot be null");
        }

    }

    @Override
    public boolean hasNext() {

        try {

            if (shardIterator.hasNext() && currentIterator == null) {
                advance();
            }

            if (shardIterator.hasNext() && !currentIterator.hasNext()) {
                advance();
            }

            if (!shardIterator.hasNext() && (currentIterator == null || !currentIterator.hasNext())) {
                advance();
            }

            return currentIterator.hasNext();

        } catch ( NoSuchElementException e ) {
            return false;
        }
    }

    @Override
    public DatabaseQueueMessage next() {

        if ( !hasNext() ) {
            throw new NoSuchElementException( "No next message exists" );
        }

        return currentIterator.next();
    }

    private void advance(){

        if (currentShard == null){
            currentShard = shardIterator.next();
        }

        Clause queueNameClause = QueryBuilder.eq( COLUMN_QUEUE_NAME, queueName);
        Clause regionClause    = QueryBuilder.eq( COLUMN_REGION, region);
        Clause shardIdClause   = QueryBuilder.eq( COLUMN_SHARD_ID, currentShard.getShardId());

        // if we have a pointer from the shard and this is the first seek, init from the pointer's position
        if ( currentShard.getPointer() != null && nextStart == null ){
            nextStart = currentShard.getPointer();
        }

        Statement query;

        if ( nextStart == null) {

            query = QueryBuilder.select().all().from(QueueMessageSerializationImpl.getTableName(messageType))
                    .where(queueNameClause)
                    .and(regionClause)
                    .and(shardIdClause)
                    .limit(PAGE_SIZE);

        } else {

            Clause messageIdClause = QueryBuilder.gt( COLUMN_QUEUE_MESSAGE_ID, nextStart);
            query = QueryBuilder.select().all().from(QueueMessageSerializationImpl.getTableName(messageType))
                    .where(queueNameClause)
                    .and(regionClause)
                    .and(shardIdClause)
                    .and(messageIdClause)
                    .limit(PAGE_SIZE);
        }


        List<Row> rows = cassandraClient.getQueueMessageSession().execute(query).all();

        logger.trace("results {} from query {}", rows.size(), query.toString());

        if ( (rows == null || rows.size() == 0) && shardIterator.hasNext()) {

            currentShard = shardIterator.next();
            advance();

        } else {

            currentIterator = getIteratorFromRows(rows);

        }
    }


    private Iterator<DatabaseQueueMessage> getIteratorFromRows(List<Row> rows){

        List<DatabaseQueueMessage> messages = new ArrayList<>(rows.size());

        rows.forEach(row -> {

            final String queueName =    row.getString( COLUMN_QUEUE_NAME);
            final String region =       row.getString( COLUMN_REGION);
            final long shardId =        row.getLong(   COLUMN_SHARD_ID);
            final UUID queueMessageId = row.getUUID(   COLUMN_QUEUE_MESSAGE_ID);
            final UUID messageId =      row.getUUID(   COLUMN_MESSAGE_ID);
            final long queuedAt =       row.getLong(   COLUMN_QUEUED_AT);
            final long inflightAt =     row.getLong(   COLUMN_INFLIGHT_AT);

            messages.add(new DatabaseQueueMessage(
                    messageId, messageType, queueName, region, shardId, queuedAt, inflightAt, queueMessageId));

            //queueMessageId is internal to the messages_available and messages_inflight tables
            nextStart = queueMessageId;

        });

        return messages.iterator();

    }


    public Shard getCurrentShard() {
        return currentShard;
    }

}
