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

import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.core.CassandraConfig;
import org.apache.usergrid.persistence.qakka.core.CassandraClientImpl;
import org.apache.usergrid.persistence.qakka.serialization.sharding.impl.ShardSerializationImpl;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.core.QakkaUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.assertTrue;


/**
 * Created by russo on 6/9/16.
 */
public class ShardIteratorTest extends AbstractTest {

    private static final Logger logger = LoggerFactory.getLogger( ShardIteratorTest.class );

    @Test
    public void getActiveShards(){

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );
        CassandraConfig cassandraConfig = getInjector().getInstance( CassandraConfig.class );
        ShardSerialization shardSerialization = new ShardSerializationImpl( cassandraConfig, cassandraClient );

        String queueName = "queue_sit_" + RandomStringUtils.randomAlphanumeric( 10 );

        Shard shard1 = new Shard(queueName, "region1", Shard.Type.DEFAULT, 100L, null);
        Shard shard2 = new Shard(queueName, "region1", Shard.Type.DEFAULT, 200L, null);

        shardSerialization.createShard(shard1);
        shardSerialization.createShard(shard2);

        Iterator<Shard> shardIterator = new ShardIterator(
                cassandraClient, queueName, "region1", Shard.Type.DEFAULT, Optional.empty());

        List<Shard> shards = new ArrayList<>(1);


        shardIterator.forEachRemaining(shard -> {

            logger.info("Shard ID: {}", shard.getShardId());
            shards.add(shard);

        });

        assertTrue(shards.size() == 2 && shards.get(0).equals(shard1) && shards.get(1).equals(shard2));


    }

    @Test
    public void seekActiveShards(){

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );
        CassandraConfig cassandraConfig = getInjector().getInstance( CassandraConfig.class );
        ShardSerialization shardSerialization = new ShardSerializationImpl( cassandraConfig, cassandraClient );

        String queueName = "queue_sit_" + RandomStringUtils.randomAlphanumeric( 10 );

        Shard shard1 = new Shard(queueName, "region1", Shard.Type.DEFAULT, 100L, null);
        Shard shard2 = new Shard(queueName, "region1", Shard.Type.DEFAULT, 200L, null);
        Shard shard3 = new Shard(queueName, "region1", Shard.Type.DEFAULT, 300L, null);

        shardSerialization.createShard(shard1);
        shardSerialization.createShard(shard2);
        shardSerialization.createShard(shard3);


        Iterator<Shard> shardIterator = new ShardIterator(
                cassandraClient, queueName, "region1", Shard.Type.DEFAULT, Optional.of(200L));

        List<Shard> shards = new ArrayList<>(1);

        shardIterator.forEachRemaining(shard -> {

            logger.info("Shard ID: {}", shard.getShardId());
            shards.add(shard);

        });

        assertTrue(shards.size() == 1 && shards.get(0).equals(shard3));
    }


    @Test
    public void shardIteratorOrdering() throws Exception {

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );
        CassandraConfig cassandraConfig = getInjector().getInstance( CassandraConfig.class );
        ShardSerialization shardSerialization = new ShardSerializationImpl( cassandraConfig, cassandraClient );

        int numShards = 10;
        String region = "default";
        String queueName = "sit_queue_" + RandomStringUtils.randomAlphanumeric(20);

        for ( long i=0; i<numShards; i++) {
            UUID messageId = QakkaUtils.getTimeUuid();
            Shard shard = new Shard( queueName, region, Shard.Type.DEFAULT, i+1, messageId );
            shardSerialization.createShard( shard );
            try { Thread.sleep(10); } catch (Exception intentionallyIgnored) {}
        }

        Iterator<Shard> shardIterator = new ShardIterator(
                cassandraClient, queueName, region, Shard.Type.DEFAULT, Optional.empty());

        int count = 0;
        Long prevTimestamp = null;
        while ( shardIterator.hasNext() ) {
            Shard shard = shardIterator.next();
            if ( prevTimestamp != null ) {
                Assert.assertTrue( prevTimestamp < shard.getPointer().timestamp() );
            }
            prevTimestamp = shard.getPointer().timestamp();
            count++;
        }

        Assert.assertEquals( numShards, count );
    }
}
