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

package org.apache.usergrid.persistence.qakka.serialization.sharding.impl;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Assignment;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.core.CassandraFig;
import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.datastax.TableDefinition;
import org.apache.usergrid.persistence.core.datastax.impl.TableDefinitionStringImpl;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;


public class ShardSerializationImpl implements ShardSerialization {

    private static final Logger logger = LoggerFactory.getLogger( ShardSerializationImpl.class );

    private final CassandraClient cassandraClient;
    private final CassandraFig cassandraFig;

    public final static String COLUMN_QUEUE_NAME = "queue_name";
    public final static String COLUMN_REGION = "region";
    public final static String COLUMN_SHARD_ID = "shard_id";
    public final static String COLUMN_ACTIVE = "active";
    public final static String COLUMN_POINTER = "pointer";


    public final static String TABLE_SHARDS_MESSAGES_AVAILABLE = "shards_messages_available";

    public final static String TABLE_SHARDS_MESSAGES_INFLIGHT = "shards_messages_inflight";


    static final String SHARDS_MESSAGES_AVAILABLE =
            "CREATE TABLE IF NOT EXISTS shards_messages_available ( " +
                    "queue_name text, " +
                    "region     text, " +
                    "shard_id   bigint, " +
                    "active     int, " +
                    "pointer    timeuuid, " +
                    "PRIMARY KEY ((queue_name, region), active, shard_id) " +
                    ") WITH CLUSTERING ORDER BY (active DESC, shard_id ASC); ";

    static final String SHARDS_MESSAGES_AVAILABLE_INFLIGHT =
            "CREATE TABLE IF NOT EXISTS shards_messages_inflight ( " +
                    "queue_name text, " +
                    "region     text, " +
                    "shard_id   bigint, " +
                    "active     int, " +
                    "pointer    timeuuid, " +
                    "PRIMARY KEY ((queue_name, region), active, shard_id) " +
                    ") WITH CLUSTERING ORDER BY (active DESC, shard_id ASC); ";


    @Inject
    public ShardSerializationImpl( CassandraFig cassandraFig,  CassandraClient cassandraClient ) {
        this.cassandraFig = cassandraFig;
        this.cassandraClient = cassandraClient;
    }

    public void createShard(final Shard shard){

        Statement insert = QueryBuilder.insertInto(getTableName(shard.getType()))
                .value(COLUMN_QUEUE_NAME, shard.getQueueName())
                .value(COLUMN_REGION, shard.getRegion())
                .value(COLUMN_SHARD_ID, shard.getShardId())
                .value(COLUMN_ACTIVE, 1)
                .value(COLUMN_POINTER, shard.getPointer());

        cassandraClient.getQueueMessageSession().execute(insert);

    }

    public Shard loadShard(final Shard shard){

        Clause queueNameClause = QueryBuilder.eq(COLUMN_QUEUE_NAME, shard.getQueueName());
        Clause regionClause = QueryBuilder.eq(COLUMN_REGION, shard.getRegion());
        Clause activeClause = QueryBuilder.eq(COLUMN_ACTIVE, 1);
        Clause shardIdClause = QueryBuilder.eq(COLUMN_SHARD_ID, shard.getShardId());



        Statement select = QueryBuilder.select().from(getTableName(shard.getType()))
                .where(queueNameClause)
                .and(regionClause)
                .and(activeClause)
                .and(shardIdClause);

        Row row = cassandraClient.getQueueMessageSession().execute(select).one();

        if (row == null){
            return null;
        }

        final String queueName = row.getString(COLUMN_QUEUE_NAME);
        final String region = row.getString(COLUMN_REGION);
        final long shardId = row.getLong(COLUMN_SHARD_ID);
        final UUID pointer = row.getUUID(COLUMN_POINTER);

        return new Shard(queueName, region, shard.getType(), shardId, pointer);



    }


    public void deleteShard(final Shard shard){

        Clause queueNameClause = QueryBuilder.eq(COLUMN_QUEUE_NAME, shard.getQueueName());
        Clause regionClause = QueryBuilder.eq(COLUMN_REGION, shard.getRegion());
        Clause activeClause = QueryBuilder.eq(COLUMN_ACTIVE, 1);
        Clause shardIdClause = QueryBuilder.eq(COLUMN_SHARD_ID, shard.getShardId());



        Statement delete = QueryBuilder.delete().from(getTableName(shard.getType()))
                .where(queueNameClause)
                .and(regionClause)
                .and(activeClause)
                .and(shardIdClause);

        cassandraClient.getQueueMessageSession().execute(delete);

    }

    public void updateShardPointer(final Shard shard){

        Assignment assignment = QueryBuilder.set(COLUMN_POINTER, shard.getPointer());

        Clause queueNameClause = QueryBuilder.eq(COLUMN_QUEUE_NAME, shard.getQueueName());
        Clause regionClause = QueryBuilder.eq(COLUMN_REGION, shard.getRegion());
        Clause activeClause = QueryBuilder.eq(COLUMN_ACTIVE, 1);
        Clause shardIdClause = QueryBuilder.eq(COLUMN_SHARD_ID, shard.getShardId());

        Statement update = QueryBuilder.update(getTableName(shard.getType()))
                .with(assignment)
                .where(queueNameClause)
                .and(regionClause)
                .and(activeClause)
                .and(shardIdClause);

        cassandraClient.getQueueMessageSession().execute(update);

    }

    public static String getTableName(Shard.Type shardType){

        String table;
        if( shardType.equals(Shard.Type.DEFAULT)) {
            table = TABLE_SHARDS_MESSAGES_AVAILABLE;
        }else if (shardType.equals(Shard.Type.INFLIGHT)) {
            table = TABLE_SHARDS_MESSAGES_INFLIGHT;
        }else{
            throw new IllegalArgumentException("Unknown ShardType");
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
                new TableDefinitionStringImpl( cassandraFig.getApplicationLocalKeyspace(),
                    TABLE_SHARDS_MESSAGES_AVAILABLE, SHARDS_MESSAGES_AVAILABLE ),
                new TableDefinitionStringImpl( cassandraFig.getApplicationLocalKeyspace(),
                    TABLE_SHARDS_MESSAGES_INFLIGHT, SHARDS_MESSAGES_AVAILABLE_INFLIGHT )
        );
    }

}
