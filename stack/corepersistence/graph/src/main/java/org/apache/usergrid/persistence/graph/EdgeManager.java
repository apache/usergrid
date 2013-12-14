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

import rx.Observable;


/**
 * Represents operations that can be performed on edges within our graph.  A graph should be within an OrganizationScope
 *
 * An Edge: is defined as the following
 *
 * It has 2 Identifiers (Id)
 * It has a type (a string name)
 *
 * All edges are directed edges.  By definition, the direction is from Source to Target.
 *
 * I.E Source ---- type -----> Target
 * Ex:
 *
 * Dave ----"follows"---> Alex
 *
 * Alex ----"likes"---> Beer
 *
 * Note that edges are directed, however, implementations always have an implicit inverse.
 * This can be used to search both incoming and outgoing edges within the graph.
 *
 * @see Edge
 *
 * @author tnine
 */
public interface EdgeManager {


    /**
     * @param edge The edge to write
     *
     * Create or update an edge.  Note that the implementation should also create incoming (reversed) edges for this
     * edge automatically
     */
    void writeEdge( Edge edge );


    /**
     * @param edge The edge to delete
     *
     *
     * Delete the edge. Implementation should also delete the reversed edge
     */
    void deleteEdge( Edge edge );

    /**
     * Load all edges where the specified node is the source.  The edges will match the search criteria
     *
     * @param search The search parameters
     *
     *
     * @return An observable that emits Edges.  The node specified in the search will be on the source end of the edge.
     * The observer will need to unsubscribe when it has completed consumption.
     */
    Observable<Edge> loadSourceEdges( SearchByEdgeType search );

    /**
     * Load all edges where the node specified is the target node
     * @param search  The search criteria
     *
     * @return An observable that emits Edges.  The node specified in search will be on the target end of the edge
     * The observer will need to unsubscribe when it has completed consumption.
     */
    Observable<Edge> loadTargetEdges( SearchByEdgeType search );


    /**
     * Return an observable that emits edges where the passed search node is the source node
     *
     * @param search The search parameters
     *
     * @return An observable that emits Edges.  Note that only the target type in this edge type will be returned It is
     *         up to the caller to end the subscription to this observable when the desired size is reached
     */
    Observable<Edge> loadSourceEdges( SearchByIdType search );


    /**
     * Return an observable that emits edges where the passed search node is the target node
     *
     * @param search The search parameters
     *
     * @return An observable that emits Edges.  Note that only the target type in this edge type will be returned It
     *         is up to the caller to end the subscription to this observable when the desired size is reached
     */
    Observable<Edge> loadTargetEdges( SearchByIdType search );

    /**
     * Get all edge types to this node.  The node provided by search is the target type
     *
     * @param search The search
     *
     * @return An observable that emits strings for edge types
     */
    Observable<String> getSourceEdgeTypes(SearchEdgeTypes search );


    /**
     * Get all the types of all sources with the specified edge type into this node.  The node in the search
     * is the target node
     *
     * @param search The search criteria
     * @return   An observable of all source id types
     */
    Observable<String> getSourceEdgeIdTypes(SearchEdgeIdTypes search);


    /**
     * Get all edges where the search criteria is the source node
     *
     * @param search The search parameters
     * @return  An observable of all edges types the source node
     */
    Observable<String> getTargetEdgeTypes(SearchEdgeTypes search );


    /**
     * Get the types of all targets where the search criteria is the source node
     *
     * @param search
     * @return An observable of all target id types
     */
    Observable<String> getTargetEdgeIdTypes( SearchEdgeIdTypes search);
}
