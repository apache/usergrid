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


import java.util.Iterator;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;

import com.netflix.astyanax.MutationBatch;


/**
 * Simple interface for serializing ONLY an edge
 */
public interface EdgeSerialization {


    /**
     * Write both the source--->Target edge and the target <----- source edge into the mutation
     *
     * @param scope The org scope of the graph
     * @param edge The edge to write
     */
    MutationBatch writeEdge( OrganizationScope scope, Edge edge );


    /**
     * Write both the source -->target edge and the target<--- source edge into the mutation
     *
     * @param scope The org scope of the graph
     * @param edge The edge to write
     */
    MutationBatch deleteEdge( OrganizationScope scope, Edge edge );

    /**
     * Get an iterator of all edges by edge type from source node
     *
     * @param scope The org scope of the graph
     * @param edgeType The search edge
     */
    Iterator<Edge> getTargetEdges( OrganizationScope scope, SearchByEdgeType edgeType );


    /**
     * Get an iterator of all edges by edge type and target type from source node * @param scope The org scope of the
     * graph
     *
     * @param scope The org scope of the graph
     * @param edgeType The search edge
     */
    Iterator<Edge> getTargetIdEdges( OrganizationScope scope, SearchByIdType edgeType );

    /**
     * Get an iterator of all edges by edge type from target node * @param scope The org scope of the graph
     *
     * @param scope The org scope of the graph
     * @param edgeType The search edge
     */
    Iterator<Edge> getSourceEdges( OrganizationScope scope, SearchByEdgeType edgeType );


    /**
     * Get an iterator of all edges by edge type and source type from target node * @param scope The org scope of the
     * graph
     *
     * @param scope The org scope of the graph
     * @param edgeType The search edge
     */
    Iterator<Edge> getSourceIdEdges( OrganizationScope scope, SearchByIdType edgeType );
}
