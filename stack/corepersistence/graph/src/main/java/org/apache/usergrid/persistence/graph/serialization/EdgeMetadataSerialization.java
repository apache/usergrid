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
import org.apache.usergrid.persistence.graph.SearchEdgeIdType;
import org.apache.usergrid.persistence.graph.SearchEdgeType;

import com.netflix.astyanax.MutationBatch;


/**
 * Simple interface for serializing ONLY an edge
 */
public interface EdgeMetadataSerialization {


    /**
     * Write both the source--->Target edge and the target <----- source edge into the mutation
     */
    MutationBatch writeEdge(OrganizationScope scope, Edge edge );

    /**
     *    TODO T.N. delete.  This is a bit complex, we have to read all edges to determine if we can remove meta data from the tree.  We may have to break this down into remove types and direction
     *    so it can be done in parallel
     */

    /**
     * Get all target edge types for the given source node
     *
     * @param search The search to execute
     *
     * @return An iterator of all the edge types
     */
    Iterator<String> getTargetEdgeTypes(OrganizationScope scope, SearchEdgeType search );

    /**
     * Get all target id types on the edge with the type given and the source node
     *
     * @param search The search to execute
     *
     * @return An iterator of all id types
     */
    Iterator<String> getTargetIdTypes(OrganizationScope scope, SearchEdgeIdType search );


    /**
     * Get all target edge types for the given target node
     *
     * @param search The search to execute
     *
     * @return An iterator of all the edge types
     */
    Iterator<String> getSourceEdgeTypes(OrganizationScope scope, SearchEdgeType search );

    /**
     * Get all target id types on the edge with the type given and the target node
     *
     * @param search The search to execute
     *
     * @return An iterator of all id types
     */
    Iterator<String> getSourceIdTypes(OrganizationScope scope, SearchEdgeIdType search );
}
