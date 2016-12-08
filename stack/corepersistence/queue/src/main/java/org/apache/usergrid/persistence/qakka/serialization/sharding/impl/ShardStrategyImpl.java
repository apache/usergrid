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

import com.google.inject.Inject;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.exceptions.NotFoundException;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardIterator;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardStrategy;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.UUID;


public class ShardStrategyImpl implements ShardStrategy {

    final CassandraClient cassandraClient;

    @Inject
    public ShardStrategyImpl(CassandraClient cassandraClient) {
        this.cassandraClient = cassandraClient;
    }

    @Override
    public Shard selectShard(String queueName, String region, Shard.Type shardType, UUID pointer) {

        // use shard iterator to walk through shards until shard can be found

        ShardIterator shardIterator = new ShardIterator(
                cassandraClient, queueName, region, shardType, Optional.empty() );

        if ( !shardIterator.hasNext() ) {
            String msg = MessageFormat.format(
                    "No shards found for queue {0} region {1} type {2}", queueName, region, shardType );
            throw new NotFoundException( msg );
        }

        // walk through shards from oldest to newest

        Shard prev = shardIterator.next();
        while ( shardIterator.hasNext() ) {
            Shard next = shardIterator.next();

            // if item is older than the next shard, the use prev shard
            if ( pointer.timestamp() < next.getPointer().timestamp() ) {
                return prev;
            }
            prev = next;
        }
        return prev;
    }
}
