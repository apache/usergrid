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
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.core.CassandraClientImpl;
import org.apache.usergrid.persistence.qakka.core.QakkaUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;


public class ShardStrategyTest extends AbstractTest {

    private static final Logger logger = LoggerFactory.getLogger( ShardStrategyTest.class );


    @Test
    public void testBasicOperation() {

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );


        ShardSerialization shardSer   = getInjector().getInstance( ShardSerialization.class );
        ShardStrategy shardStrategy   = getInjector().getInstance( ShardStrategy.class );

        UUID messageIdToLocate = null;
        long selectedShardId = 4L;

        int numShards = 10;
        String region = "default";
        String queueName = "sst_queue_" + RandomStringUtils.randomAlphanumeric(20);

        for ( long i=0; i<numShards; i++) {
            shardSer.createShard( new Shard( queueName, region, Shard.Type.DEFAULT, i, QakkaUtils.getTimeUuid()));
            try { Thread.sleep(10); } catch (Exception intentionallyIgnored) {}
            if ( i == selectedShardId ) {
                messageIdToLocate = QakkaUtils.getTimeUuid();
            }
            try { Thread.sleep(10); } catch (Exception intentionallyIgnored) {}
        }

        Shard selectedShard = shardStrategy.selectShard( queueName, region, Shard.Type.DEFAULT, messageIdToLocate );

        Assert.assertEquals( selectedShardId, selectedShard.getShardId() );

    }
}
