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
 * Represents operations that can be performed on edges within our graph.  A graph should be within an
 * OrganizationScope
 *
 * An Edge: is defined as the following.
 *
 * The edge is directed It has 2 Identifiers (Id).  1 Id is the source node, 1 Id is the target node It has an edge type
 * (a string name)
 *
 * All edges are directed edges.  By definition, the direction is from Source to Target.
 *
 * I.E Source ---- type -----> Target Ex:
 *
 * Dave (user) ----"follows"---> Alex (user)
 *
 * Alex (user)  ----"likes"---> Guinness (beer)
 *
 * Todd (user) ----"worksfor"-----> Apigee (company)
 *
 * Note that edges are directed.  All implementations always have an implicit inverse of the directed edge. This can be
 * used to search both incoming and outgoing edges within the graph.
 *
 * @author tnine
 * @see Edge
 */
public interface EdgeManager {


    /**
     * @param edge The edge to write
     *
     * Create or update an edge.  Note that the implementation should also create incoming (reversed) edges for this
     * edge.
     */
    Observable<Edge> writeEdge( Edge edge );


    /**
     * @param edge The edge to delete
     *
     *
     * Delete the edge. Implementation should also delete the incoming (reversed) edge.
     */
    Observable<Edge> deleteEdge( Edge edge );

    /**
     * TODO: This needs to mark a node as deleted while consistency processing occurs, our reads would need to check this filter on read
     *
     * Remove the node from the graph.
     *
     * @param node
     * @return
     */
    Observable<Id> deleteNode(Id node);

    /**
     * Returns an observable that emits all edges where the specified node is the source node. The edges will match the
     * search criteria of the edge type
     *
     * @param search The search parameters
     *
     * @return An observable that emits Edges. The observer will need to unsubscribe when it has completed consumption.
     */
    Observable<Edge> loadEdgesFromSource( SearchByEdgeType search );

    /**
     * Returns an observable that emits all edges where the specified node is the target node. The edges will match the
     * search criteria of the edge type
     *
     * @param search The search parameters
     *
     * @return An observable that emits Edges. The observer will need to unsubscribe when it has completed consumption.
     */
    Observable<Edge> loadEdgesToTarget( SearchByEdgeType search );


    /**
     * Returns an observable that emits all edges where the specified node is the source node. The edges will match the
     * search criteria of the edge type and the target type
     *
     * @param search The search parameters
     *
     * @return An observable that emits Edges. The observer will need to unsubscribe when it has completed consumption.
     */
    Observable<Edge> loadEdgesFromSourceByType( SearchByIdType search );


    /**
     * Returns an observable that emits all edges where the specified node is the target node. The edges will match the
     * search criteria of the edge type and the target type
     *
     * @param search The search parameters
     *
     * @return An observable that emits Edges. The observer will need to unsubscribe when it has completed consumption.
     */
    Observable<Edge> loadEdgesToTargetByType( SearchByIdType search );

    /**
     * Get all edge types to this node.  The node provided by search is the target node.
     *
     * @param search The search
     *
     * @return An observable that emits strings for edge types
     */
    Observable<String> getEdgeTypesFromSource( SearchEdgeType search );


    /**
     * Get all id types to this node.  The node provided by search is the target node with the edge type to search.
     *
     * @param search The search criteria
     *
     * @return An observable of all source id types
     */
    Observable<String> getIdTypesFromSource( SearchIdType search );


    /**
     * Get all edge types from this node.  The node provided by search is the source node.
     *
     * @param search The search
     *
     * @return An observable that emits strings for edge types
     */
    Observable<String> getEdgeTypesToTarget( SearchEdgeType search );


    /**
     * Get all id types from this node.  The node provided by search is the source node with the edge type to search.
     *
     * @param search The search criteria
     *
     * @return An observable of all source id types
     */
    Observable<String> getIdTypesToTarget( SearchIdType search );
}
