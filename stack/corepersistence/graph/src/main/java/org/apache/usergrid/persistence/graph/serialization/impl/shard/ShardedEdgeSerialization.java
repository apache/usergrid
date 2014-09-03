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
package org.apache.usergrid.persistence.graph.serialization.impl.shard;


import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;

import com.netflix.astyanax.MutationBatch;


/**
 * Performs serialization on the shards
 */
public interface ShardedEdgeSerialization {

    /**
     * EdgeWrite both the source--->Target edge and the target <----- source edge into the mutation
     *
     * @param columnFamilies The column families to use
     * @param scope The org scope of the graph
     * @param markedEdge The edge to write
     * @param timestamp The timestamp to use
     */
    MutationBatch writeEdgeFromSource( EdgeColumnFamilies columnFamilies, ApplicationScope scope, MarkedEdge markedEdge,
                                       Collection<Shard> shards,  DirectedEdgeMeta sourceEdgeMeta,  UUID timestamp );


    /**
     * Write the edge from source->target
     */
    MutationBatch writeEdgeFromSourceWithTargetType( EdgeColumnFamilies columnFamilies, ApplicationScope scope,
                                                     MarkedEdge markedEdge, Collection<Shard> shards,  DirectedEdgeMeta sourceEdgeMeta, UUID timestamp );

    /**
     * Write the edge from target to source
     */
    MutationBatch writeEdgeToTarget( EdgeColumnFamilies columnFamilies, ApplicationScope scope, MarkedEdge markedEdge,
                                     Collection<Shard> shards,  DirectedEdgeMeta sourceEdgeMeta, UUID timestamp );


    /**
     * Write the edge from target to source with source type
     */
    MutationBatch writeEdgeToTargetWithSourceType( EdgeColumnFamilies columnFamilies, ApplicationScope scope,
                                                   MarkedEdge markedEdge, Collection<Shard> shards, DirectedEdgeMeta sourceEdgeMeta,  UUID timestamp );


    /**
        * EdgeWrite both the source--->Target edge and the target <----- source edge into the mutation
        *
        * @param columnFamilies The column families to use
        * @param scope The org scope of the graph
        * @param markedEdge The edge to write
        * @param timestamp The timestamp to use
        */
       MutationBatch writeEdgeVersions( EdgeColumnFamilies columnFamilies, ApplicationScope scope, MarkedEdge markedEdge,
                                          Collection<Shard> shards,  DirectedEdgeMeta sourceEdgeMeta,  UUID timestamp );



    /**
     * EdgeWrite both the source--->Target edge and the target <----- source edge into the mutation
     *
     * @param columnFamilies The column families to use
     * @param scope The org scope of the graph
     * @param markedEdge The edge to write
     * @param timestamp The timestamp to use
     */
    MutationBatch deleteEdgeFromSource( EdgeColumnFamilies columnFamilies, ApplicationScope scope,
                                        MarkedEdge markedEdge, Collection<Shard> shards,  DirectedEdgeMeta sourceEdgeMeta, UUID timestamp );


    /**
     * Write the edge from source->target
     */
    MutationBatch deleteEdgeFromSourceWithTargetType( EdgeColumnFamilies columnFamilies, ApplicationScope scope,
                                                      MarkedEdge markedEdge, Collection<Shard> shards,  DirectedEdgeMeta sourceEdgeMeta,  UUID timestamp );

    /**
     * Write the edge from target to source
     */
    MutationBatch deleteEdgeToTarget( EdgeColumnFamilies columnFamilies, ApplicationScope scope, MarkedEdge markedEdge,
                                      Collection<Shard> shards, DirectedEdgeMeta sourceEdgeMeta,  UUID timestamp );


    /**
     * Write the edge from target to source with source type
     */
    MutationBatch deleteEdgeToTargetWithSourceType( EdgeColumnFamilies columnFamilies, ApplicationScope scope,
                                                    MarkedEdge markedEdge, Collection<Shard> shards,  DirectedEdgeMeta sourceEdgeMeta, UUID timestamp );

    /**
            * EdgeWrite both the source--->Target edge and the target <----- source edge into the mutation
            *
            * @param columnFamilies The column families to use
            * @param scope The org scope of the graph
            * @param markedEdge The edge to write
            * @param timestamp The timestamp to use
            */
           MutationBatch deleteEdgeVersions( EdgeColumnFamilies columnFamilies, ApplicationScope scope, MarkedEdge markedEdge,
                                              Collection<Shard> shards,  DirectedEdgeMeta sourceEdgeMeta,  UUID timestamp );


    /**
     * Search for all versions of this edge < the search version.  Returns all versions
     *
     * @param columnFamilies The column families to use
     * @param scope The application scope
     * @param search The search criteria
     * @param shards The shards multiget when reading
     */
    Iterator<MarkedEdge> getEdgeVersions( EdgeColumnFamilies columnFamilies, ApplicationScope scope,
                                          SearchByEdge search, Collection<Shard> shards );

    /**
     * Get an iterator of all edges by edge type originating from source node
     *
     * @param columnFamilies The column families to use
     * @param scope The application scope
     * @param search The search criteria
     * @param shards The shards to iterate when searching
     */
    Iterator<MarkedEdge> getEdgesFromSource( EdgeColumnFamilies columnFamilies, ApplicationScope scope,
                                             SearchByEdgeType search, Collection<Shard> shards );


    /**
     * Get an iterator of all edges by edge type originating from source node.  Also filters by target node id type
     *
     * @param columnFamilies The column families to use
     * @param scope The application scope
     * @param search The search criteria
     * @param shards The shards to iterate when searching
     */
    Iterator<MarkedEdge> getEdgesFromSourceByTargetType( EdgeColumnFamilies columnFamilies, ApplicationScope scope,
                                                         SearchByIdType search, Collection<Shard> shards );

    /**
     * Get an iterator of all edges by edge type pointing to the target node.  Returns all versions
     *
     * @param columnFamilies The column families to use
     * @param scope The application scope
     * @param search The search criteria
     * @param shards The shards to iterate when searching
     */
    Iterator<MarkedEdge> getEdgesToTarget( EdgeColumnFamilies columnFamilies, ApplicationScope scope,
                                           SearchByEdgeType search, Collection<Shard> shards );


    /**
     * Get an iterator of all edges by edge type pointing to the target node.  Also uses the source id type to limit the
     * results
     *
     * @param columnFamilies The column families to use
     * @param scope The application scope
     * @param search The search criteria
     * @param shards The shards to iterate when searching
     */
    Iterator<MarkedEdge> getEdgesToTargetBySourceType( EdgeColumnFamilies columnFamilies, ApplicationScope scope,
                                                       SearchByIdType search, Collection<Shard> shards );
}
