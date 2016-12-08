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

package org.apache.usergrid.persistence.qakka.serialization.queuemessages.impl;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.core.CassandraConfig;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.apache.usergrid.persistence.core.datastax.impl.TableDefinitionStringImpl;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.core.QakkaUtils;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessageBody;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.MessageCounterSerialization;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.QueueMessageSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardCounterSerialization;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardIterator;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.java8.FuturesConvertersImpl;

import java.util.*;


public class QueueMessageSerializationImpl implements QueueMessageSerialization {
    private static final Logger logger = LoggerFactory.getLogger( QueueMessageSerializationImpl.class );

    private final CassandraClient cassandraClient;
    private final CassandraConfig cassandraConfig;

    private final int maxTtl;

    private final ActorSystemFig            actorSystemFig;
    private final ShardStrategy             shardStrategy;
    private final ShardCounterSerialization shardCounterSerialization;
    private final MessageCounterSerialization messageCounterSerialization;

    public final static String COLUMN_QUEUE_NAME       = "queue_name";
    public final static String COLUMN_REGION           = "region";
    public final static String COLUMN_SHARD_ID         = "shard_id";
    public final static String COLUMN_QUEUED_AT        = "queued_at";
    public final static String COLUMN_INFLIGHT_AT      = "inflight_at";
    public final static String COLUMN_QUEUE_MESSAGE_ID = "queue_message_id";
    public final static String COLUMN_MESSAGE_ID       = "message_id";
    public final static String COLUMN_CONTENT_TYPE     = "content_type";
    public final static String COLUMN_MESSAGE_DATA     = "data";

    public final static String TABLE_MESSAGES_AVAILABLE = "messages_available";

    public final static String TABLE_MESSAGES_INFLIGHT = "messages_inflight";

    public final static String TABLE_MESSAGE_DATA = "message_data";

    static final String MESSAGES_AVAILABLE =
        "CREATE TABLE IF NOT EXISTS messages_available ( " +
                "queue_name       text, " +
                "region           text, " +
                "shard_id         bigint, " +
                "queue_message_id timeuuid, " +
                "message_id       uuid, " +
                "queued_at        bigint, " +
                "inflight_at      bigint, " +
                "PRIMARY KEY ((queue_name, region, shard_id), queue_message_id ) " +
                ") WITH CLUSTERING ORDER BY (queue_message_id ASC) AND " +
                    "gc_grace_seconds = 60 AND " +
                    "compaction = {'class': " + "'LeveledCompactionStrategy', " +
                        "'sstable_size_in_mb': 5, " +
                        "'tombstone_compaction_interval': 60, " +
                        "'tombstone_threshold': 0.05, " +
                        "'unchecked_tombstone_compaction': true" +
                    "};";

    static final String MESSAGES_INFLIGHT =
        "CREATE TABLE IF NOT EXISTS messages_inflight ( " +
                "queue_name       text, " +
                "region           text, " +
                "shard_id         bigint, " +
                "queue_message_id timeuuid, " +
                "message_id       uuid, " +
                "queued_at        bigint, " +
                "inflight_at      bigint, " +
                "PRIMARY KEY ((queue_name, region, shard_id), queue_message_id ) " +
                ") WITH CLUSTERING ORDER BY (queue_message_id ASC) AND " +
                    "gc_grace_seconds = 60 AND " +
                        "compaction = {'class': " + "'LeveledCompactionStrategy', " +
                        "'sstable_size_in_mb': 5, " +
                        "'tombstone_compaction_interval': 60, " +
                        "'tombstone_threshold': 0.05, " +
                        "'unchecked_tombstone_compaction': true" +
                    "};";

    static final String MESSAGE_DATA =
        "CREATE TABLE IF NOT EXISTS message_data ( " +
                "message_id uuid, " +
                "data blob, " +
                "content_type text, " +
                "PRIMARY KEY ((message_id)) " +
                "); ";

    @Inject
    public QueueMessageSerializationImpl(
            CassandraConfig           cassandraConfig,
            ActorSystemFig            actorSystemFig,
            ShardStrategy             shardStrategy,
            ShardCounterSerialization shardCounterSerialization,
            MessageCounterSerialization messageCounterSerialization,
            CassandraClient           cassandraClient,
            QakkaFig                  qakkaFig
        ) {
        this.cassandraConfig             = cassandraConfig;
        this.actorSystemFig              = actorSystemFig;
        this.shardStrategy               = shardStrategy;
        this.shardCounterSerialization   = shardCounterSerialization;
        this.messageCounterSerialization = messageCounterSerialization;
        this.cassandraClient             = cassandraClient;

        this.maxTtl = qakkaFig.getMaxTtlSeconds();
    }


    @Override
    public UUID writeMessage(final DatabaseQueueMessage message) {

        logger.trace("write message {}", message.getQueueMessageId());

        final UUID queueMessageId =  message.getQueueMessageId() == null ?
                QakkaUtils.getTimeUuid() : message.getQueueMessageId();

        Shard.Type shardType = DatabaseQueueMessage.Type.DEFAULT.equals( message.getType() ) ?
                Shard.Type.DEFAULT : Shard.Type.INFLIGHT;

        if ( message.getShardId() == null ) {
            Shard shard = shardStrategy.selectShard(
                    message.getQueueName(), actorSystemFig.getRegionLocal(), shardType, queueMessageId );
            message.setShardId( shard.getShardId() );
        }

        Statement insert = createWriteMessageStatement( message );
        cassandraClient.getQueueMessageSession().execute(insert);

        logger.trace("Wrote queue {} queue message {} shardId {}",
            message.getQueueName(), message.getQueueMessageId(), message.getShardId() );

        shardCounterSerialization.incrementCounter( message.getQueueName(), shardType, message.getShardId(), 1 );

        messageCounterSerialization.incrementCounter( message.getQueueName(), message.getType(), 1L );

        return queueMessageId;
    }


    @Override
    public DatabaseQueueMessage loadMessage(
            final String queueName,
            final String region,
            final Long shardIdOrNull,
            final DatabaseQueueMessage.Type type,
            final UUID queueMessageId ) {

        if ( queueMessageId == null ) {
            return null;
        }

        logger.trace("loadMessage {}", queueMessageId);

        final long shardId;
        if ( shardIdOrNull == null ) {
            Shard.Type shardType = DatabaseQueueMessage.Type.DEFAULT.equals( type ) ?
                    Shard.Type.DEFAULT : Shard.Type.INFLIGHT;
            Shard shard = shardStrategy.selectShard(
                    queueName, region, shardType, queueMessageId );
            shardId = shard.getShardId();
        } else {
            shardId = shardIdOrNull;
        }

        Clause queueNameClause = QueryBuilder.eq(      COLUMN_QUEUE_NAME, queueName );
        Clause regionClause = QueryBuilder.eq(         COLUMN_REGION, region );
        Clause shardIdClause = QueryBuilder.eq(        COLUMN_SHARD_ID, shardId );
        Clause queueMessageIdClause = QueryBuilder.eq( COLUMN_QUEUE_MESSAGE_ID, queueMessageId);

        Statement select = QueryBuilder.select().from(getTableName( type ))
                .where(queueNameClause)
                .and(regionClause)
                .and(shardIdClause)
                .and(queueMessageIdClause);

        Row row = cassandraClient.getQueueMessageSession().execute(select).one();

        if (row == null) {
            return null;
        }

        return new DatabaseQueueMessage(
            row.getUUID(   COLUMN_MESSAGE_ID),
            type,
            row.getString( COLUMN_QUEUE_NAME),
            row.getString( COLUMN_REGION),
            row.getLong(   COLUMN_SHARD_ID),
            row.getLong(   COLUMN_QUEUED_AT),
            row.getLong(   COLUMN_INFLIGHT_AT),
            row.getUUID(   COLUMN_QUEUE_MESSAGE_ID)
        );
    }


    @Override
    public void deleteMessage(
            final String queueName,
            final String region,
            final Long shardIdOrNull,
            final DatabaseQueueMessage.Type type,
            final UUID queueMessageId ) {


        logger.trace("deleteMessage {}", queueMessageId);

        Statement delete = createDeleteMessageStatement( queueName, region, null, type,queueMessageId);
        cassandraClient.getQueueMessageSession().execute( delete );

        messageCounterSerialization.decrementCounter( queueName, type, 1L );
    }


    @Override
    public DatabaseQueueMessageBody loadMessageData(final UUID messageId ){

        logger.trace("loadMessageData {}", messageId);

        Clause messageIdClause = QueryBuilder.eq( COLUMN_MESSAGE_ID, messageId );

        Statement select = QueryBuilder.select().from( TABLE_MESSAGE_DATA).where(messageIdClause);

        Row row = cassandraClient.getApplicationSession().execute(select).one();
        if ( row == null ) {
            return null;
        }

        return new DatabaseQueueMessageBody(
                row.getBytes( COLUMN_MESSAGE_DATA),
                row.getString( COLUMN_CONTENT_TYPE));
    }


    @Override
    public void writeMessageData( final UUID messageId, final DatabaseQueueMessageBody messageBody ) {
        Preconditions.checkArgument(QakkaUtils.isTimeUuid(messageId), "MessageId is not a type 1 UUID");

        logger.trace("writeMessageData {}", messageId);

        Statement insert = QueryBuilder.insertInto(TABLE_MESSAGE_DATA)
                .value( COLUMN_MESSAGE_ID, messageId)
                .value( COLUMN_MESSAGE_DATA, messageBody.getBlob())
                .value( COLUMN_CONTENT_TYPE, messageBody.getContentType())
            .using( QueryBuilder.ttl( maxTtl ) );

        cassandraClient.getApplicationSession().execute(insert);
    }


    @Override
    public void deleteMessageData( final UUID messageId ) {

        logger.trace("deleteMessageData {}", messageId);

        Clause messageIdClause = QueryBuilder.eq(COLUMN_MESSAGE_ID, messageId);
        Statement delete = QueryBuilder.delete().from(TABLE_MESSAGE_DATA).where(messageIdClause);

        cassandraClient.getApplicationSession().execute(delete);
    }


    @Override
    public void putInflight( DatabaseQueueMessage message ) {

        logger.trace("putInflight {}", message.getQueueMessageId());

        // create statement to write queue message to inflight table

        DatabaseQueueMessage inflightMessage = new DatabaseQueueMessage(
            message.getMessageId(),
            DatabaseQueueMessage.Type.INFLIGHT,
            message.getQueueName(),
            message.getRegion(),
            null,
            message.getQueuedAt(),
            System.currentTimeMillis(),
            message.getQueueMessageId() );

        Statement insert = createWriteMessageStatement( inflightMessage );

        // create statement to delete queue message from available table

        Statement delete = createDeleteMessageStatement(
            message.getQueueName(),
            message.getRegion(),
            null,
            DatabaseQueueMessage.Type.DEFAULT,
            message.getQueueMessageId());

        // execute statements as a batch

        BatchStatement batchStatement = new BatchStatement();
        batchStatement.add( insert );
        batchStatement.add( delete );
        cassandraClient.getQueueMessageSession().execute( batchStatement );

        // bump counters

        shardCounterSerialization.incrementCounter(
            message.getQueueName(), Shard.Type.INFLIGHT, message.getShardId(), 1 );

        messageCounterSerialization.incrementCounter(
            message.getQueueName(), DatabaseQueueMessage.Type.INFLIGHT, 1L );

        messageCounterSerialization.decrementCounter(
            message.getQueueName(), DatabaseQueueMessage.Type.DEFAULT, 1L );
    }


    @Override
    public void deleteAllMessages( String queueName ) {

        logger.trace("deleteAllMessages " + queueName);

        Shard.Type[] shardTypes = new Shard.Type[] {Shard.Type.DEFAULT, Shard.Type.INFLIGHT};

        // batch up and then execute delete statements
        BatchStatement deleteAllBatch = new BatchStatement();
        for ( Shard.Type shardType : shardTypes ) {
            ShardIterator defaultShardIterator = new ShardIterator( cassandraClient,
                queueName, actorSystemFig.getRegionLocal(), shardType, Optional.empty() );

            while (defaultShardIterator.hasNext()) {
                Shard shard = defaultShardIterator.next();
                Statement deleteAll = createDeleteAllMessagesStatement( shard );
                deleteAllBatch.add( deleteAll );

                logger.trace("Deleting messages for queue {} shardType {} shard {} query {}",
                    queueName, shardType, shard.getShardId(), deleteAll.toString() );
            }
        }

        cassandraClient.getQueueMessageSession().execute( deleteAllBatch );

        // clear counters, we only want to this to happen after successful deletion
        for ( Shard.Type shardType : shardTypes ) {

            ShardIterator defaultShardIterator = new ShardIterator( cassandraClient,
                queueName, actorSystemFig.getRegionLocal(), shardType, Optional.empty() );

            while (defaultShardIterator.hasNext()) {
                Shard shard = defaultShardIterator.next();

                shardCounterSerialization.resetCounter( shard );

                DatabaseQueueMessage.Type type = Shard.Type.DEFAULT.equals( shardType )
                    ? DatabaseQueueMessage.Type.DEFAULT : DatabaseQueueMessage.Type.INFLIGHT;
                messageCounterSerialization.resetCounter( queueName, type );

                logger.trace("reset counters for queueName {} type {} shard {}",
                    queueName, shardType, shard.getShardId() );
            }
        }

        // TODO: delete message data (separate method)
    }


    private Statement createDeleteAllMessagesStatement( Shard shard ) {

        Clause queueNameClause = QueryBuilder.eq(      COLUMN_QUEUE_NAME, shard.getQueueName() );
        Clause regionClause = QueryBuilder.eq(         COLUMN_REGION, shard.getRegion() );
        Clause shardIdClause = QueryBuilder.eq(        COLUMN_SHARD_ID, shard.getShardId() );

        DatabaseQueueMessage.Type dbqmType = Shard.Type.DEFAULT.equals( shard.getType() )
            ? DatabaseQueueMessage.Type.DEFAULT : DatabaseQueueMessage.Type.INFLIGHT;

        Statement deleteAll = QueryBuilder.delete().from( getTableName( dbqmType ))
            .where(queueNameClause)
            .and(regionClause)
            .and(shardIdClause);

        return deleteAll;
    }


    @Override
    public void timeoutInflight( DatabaseQueueMessage message ) {

        logger.trace("timeoutInflight {}", message.getQueueMessageId() );

        // create statement to write queue message back to available table, with new UUID

        UUID newQueueMessageId = QakkaUtils.getTimeUuid();

        DatabaseQueueMessage newMessage = new DatabaseQueueMessage(
            message.getMessageId(),
            DatabaseQueueMessage.Type.DEFAULT,
            message.getQueueName(),
            message.getRegion(),
            null,
            System.currentTimeMillis(),
            -1L,
            newQueueMessageId );

        Statement write = createWriteMessageStatement( newMessage );

        // create statement to remove message from inflight table

        Statement delete = createDeleteMessageStatement(
            message.getQueueName(),
            message.getRegion(),
            message.getShardId(),
            message.getType(),
            message.getQueueMessageId());

        // execute statements as a batch

        BatchStatement batchStatement = new BatchStatement();
        batchStatement.add( write );
        batchStatement.add( delete );
        cassandraClient.getQueueMessageSession().execute( batchStatement );

        // bump counters

        shardCounterSerialization.incrementCounter(
            message.getQueueName(), Shard.Type.DEFAULT, message.getShardId(), 1 );

        messageCounterSerialization.incrementCounter(
            message.getQueueName(), DatabaseQueueMessage.Type.DEFAULT, 1L );

        messageCounterSerialization.decrementCounter(
            message.getQueueName(), DatabaseQueueMessage.Type.INFLIGHT, 1L );
    }


    private Statement createDeleteMessageStatement( final String queueName,
                                                    final String region,
                                                    final Long shardIdOrNull,
                                                    final DatabaseQueueMessage.Type type,
                                                    final UUID queueMessageId ) {
        final long shardId;
        if ( shardIdOrNull == null ) {
            Shard.Type shardType = DatabaseQueueMessage.Type.DEFAULT.equals( type ) ?
                Shard.Type.DEFAULT : Shard.Type.INFLIGHT;
            Shard shard = shardStrategy.selectShard(
                queueName, region, shardType, queueMessageId );
            shardId = shard.getShardId();
        } else {
            shardId = shardIdOrNull;
        }

        Clause queueNameClause = QueryBuilder.eq(      COLUMN_QUEUE_NAME, queueName );
        Clause regionClause = QueryBuilder.eq(         COLUMN_REGION, region );
        Clause shardIdClause = QueryBuilder.eq(        COLUMN_SHARD_ID, shardId );
        Clause queueMessageIdClause = QueryBuilder.eq( COLUMN_QUEUE_MESSAGE_ID, queueMessageId);

        Statement delete = QueryBuilder.delete().from(getTableName( type ))
            .where(queueNameClause)
            .and(regionClause)
            .and(shardIdClause)
            .and(queueMessageIdClause);

        return delete;
    }


    private Statement createWriteMessageStatement( DatabaseQueueMessage message ) {

        final UUID queueMessageId =  message.getQueueMessageId() == null ?
            QakkaUtils.getTimeUuid() : message.getQueueMessageId();

        final long shardId;

        if ( message.getShardId() != null ) {
            shardId = message.getShardId();

        } else if ( DatabaseQueueMessage.Type.DEFAULT.equals( message.getType() )) {
            Shard shard = shardStrategy.selectShard(
                message.getQueueName(), message.getRegion(), Shard.Type.DEFAULT, message.getQueueMessageId() );
            shardId = shard.getShardId();

        } else {
            Shard shard = shardStrategy.selectShard(
                message.getQueueName(), message.getRegion(), Shard.Type.INFLIGHT, message.getQueueMessageId() );
            shardId = shard.getShardId();
        }

        Statement insert = QueryBuilder.insertInto(getTableName(message.getType()))
            .value( COLUMN_QUEUE_NAME,       message.getQueueName())
            .value( COLUMN_REGION,           message.getRegion())
            .value( COLUMN_SHARD_ID,         shardId)
            .value( COLUMN_MESSAGE_ID,       message.getMessageId())
            .value( COLUMN_QUEUE_MESSAGE_ID, queueMessageId)
            .value( COLUMN_INFLIGHT_AT,      message.getInflightAt())
            .value( COLUMN_QUEUED_AT,        message.getQueuedAt())
            .using( QueryBuilder.ttl( maxTtl ) );

        return insert;
    }


    public static String getTableName(DatabaseQueueMessage.Type messageType){

        String table;
        if( messageType.equals(DatabaseQueueMessage.Type.DEFAULT)) {
            table = TABLE_MESSAGES_AVAILABLE;
        }else if (messageType.equals(DatabaseQueueMessage.Type.INFLIGHT)) {
            table = TABLE_MESSAGES_INFLIGHT;
        }else{
            throw new IllegalArgumentException("Unknown DatabaseQueueMessage Type");
        }

        return table;
    }

    @Override
    public Collection<MultiTenantColumnFamilyDefinition> getColumnFamilies() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<TableDefinition> getTables() {
        return Lists.newArrayList(

            new TableDefinitionStringImpl( cassandraConfig.getApplicationLocalKeyspace(),
                TABLE_MESSAGES_AVAILABLE, MESSAGES_AVAILABLE ),

            new TableDefinitionStringImpl( cassandraConfig.getApplicationLocalKeyspace(),
                TABLE_MESSAGES_INFLIGHT, MESSAGES_INFLIGHT ),

            new TableDefinitionStringImpl( cassandraConfig.getApplicationKeyspace(),
                TABLE_MESSAGE_DATA, MESSAGE_DATA )
        );
    }

}
