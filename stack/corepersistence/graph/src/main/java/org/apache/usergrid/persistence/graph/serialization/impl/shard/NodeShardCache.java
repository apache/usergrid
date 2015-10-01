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


/**
 *  Cache implementation for returning versions based on the slice.  This shard may be latent.  As a result
 *  the allocation of new shards should be 2*shard timeout in the future.
 *
 */
public interface NodeShardCache {


    /**
     * Get the shard for the given timestamp
     * @param scope The scope for the application
     * @param timestamp The time to select the slice for.
     * @param directedEdgeMeta The directed edge meta data
     */
    public ShardEntryGroup getWriteShardGroup( final ApplicationScope scope,
                                               final long timestamp, final DirectedEdgeMeta directedEdgeMeta );

    /**
     * Get an iterator of all versions <= the version for iterating shard entry sets.  The iterator of groups will be ordered
     * highest to lowest.  I.E range scanning from Long.MAX_VALUE to 0
     * @param scope The scope for the application
     * @param maxTimestamp The highest timestamp
     * @param directedEdgeMeta The directed edge meta data
     * @return
     */
    public Iterator<ShardEntryGroup> getReadShardGroup( final ApplicationScope scope, final long maxTimestamp, final DirectedEdgeMeta directedEdgeMeta  );

}
