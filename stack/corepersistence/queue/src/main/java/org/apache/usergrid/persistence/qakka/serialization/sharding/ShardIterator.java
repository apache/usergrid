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

package org.apache.usergrid.persistence.qakka.serialization.sharding;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.serialization.sharding.impl.ShardSerializationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ShardIterator implements Iterator<Shard> {

    private static final Logger logger = LoggerFactory.getLogger( ShardIterator.class );

    private final CassandraClient cassandraClient;

    private final int PAGE_SIZE = 100;
    private final String queueName;
    private final String region;
    private final Shard.Type shardType;
    private final Optional<Long> shardId;

    private Iterator<Shard> currentIterator;

    private long nextStart = 0L;


    public ShardIterator(
            final CassandraClient cassandraClient,
            final String queueName,
            final String region,
            final Shard.Type shardtype,
            final Optional<Long> lastShardId){

        this.queueName = queueName;
        this.region = region;
        this.shardType = shardtype;
        this.shardId = lastShardId.isPresent() ? lastShardId : Optional.of(0L);
        this.cassandraClient = cassandraClient;
    }

    @Override
    public boolean hasNext() {

        if(currentIterator == null || !currentIterator.hasNext()){
            advance();
        }

        return currentIterator.hasNext();

    }

    @Override
    public Shard next() {

        if ( !hasNext() ) {
            throw new NoSuchElementException( "No next shard exists" );
        }

        return currentIterator.next();

    }

    private void advance(){


        Clause queueNameClause = QueryBuilder.eq( ShardSerializationImpl.COLUMN_QUEUE_NAME, queueName);
        Clause regionClause = QueryBuilder.eq( ShardSerializationImpl.COLUMN_REGION, region);
        Clause activeClause = QueryBuilder.eq( ShardSerializationImpl.COLUMN_ACTIVE, 1);
        Clause shardIdClause;
        if(nextStart == 0L && shardId.isPresent()){
            shardIdClause = QueryBuilder.gt( ShardSerializationImpl.COLUMN_SHARD_ID, shardId.get());
        }else if( nextStart == 0L && !shardId.isPresent()){
            shardIdClause = QueryBuilder.gte( ShardSerializationImpl.COLUMN_SHARD_ID, 0L);

        }else{
            shardIdClause = QueryBuilder.gt( ShardSerializationImpl.COLUMN_SHARD_ID, nextStart);
        }



        Statement query = QueryBuilder.select().all().from(ShardSerializationImpl.getTableName(shardType))
                .where(queueNameClause)
                .and(regionClause)
                .and(activeClause)
                .and(shardIdClause)
                .limit(PAGE_SIZE);

        List<Row> rows = cassandraClient.getSession().execute(query).all();


        currentIterator = getIteratorFromRows(rows);


    }


    private Iterator<Shard> getIteratorFromRows( List<Row> rows){

        List<Shard> shards = new ArrayList<>(rows.size());

        rows.forEach(row -> {

            final String queueName = row.getString( ShardSerializationImpl.COLUMN_QUEUE_NAME);
            final String region = row.getString( ShardSerializationImpl.COLUMN_REGION);
            final long shardId = row.getLong( ShardSerializationImpl.COLUMN_SHARD_ID);
            final UUID pointer = row.getUUID( ShardSerializationImpl.COLUMN_POINTER);

            shards.add(new Shard(queueName, region, shardType, shardId, pointer));

            nextStart = shardId;

        });

        return shards.iterator();

    }


}
