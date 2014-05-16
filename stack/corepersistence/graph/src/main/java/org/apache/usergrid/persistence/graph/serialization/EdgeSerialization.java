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
import java.util.UUID;

import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.core.migration.Migration;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;

import com.netflix.astyanax.MutationBatch;


/**
 * Simple interface for serializing ONLY an edge
 */
public interface EdgeSerialization extends Migration {


    /**
     * EdgeWrite both the source--->Target edge and the target <----- source edge into the mutation
     *
     * @param scope The org scope of the graph
     * @param edge The edge to write
     */
    MutationBatch writeEdge( OrganizationScope scope, MarkedEdge edge, UUID timestamp );

    /**
     * EdgeWrite both the source -->target edge and the target<--- source edge into the mutation
     *
     * @param scope The org scope of the graph
     * @param edge The edge to write
     */
    MutationBatch deleteEdge( OrganizationScope scope, MarkedEdge edge, UUID timestamp );


    /**
     * Search for all versions of this edge < the search version.  Returns all versions
     * @param scope
     * @param search
     * @return
     */
    Iterator<MarkedEdge> getEdgeVersions( OrganizationScope scope, SearchByEdge search );

    /**
     * Get an iterator of all edges by edge type originating from source node
     *
     * @param scope The org scope of the graph
     * @param edgeType The search edge
     */
    Iterator<MarkedEdge> getEdgesFromSource( OrganizationScope scope, SearchByEdgeType edgeType );


    /**
     * Get an iterator of all edges by edge type originating from source node.  Also filters by target node id type
     *
     * @param scope The org scope of the graph
     * @param edgeType The search edge
     */
    Iterator<MarkedEdge> getEdgesFromSourceByTargetType( OrganizationScope scope, SearchByIdType edgeType );

    /**
     * Get an iterator of all edges by edge type pointing to the target node.  Returns all versions
     *
     * @param scope The org scope of the graph
     * @param edgeType The search edge
     */
    Iterator<MarkedEdge> getEdgesToTarget( OrganizationScope scope, SearchByEdgeType edgeType );


    /**
     * Get an iterator of all edges by edge type pointing to the target node.  Also uses the source id type to limit the
     * results
     *
     * @param scope The org scope of the graph
     * @param edgeType The search edge
     */
    Iterator<MarkedEdge> getEdgesToTargetBySourceType( OrganizationScope scope, SearchByIdType edgeType );
}
