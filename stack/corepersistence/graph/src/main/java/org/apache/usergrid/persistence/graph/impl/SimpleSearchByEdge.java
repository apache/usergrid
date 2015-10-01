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

package org.apache.usergrid.persistence.graph.impl;


import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.serialization.util.GraphValidation;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;


/**
 * Simple bean implementation of search by edge
 */
public class SimpleSearchByEdge implements SearchByEdge {

    private final Id sourceNode;
    private final Id targetNode;
    private final String type;
    private final long maxTimestamp;
    private final Optional<Edge> last;
    private final SearchByEdgeType.Order order;
    private final boolean filterMarked;


    /**
     * Create the search modules
     *
     * @param sourceNode The source node of the edge
     * @param targetNode The target node of the edge
     * @param type The edge type
     * @param maxTimestamp The maximum timestamp to seek from
     * @param last The value to start seeking from.  Must be >= this value
     */
    public SimpleSearchByEdge( final Id sourceNode, final String type, final Id targetNode, final long maxTimestamp,
                               final SearchByEdgeType.Order order, final Optional<Edge> last ) {
        this( sourceNode, type, targetNode, maxTimestamp, order, last, true );
    }


    /**
     * Create the search modules
     *
     * @param sourceNode The source node of the edge
     * @param type The edge type
     * @param targetNode The target node of the edge
     * @param maxTimestamp The maximum timestamp to seek from
     * @param last The value to start seeking from.  Must be >= this value
     */
    public SimpleSearchByEdge( final Id sourceNode, final String type, final Id targetNode, final long maxTimestamp,
                               final SearchByEdgeType.Order order, final Optional<Edge> last,
                               final boolean filterMarked ) {


        ValidationUtils.verifyIdentity( sourceNode );
        ValidationUtils.verifyIdentity( targetNode );
        ValidationUtils.verifyString( type, "type" );
        GraphValidation.validateTimestamp( maxTimestamp, "maxTimestamp" );
        Preconditions.checkNotNull( order, "order must not be null" );
        Preconditions.checkNotNull( last, "last can never be null" );


        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        this.type = type;
        this.maxTimestamp = maxTimestamp;
        this.order = order;
        this.last = last;
        this.filterMarked = filterMarked;
    }


    @Override
    public Id sourceNode() {
        return sourceNode;
    }


    @Override
    public Id targetNode() {
        return targetNode;
    }


    @Override
    public String getType() {
        return type;
    }


    @Override
    public long getMaxTimestamp() {
        return maxTimestamp;
    }


    @Override
    public boolean filterMarked() { return filterMarked; }


    @Override
    public Optional<Edge> last() {
        return last;
    }


    @Override
    public SearchByEdgeType.Order getOrder() {
        return order;
    }
}
