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


import java.util.UUID;

import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.serialization.util.EdgeUtils;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;


/**
 * Simple bean implementation of search by edge
 *
 */
public class SimpleSearchByEdge implements SearchByEdge {

    private final Id sourceNode;
    private final Id targetNode;
    private final String type;
    private final long maxTimestamp;
    private final Optional<Edge> last;


    /**
     * Create the search modules
     * @param sourceNode The source node of the edge
     * @param targetNode The target node of the edge
     * @param type The edge type
     * @param maxTimestamp The maximum timestamp to seek from
     * @param last The value to start seeking from.  Must be >= this value
     */
    public SimpleSearchByEdge( final Id sourceNode, final String type, final Id targetNode, final long maxTimestamp, final Edge last ) {
        ValidationUtils.verifyIdentity(sourceNode);
        ValidationUtils.verifyIdentity(targetNode);
        ValidationUtils.verifyString( type, "type" );
        EdgeUtils.validateTimestamp(  maxTimestamp, "maxTimestamp" );


        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        this.type = type;
        this.maxTimestamp = maxTimestamp;
        this.last = Optional.fromNullable(last);
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
    public Optional<Edge> last() {
        return last;
    }
}
