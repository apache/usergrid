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

package org.apache.usergrid.persistence.graph.test.util;


import java.util.UUID;

import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleMarkedEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByIdType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchIdType;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;


/**
 * Simple class for edge testing generation
 */
public class EdgeTestUtils {


    /**
     * Create an edge for testing
     *
     * @param sourceType The source type to use in the id
     * @param edgeType The edge type to use
     * @param targetType The target type to use
     *
     * @return an Edge for testing
     */
    public static MarkedEdge createEdge( final String sourceType, final String edgeType, final String targetType ) {
        return createEdge( createId( sourceType ), edgeType, createId( targetType ), UUIDGenerator.newTimeUUID() );
    }


    /**
     * Create an edge for testing
     */
    public static MarkedEdge createEdge( final Id sourceId, final String edgeType, final Id targetId ) {
        return createEdge( sourceId, edgeType, targetId, UUIDGenerator.newTimeUUID() );
    }


    /**
     * Create an edge with the specified params
     */
    public static MarkedEdge createEdge( final Id sourceId, final String edgeType, final Id targetId, final UUID version ) {
        return new SimpleMarkedEdge( sourceId, edgeType, targetId, version, false );
    }


    /**
     * Create the id
     */
    public static Id createId( String type ) {
        return createId( UUIDGenerator.newTimeUUID(), type );
    }


    /**
     * Generate an ID with the type and id
     *
     * @param id The uuid in the id
     * @param type The type of id
     */
    public static Id createId( UUID id, String type ) {
        return new SimpleId( id, type );
    }


    /**
     *
     * @param sourceId
     * @param type
     * @param maxVersion
     * @param last
     * @return
     */
    public static SearchByEdgeType createSearchByEdge( final Id sourceId, final String type, final UUID maxVersion,
                                                       final Edge last ) {
        return new SimpleSearchByEdgeType( sourceId, type, maxVersion, last );
    }


    /**
     *
     * @param sourceId
     * @param type
     * @param maxVersion
     * @param idType
     * @param last
     * @return
     */
    public static SearchByIdType createSearchByEdgeAndId( final Id sourceId, final String type, final UUID maxVersion,
                                                          final String idType, final Edge last ) {
        return new SimpleSearchByIdType( sourceId, type, maxVersion, idType, last );
    }


    /**
     *
     * @param sourceId
     * @param last
     * @return
     */
    public static SearchEdgeType createSearchEdge( final Id sourceId, final String last ) {
        return new SimpleSearchEdgeType( sourceId, last );
    }


    /**
     * Create the search by Id type
     */
    public static SimpleSearchIdType createSearchIdType( final Id sourceId, final String type, final String last ) {
        return new SimpleSearchIdType( sourceId, type, last );
    }


    /**
     * Get the edge by type
     */
    public static SearchByEdge createGetByEdge( final Id sourceId, final String type, final Id targetId,
                                                final UUID maxVersion, final Edge last ) {
        return new SimpleSearchByEdge( sourceId, type, targetId, maxVersion, last );
    }
}


