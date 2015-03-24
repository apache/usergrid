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

import org.apache.usergrid.persistence.core.migration.data.VersionedData;
import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.SearchIdType;
import org.apache.usergrid.persistence.model.entity.Id;

import com.netflix.astyanax.MutationBatch;


/**
 * Simple interface for serializing an edge meta data
 */
public interface EdgeMetadataSerialization extends Migration, VersionedData {


    /**
     * EdgeWrite both the source--->Target edge and the target <----- source edge into the mutation
     */
    MutationBatch writeEdge( ApplicationScope scope, Edge edge );

    /**
     * Remove all meta data from the source to the target type.  The caller must ensure that this is the last edge with
     * this type at version <= edge version
     *
     * @param scope The org scope
     * @param edge The edge to remove
     *
     * @return a mutation batch with the delete operations
     */
    MutationBatch removeEdgeTypeFromSource( ApplicationScope scope, Edge edge );


    /**
     * Remove the edge type from the source with the specified version
     *
     * @param scope Organization scope
     * @param sourceNode Source node
     * @param type The edge type
     * @param timestamp The version to use on the delete
     *
     * @return A mutation batch to use on issuing the delelete
     */
    MutationBatch removeEdgeTypeFromSource( ApplicationScope scope, Id sourceNode, String type, long timestamp );

    /**
     * Remove all meta data from the source to the target type.  The caller must ensure that this is the last edge with
     * this type at version <= edge version
     *
     * @param scope The org scope
     * @param edge The edge to remove
     *
     * @return a mutation batch with the delete operations
     */
    MutationBatch removeIdTypeFromSource( ApplicationScope scope, Edge edge );


    /**
     * Remove all meta data from the source to the target type.  The caller must ensure that this is the last edge with
     * this type at version <= edge version
     *
     * @param scope Organization scope
     * @param sourceNode Source node
     * @param type The edge type
     * @param idType The idType to use
     * @param timestamp The version to use on the delete
     *
     * @return a mutation batch with the delete operations
     */
    MutationBatch removeIdTypeFromSource( ApplicationScope scope, Id sourceNode, String type, String idType,
                                          long timestamp );

    /**
     * Remove all meta data from the target to the source type.  The caller must ensure that this is the last edge with
     * this type at version <= edge version
     *
     * @param scope The org scope
     * @param edge The edge to remove
     *
     * @return a mutation batch with the delete operations
     */
    MutationBatch removeEdgeTypeToTarget( ApplicationScope scope, Edge edge );


    /**
     * Remove all meta data from the target to the source type.  The caller must ensure that this is the last edge with
     * this type at version <= edge version
     *
     * @param scope Organization scope
     * @param targetNode Source node
     * @param type The edge type
     * @param timestamp The version to use on the delete
     *
     * @return A mutation batch to use on issuing the delelete
     */
    MutationBatch removeEdgeTypeToTarget( ApplicationScope scope, Id targetNode, String type, long timestamp );


    /**
     * Remove all meta data from the target to the source type.  The caller must ensure that this is the last edge with
     * this type at version <= edge version
     *
     * @param scope The org scope
     * @param edge The edge to remove
     *
     * @return a mutation batch with the delete operations
     */
    MutationBatch removeIdTypeToTarget( ApplicationScope scope, Edge edge );


    /**
     * Remove all meta data from the target to the source type.  The caller must ensure that this is the last edge with
     * this type at version <= edge version
     *
     * @param scope Organization scope
     * @param targetNode Source node
     * @param type The edge type
     * @param idType The idType to use
     * @param timestamp The version to use on the delete
     *
     * @return a mutation batch with the delete operations
     */
    MutationBatch removeIdTypeToTarget( ApplicationScope scope, Id targetNode, String type, String idType,
                                        long timestamp );

    /**
     * Get all edge types from the given source node
     *
     * @param search The search to execute
     *
     * @return An iterator of all the edge types
     */
    Iterator<String> getEdgeTypesFromSource( ApplicationScope scope, SearchEdgeType search );

    /**
     * Get all target id types on the edge with the type given from the source node
     *
     * @param search The search to execute
     *
     * @return An iterator of all id types
     */
    Iterator<String> getIdTypesFromSource( ApplicationScope scope, SearchIdType search );


    /**
     * Get all source edge types pointing to the given target node
     *
     * @param search The search to execute
     *
     * @return An iterator of all the edge types
     */
    Iterator<String> getEdgeTypesToTarget( ApplicationScope scope, SearchEdgeType search );

    /**
     * Get all source id types on the edge with the type given pointing to the target node
     *
     * @param search The search to execute
     *
     * @return An iterator of all id types
     */
    Iterator<String> getIdTypesToTarget( ApplicationScope scope, SearchIdType search );
}
