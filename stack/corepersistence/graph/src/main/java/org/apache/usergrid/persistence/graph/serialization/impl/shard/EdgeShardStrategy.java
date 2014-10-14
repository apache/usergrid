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


import java.util.Iterator;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;


public interface EdgeShardStrategy {

    /**
     * Get the shard key used for writing this shard.  CUD operations should use this
     *
     * @param scope The application's scope]
     * @param timestamp The timestamp on the edge
     */
    public ShardEntryGroup getWriteShards( final ApplicationScope scope, final long timestamp, final DirectedEdgeMeta directedEdgeMeta );


    /**
     * Get the iterator of all shards for this entity
     *
     * @param scope The application scope
     * @param maxTimestamp The max timestamp to use
     */
    public Iterator<ShardEntryGroup> getReadShards(final ApplicationScope scope, final long maxTimestamp, final DirectedEdgeMeta directedEdgeMeta );

    /**
     * Increment our count meta data by the passed value.  Can be a positive or a negative number.
     * @param scope The scope in the application
     * @param shard The shard to use
     * @param count The amount to increment or decrement
     * @param directedEdgeMeta The edge meta data to use
     * @return
     */
    public void increment(final ApplicationScope scope, Shard shard, long count, final DirectedEdgeMeta directedEdgeMeta );




}
