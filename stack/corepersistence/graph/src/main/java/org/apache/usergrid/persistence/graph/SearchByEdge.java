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

package org.apache.usergrid.persistence.graph;


import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;


/**
 * Defines parameters for a search operation to search for a specific edge with a version
 * <= the given version
 *
 * @author tnine */
public interface SearchByEdge {

    /**
     * Get the Id of the node of this edge
     * @return
     */
    Id sourceNode();


    /**
     * Get the id of the target node in the edge
     * @return
     */
    Id targetNode();

    /**
     * Get the name of the edge
     * @return
     */
    String getType();

    /**
     * Get the Maximum timestamp of an edge we can return.

     * @return The max timestamp as a long
     */
    long getMaxTimestamp();

    /**
     * Return true if we should filter edges marked for deletion
     * @return
     */
    boolean filterMarked();

    /**
     * The optional start parameter.  All edges emitted with be > the specified start edge.
     * This is useful for paging.  Simply use the last value returned in the previous call in the start parameter
     * @return
     */
    Optional<Edge> last();

    /**
     * Get the sort order
     * @return
     */
    SearchByEdgeType.Order getOrder();

}
