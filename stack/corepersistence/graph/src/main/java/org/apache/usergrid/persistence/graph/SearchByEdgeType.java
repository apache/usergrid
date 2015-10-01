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
 * Defines parameters for a search operation where searching from a source node
 * using a specific type on the edge.  This will return edges with all target types
 *
 * @author tnine */
public interface SearchByEdgeType {

    /**
     * Get the Id of the node of this edge
     * @return
     */
    Id getNode();


    /**
     * Get the name of the edge
     * @return
     */
    String getType();

    /**
     * Get the Maximum Version of an edge we can return.
     * This should always be a type 1 time uuid.
     * @return
     */
    long getMaxTimestamp();

    /**
     * The optional start parameter.  All edges emitted with be > the specified start edge.
     * This is useful for paging.  Simply use the last value returned in the previous call in the start parameter
     * @return
     */
    Optional<Edge> last();

    /**
     * Get the direction we're seeking
     * @return
     */
    Order getOrder();

    /**
     * Return true to filter marked edges from the results
     * @return
     */
    boolean filterMarked();


    /**
     * Options for ordering.  By default, we want to perform descending for common use cases and read speed.  This is our our data
     * is optimized in cassandra
     */
    enum Order {
        DESCENDING,
        ASCENDING
    }

}
