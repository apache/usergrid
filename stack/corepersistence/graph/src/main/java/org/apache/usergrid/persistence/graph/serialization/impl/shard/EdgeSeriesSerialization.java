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
import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.netflix.astyanax.MutationBatch;


/**
 * The interface for creating and retrieving time series row keys
 */
public interface EdgeSeriesSerialization {

    /**
     * Write a new time shard for the meta data
     * @param scope The scope to write
     * @param nodeId The id in the edge
     * @param slice The next time to write
     * @param types The types to write to.  Can be edge type, or edgeType+id type
     */
    public MutationBatch writeEdgeMeta(OrganizationScope scope, Id nodeId, long slice,  String... types);

    /**
     * Get an iterator of all meta data and types.  Returns a range from High to low
     * @param scope The organization scope
     * @param nodeId The id of the node
     * @param start The shard time to start seeking from.  Values <= this value will be returned.
     * @param count The maximum size to return
     * @param types The types to use
     * @return
     */
    public Iterator<Long> getEdgeMetaData(OrganizationScope scope, Id nodeId, long start, int count, String... types);

    /**
     * Remove the slice from the edge meta data from the types.

     * @param scope
     * @param nodeId
     * @param slice
     * @param types
     * @return
     */
    public MutationBatch removeEdgeMeta(OrganizationScope scope, Id nodeId, long slice, String... types);

}
