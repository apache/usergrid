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


import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


// TODO: delete me!

public class PlaceholderShardStrategy implements ShardStrategy {

    private Map<String, Shard> shardMap = new HashMap<>();


    @Override
    public Shard selectShard(String queueName, String region, Shard.Type type, UUID pointer) {
        String key = queueName + region + type;
        shardMap.putIfAbsent( key, new Shard( queueName, region, type, 0L, pointer ) );
        return shardMap.get( key );
    }
}
