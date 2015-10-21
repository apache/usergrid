/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.persistence.graph.serialization.impl.shard;


import java.util.Iterator;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.MarkedEdge;

import com.google.common.util.concurrent.ListenableFuture;


public interface ShardGroupDeletion {


    /**
     * Audit the shard entry group with the given NEW instance of the shardGroupColumnIterator. Returns the future with
     * the outcome of the deletion task
     *
     * @param shardEntryGroup The group to evaluate
     * @param edgeIterator The new instance of the edge iterator
     *
     * @return The delete result with the state of the delete operation
     */
    ListenableFuture<DeleteResult> maybeDeleteShard( final ApplicationScope applicationScope,
                                                     final DirectedEdgeMeta directedEdgeMeta,
                                                     final ShardEntryGroup shardEntryGroup,
                                                     final Iterator<MarkedEdge> edgeIterator );


    enum DeleteResult {
        /**
         * Returned if the shard was delete
         */
        DELETED,

        /**
         * The shard contains edges and cannot be deleted
         */
        CONTAINS_EDGES,

        /**
         * The shard is too new, and may not have been fully replicated, we can't delete it safely
         */
        TOO_NEW,

        /**
         * Our capacity was saturated, we didnt' check the shard
         */
        NOT_CHECKED;
    }
}
