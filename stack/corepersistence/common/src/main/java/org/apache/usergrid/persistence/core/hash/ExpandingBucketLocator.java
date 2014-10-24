/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.core.hash;/*
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


import java.util.ArrayList;
import java.util.List;

import com.google.common.hash.Funnel;


/**
 * An algorithm that will generate all possible keys for different "Levels" of sharding.  For instance, imagine this scheme.
 *
 * 1 Shard
 * 2 Shards
 * 4 Shards
 * 8 Shards
 *
 * (note that we do not need to expand by 2x each time, this is merely an example).
 *
 * When seeking on a string key, for 4 levels of the key, we get 4 different keys due to different shard sizes.
 * This is faster than seeking ALL shards, since this would result in 15 shards, vs 4 in the example.
 *
 * @param <T>
 */
public class ExpandingBucketLocator<T> {

    private final BucketLocator<T>[] bucketLocatorList;


    /**
     * Create a new instance with the specified history. For instance, from the javadoc above, the constructor
     * would contains {8, 4, 3, 2, 1}.  Shards are returned in the size order they are given in the constructor
     * @param funnel
     * @param bucketSizes
     */
    public ExpandingBucketLocator( final Funnel<T> funnel, final int... bucketSizes ) {

        bucketLocatorList = new BucketLocator[bucketSizes.length];

        for(int i = 0; i < bucketSizes.length; i ++){
            bucketLocatorList[i] = new BucketLocator<>( funnel, bucketSizes[i] );
        }

    }


    /**
     * Hash the results, and return them in the same order as specified in the constructor
     * @param hash
     * @return
     */
    public int[] getBuckets(T hash){
        int[] results = new int[bucketLocatorList.length];

        for(int i = 0; i < bucketLocatorList.length; i ++){
          results[i] = bucketLocatorList[i].getBucket( hash );
        }

        return results;
    }
}
