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


import java.util.Collection;
import java.util.Map;

import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.netflix.astyanax.MutationBatch;


/**
 * Simple interface for serializing node information for mark/sweep
 */
public interface NodeSerialization extends Migration {


    /**
     * Mark the node as a pending delete.
     *
     * @param scope The org scope of the graph
     * @param node The node to mark
     * @param timestamp The timestamp to mark for deletion.  Anything <= this time is considered deleted from the graph
     */
    MutationBatch mark( ApplicationScope scope, Id node, long timestamp );


    /**
     * EdgeDelete the mark entry, signaling a delete is complete
     * @param scope
     * @param node
     * @return
     */
    MutationBatch delete( ApplicationScope scope, Id node, long timestamp );

    /**
     * Get the maximum timestamp of a node marked for deletion.  If the node has no mark
     * the optional will return empty
     * @param scope The scope to search in
     * @param nodeId The node id
     * @return The optional timestamp.  If none is present, the node is not currently marked
     */
    Optional<Long> getMaxVersion(ApplicationScope scope, Id nodeId);

    /**
     * Return a map with all max versions from the specified nodeIds.  If no max version is present
     * in the mark, it will not be present in the response
     *
     * @param scope The scope to use
     * @param edges The collection of edges we need to check against.  Both the source and target Id's will be added
     * @return A map of all marked Id's, with the mark version as the value
     */
    Map<Id, Long> getMaxVersions(ApplicationScope scope, Collection<? extends Edge> edges);
}
