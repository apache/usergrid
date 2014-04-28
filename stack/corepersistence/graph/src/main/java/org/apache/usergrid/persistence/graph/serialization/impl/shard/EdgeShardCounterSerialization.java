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

package org.apache.usergrid.persistence.graph.serialization.impl.shard;


import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.core.migration.Migration;
import org.apache.usergrid.persistence.model.entity.Id;

import com.netflix.astyanax.MutationBatch;


/**
 * The interface to define counter operations.  Note that the implementation may not be immediately consistent.
 *
 * TODO: Ivestigate this further with CountMinSketch.  Since our cardinality needs to be "good enough", we may be able
 * to offer much better performance than the Cassandra counters by using CountMinSketch in a time series manner on each node, and persisting it's map
 * in memory with period flush into a standard CF.  On query, we can read a unioned column.
 * On flush, we can flush, then read+union and set the timestamp on the column so that only 1 union will be the max.
 *
 */
public interface EdgeShardCounterSerialization extends Migration{

    /**
     * Write a new time shard for the meta data
     * @param scope The scope to write
     * @param nodeId The id in the edge
     * @param shardId The shard Id to use
     * @param types The types to write to.  Can be edge type, or edgeType+id type
     */
    public MutationBatch writeMetaDataLog( OrganizationScope scope, Id nodeId, long shardId, long count, String... types );


    /**
     * Get the most recent rollup of all of the given summations.  If one is not present the optional will be empty
     * @param scope
     * @param nodeId
     * @param shardId
     * @param types
     * @return
     */
    public long getCount(OrganizationScope scope, Id nodeId, long shardId, String... types);



}
