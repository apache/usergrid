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

package org.apache.usergrid.persistence.graph.serialization;


import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.clearspring.analytics.stream.cardinality.HyperLogLog;
import com.google.common.base.Optional;
import com.netflix.astyanax.MutationBatch;


/**
 * The interface to define counter operations.  Note that the implementation may not be immediately consistent.
 *
 * TODO: Ivestigate this further with HyperLogLog.  Since our cardinality needs to be "good enough", we may be able
 * to offer much better performance than the Cassandra counters by using hyperloglog on each node, and persisting it's map
 * in memory with period flush into a standard CF.  On query, we can read a unioned column.
 * On flush, we can flush, then read+union and set the timestamp on the column so that only 1 union will be the max.
 *
 * http://blog.aggregateknowledge.com/2012/10/25/sketch-of-the-day-hyperloglog-cornerstone-of-a-big-data-infrastructure/
 *
 * See also
 *
 * http://blog.aggregateknowledge.com/2012/10/25/sketch-of-the-day-hyperloglog-cornerstone-of-a-big-data-infrastructure/
 *
 * See also
 *
 * https://github.com/addthis/stream-lib/blob/master/src/main/java/com/clearspring/analytics/stream/cardinality/HyperLogLog.java
 *
 */
public interface EdgeSeriesCounterSerialization {

    /**
     * Write a new time shard for the meta data
     * @param scope The scope to write
     * @param nodeId The id in the edge
     * @param shardId The shard Id to use
     * @param types The types to write to.  Can be edge type, or edgeType+id type
     */
    public MutationBatch writeMetaDataLog( OrganizationScope scope, Id nodeId, UUID shardId, HyperLogLog log, UUID workerId, String... types );

    /**
     * return all persistent instance of the hyperlog log
     * @param scope
     * @param nodeId
     * @param shardId
     * @param types
     * @return
     */

    public List<HyperLogLog> getMetadataLog( OrganizationScope scope, Id nodeId, UUID shardId, String... types );

    /**
     * Get the most recent rollup of all of the given summations.  If one is not present the optional will be empty
     * @param scope
     * @param nodeId
     * @param shardId
     * @param types
     * @return
     */
    public Optional<HyperLogLog> getSummationLog(OrganizationScope scope, Id nodeId, UUID shardId, String... types);

    /**
     * Write the summation log.  Uses the timestamp passed in the column
     * @param scope The scope to write
     * @param nodeId The id in the edge
     * @param shardId The shard Id to use
     * @param log The log to write
     * @param creatorId The identifier of this writer
     * @param types The types to write to.  Can be edge type, or edgeType+id type
     */
    public MutationBatch writeSummationLog( OrganizationScope scope, Id nodeId, UUID shardId, HyperLogLog log, UUID workerId,  String... types );


    /**
     * Remove the slice from the edge meta data from the types.
     * @param scope
     * @param nodeId
     * @param shardId
     * @param types
     * @return
     */
    public MutationBatch removeEdgeMetadataCount( OrganizationScope scope, Id nodeId, UUID shardId, String... types );

}
