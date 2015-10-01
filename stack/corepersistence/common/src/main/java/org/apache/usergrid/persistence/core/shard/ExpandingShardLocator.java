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

package org.apache.usergrid.persistence.core.shard;


import com.google.common.hash.Funnel;


/**
 * An algorithm that will generate all possible keys for different "Levels" of sharding.  For instance, imagine this
 * scheme.
 *
 * 1 Shard 2 Shards 4 Shards 8 Shards
 *
 * (note that we do not need to expand by 2x each time, this is merely an example).
 *
 * When seeking on a string key, for 4 levels of the key, we get 4 different keys due to different shard sizes. This is
 * faster than seeking ALL shards, since this would result in 15 shards, vs 4 in the example.
 */
public class ExpandingShardLocator<T> {

    private final ShardLocator<T>[] shardLocatorList;


    /**
     * Create a new instance with the specified history. For instance, from the javadoc above, the constructor would
     * contains {8, 4, 3, 2, 1}.  Shards are returned in the size order they are given in the constructor
     */
    public ExpandingShardLocator( final Funnel<T> funnel, final int... bucketSizes ) {

        shardLocatorList = new ShardLocator[bucketSizes.length];

        for ( int i = 0; i < bucketSizes.length; i++ ) {
            shardLocatorList[i] = new ShardLocator<>( funnel, bucketSizes[i] );
        }
    }


    /**
     * Hash the results, and return them in the same order as specified in the constructor
     */
    public int[] getAllBuckets( T hash ) {
        int[] results = new int[shardLocatorList.length];

        for ( int i = 0; i < shardLocatorList.length; i++ ) {
            results[i] = shardLocatorList[i].getBucket( hash );
        }

        return results;
    }


    /**
     * Get the current bucket for the hash value.  Hashes from the first element in the list
     */
    public int getCurrentBucket( T hash ) {
        return shardLocatorList[0].getBucket( hash );
    }
}
