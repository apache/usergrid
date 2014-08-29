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


import org.apache.usergrid.persistence.core.scope.ApplicationScope;


/**
 * Interface for creating approximate estimates of shards
 */
public interface NodeShardApproximation {


    /**
     * Increment the shard Id the specified amount
     *
     * @param scope The scope
     * @param shard The shard to use
     * @param count The count to increment
     * @param directedEdgeMeta The directed edge meta data to use
     */
    public void increment( final ApplicationScope scope, final Shard shard,
                           final long count, final DirectedEdgeMeta directedEdgeMeta );


    /**
     * Get the approximation of the number of unique items
     *
     * @param scope The scope
     * @param directedEdgeMeta The directed edge meta data to use
     */
    public long getCount( final ApplicationScope scope, final Shard shard,  final DirectedEdgeMeta directedEdgeMeta );


    /**
     * Flush the current counters in the Approximation.  Will return immediately after the flush. You can then use flushPending
     * to check the state.
     */
    public void beginFlush();

    /**
     * Return true if there is data to be flushed
     * @return
     */
    public boolean flushPending();


}
