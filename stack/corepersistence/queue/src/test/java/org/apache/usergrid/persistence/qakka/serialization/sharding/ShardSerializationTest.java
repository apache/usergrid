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

import org.apache.usergrid.persistence.qakka.core.CassandraClientImpl;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.core.QakkaUtils;
import org.apache.usergrid.persistence.qakka.serialization.sharding.impl.ShardSerializationImpl;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * Created by russo on 6/8/16.
 */
public class ShardSerializationTest extends AbstractTest {

    private static final Logger logger = LoggerFactory.getLogger( ShardSerializationTest.class );


    @Test
    public void writeNewShard(){

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );
        ShardSerialization shardSerialization = new ShardSerializationImpl( cassandraClient );

        Shard shard1 = new Shard("test", "region1", Shard.Type.DEFAULT, 100L, null);
        shardSerialization.createShard(shard1);
    }

    @Test
    public void deleteShard(){

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );
        ShardSerialization shardSerialization = new ShardSerializationImpl( cassandraClient );

        Shard shard1 = new Shard("test", "region1", Shard.Type.DEFAULT, 100L, null);

        shardSerialization.createShard(shard1);
        shardSerialization.deleteShard(shard1);
        assertNull(shardSerialization.loadShard(shard1));



    }

    @Test
    public void loadNullShard(){

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );
        ShardSerialization shardSerialization = new ShardSerializationImpl( cassandraClient );

        Shard shard1 = new Shard("junk", "region1", Shard.Type.DEFAULT, 100L, null);

        assertNull(shardSerialization.loadShard(shard1));



    }

    @Test
    public void updatePointer(){

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );
        ShardSerialization shardSerialization = new ShardSerializationImpl( cassandraClient );
        
        Shard shard1 = new Shard("test", "region1", Shard.Type.DEFAULT, 100L, null);
        shardSerialization.createShard(shard1);

        final UUID pointer = QakkaUtils.getTimeUuid();

        shard1.setPointer(pointer);
        shardSerialization.updateShardPointer(shard1);

        Shard returnedShard = shardSerialization.loadShard(shard1);

        assertEquals(pointer, returnedShard.getPointer());


    }

}
