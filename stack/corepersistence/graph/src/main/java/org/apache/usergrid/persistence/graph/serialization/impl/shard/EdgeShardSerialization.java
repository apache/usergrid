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

import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import com.google.common.base.Optional;
import com.netflix.astyanax.MutationBatch;


/**
 * The interface for creating and retrieving time series row keys
 */
public interface EdgeShardSerialization extends Migration{

    /**
     * Write a new time shard for the meta data
     * @param scope The scope to write
     * @param shard The shard to write
     * @param directedEdgeMeta The edge meta data to use
     */
    public MutationBatch writeShardMeta( ApplicationScope scope, Shard shard, DirectedEdgeMeta directedEdgeMeta );

    /**
     * Get an iterator of all meta data and types.  Returns a range from High to low
     * @param scope The organization scope
     * @param start The shard time to start seeking from.  Values <= this value will be returned.
     * @param directedEdgeMeta The edge meta data to use
     * @return
     */
    public Iterator<Shard> getShardMetaData( ApplicationScope scope, Optional<Shard> start,  DirectedEdgeMeta directedEdgeMeta);

    /**
     * Remove the shard from the edge meta data from the types.

     * @param scope The scope of the application
     * @param shard The shard to remove
     * @param directedEdgeMeta The edge meta data to use
     * @return
     */
    public MutationBatch removeShardMeta( ApplicationScope scope, Shard shard,  DirectedEdgeMeta directedEdgeMeta );

}
