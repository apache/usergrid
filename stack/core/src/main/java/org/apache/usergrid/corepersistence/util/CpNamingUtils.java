package org.apache.usergrid.corepersistence.util;
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


import java.util.UUID;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.index.impl.IndexEdgeImpl;
import org.apache.usergrid.persistence.index.impl.SearchEdgeImpl;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.utils.UUIDUtils;


/**
 * Utilises for constructing standard naming conventions for collections and connections
 */
public class CpNamingUtils {

    /** Edge types for collection suffix */
    public static final String EDGE_COLL_SUFFIX = "zzzcollzzz";

    /** Edge types for connection suffix */
    public static final String EDGE_CONN_SUFFIX = "zzzconnzzz";

    /** App where we store management info */
    public static final UUID MANAGEMENT_APPLICATION_ID = UUID.fromString( "b6768a08-b5d5-11e3-a495-11ddb1de66c8" );

    /** Old and deprecated SYSTEM_APP */
    public static final UUID SYSTEM_APP_ID = UUID.fromString( "b6768a08-b5d5-11e3-a495-10ddb1de66c3" );

    /**
     * Information about applications is stored in the management app using these types
     */
    public static final String APPLICATION_INFO = "application-info";
    public static final String APPLICATION_INFOS = "application-infos";

    public static final String DELETED_APPLICATION_INFO = "deleted-application-info";
    public static final String DELETED_APPLICATION_INFOS = "deleted-application-infos";

    /**
     * The name of the map that holds our entity id->type mapping
     */
    public static String TYPES_BY_UUID_MAP = "zzz_typesbyuuid_zzz";


    /**
     * Generate a standard edge name for our graph using the connection name. To be used only for searching.  DO NOT use
     * for creation.  Use the createConnectionEdge instead.
     *
     * @param connectionType The type of connection made
     */
    public static String getEdgeTypeFromConnectionType( String connectionType ) {
        return ( EDGE_CONN_SUFFIX + "|" + connectionType ).toLowerCase();
    }


    /**
     * Generate a standard edges from for a collection
     *
     * To be used only for searching DO NOT use for creation. Use the createCollectionEdge instead.
     */
    public static String getEdgeTypeFromCollectionName( String collectionName ) {
        return ( EDGE_COLL_SUFFIX + "|" + collectionName ).toLowerCase();
    }


    /**
     * Get the index scope for the edge from the source to the target.  The entity being indexed
     * is the target node
     */
    public static IndexEdge generateScopeFromSource( final Edge edge ) {
        return new IndexEdgeImpl( edge.getSourceNode(), edge.getType(), SearchEdge.NodeType.TARGET,
            edge.getTimestamp() );
    }


    /**
     * Get the index scope for the edge from the source.  The entity being indexed is the source node
     */
    public static IndexEdge generateScopeFromTarget( final Edge edge ) {
        return new IndexEdgeImpl( edge.getTargetNode(), edge.getType(), SearchEdge.NodeType.SOURCE,
            edge.getTimestamp() );
    }


    /**
     * Create the search edge from the source.  The nodes being searched are Target nodes on the edges
     */
    public static SearchEdge createSearchEdgeFromSource( final Edge edge ) {
        return new SearchEdgeImpl( edge.getSourceNode(), edge.getType(), SearchEdge.NodeType.TARGET );
    }


    /**
     * Create the search edge from the target.  The nodes being searched are source nodes on the edges
     */
    public static SearchEdge createSearchEdgeFromTarget( final Edge edge ) {
        return new SearchEdgeImpl( edge.getTargetNode(), edge.getType(), SearchEdge.NodeType.SOURCE );
    }


    /**
     * TODO move sourceId to ApplicationScope
     */
    public static Edge createCollectionEdge( final Id sourceId, final String collectionName, final Id entityId ) {
        final String edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( collectionName );

        final UUID entityIdUUID = entityId.getUuid();

        //if they don't use a time based uuid (such as in devices) we need to create a timestamp from "now" since
        // this is when the entity
        //will be added to the collection
        final UUID timeStampUuid = UUIDUtils.isTimeBased( entityIdUUID ) ? entityIdUUID : UUIDUtils.newTimeUUID();

        long uuidTimestamp = UUIDUtils.getUUIDLong( timeStampUuid );

        // create graph edge connection from head entity to member entity
        return new SimpleEdge( sourceId, edgeType, entityId, uuidTimestamp );
    }


    /**
     * Create a connection searchEdge
     */
    public static SearchEdge createCollectionSearchEdge( final Id sourceId, final String connectionType ) {
        return new SearchEdgeImpl( sourceId, getEdgeTypeFromCollectionName( connectionType ),
            SearchEdge.NodeType.TARGET );
    }


    /**
     * Create a new connection edge from the source node with the given connection type and target id
     */
    public static Edge createConnectionEdge( final Id sourceEntityId, final String connectionType,
                                             final Id targetEntityId ) {
        final String edgeType = getEdgeTypeFromConnectionType( connectionType );

        // create graph edge connection from head entity to member entity
        return new SimpleEdge( sourceEntityId, edgeType, targetEntityId, System.currentTimeMillis() );
    }


    /**
     * Create a connection searchEdge
     *
     * @param sourceId The source id in the connection
     * @param connectionType The type of the connection to create a search for
     */
    public static SearchEdge createConnectionSearchEdge( final Id sourceId, final String connectionType ) {
        return new SearchEdgeImpl( sourceId, getEdgeTypeFromConnectionType( connectionType ),
            SearchEdge.NodeType.TARGET );
    }


    /**
     * search for all versions of a connection between 2 entities in teh graph
     *
     * @param sourceId The source id of the edge
     * @param connectionType The connection type used in the edge
     * @param targetId The target id
     *
     * @return A search by edge command to search the graph
     */
    public static SearchByEdge createConnectionSearchByEdge( final Id sourceId, final String connectionType,
                                                             final Id targetId ) {

        final String edgeType = getEdgeTypeFromConnectionType( connectionType );

        return new SimpleSearchByEdge( sourceId, edgeType, targetId, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
            null );
    }


    /**
     * Get the application scope from the given uuid
     *
     * @param applicationId The applicationId
     */
    public static ApplicationScope getApplicationScope( UUID applicationId ) {

        // We can always generate a scope, it doesn't matter if  the application exists yet or not.
        return new ApplicationScopeImpl( generateApplicationId( applicationId ) );
    }


    /**
     * Generate an applicationId from the given UUID
     *
     * @param applicationId the applicationId
     */
    public static Id generateApplicationId( UUID applicationId ) {
        return new SimpleId( applicationId, Application.ENTITY_TYPE );
    }


    /**
     * Generate an application scope for the management application
     */
    public static Id getManagementApplicationId() {
        return generateApplicationId( MANAGEMENT_APPLICATION_ID );
    }


    /**
     * Get the map scope for the applicationId to store entity uuid to type mapping
     */
    public static MapScope getEntityTypeMapScope( final Id applicationId ) {
        return new MapScopeImpl( applicationId, CpNamingUtils.TYPES_BY_UUID_MAP );
    }


    /**
     * Generate either the collection name or connection name from the edgeName
     */
    public static String getNameFromEdgeType( final String edgeName ) {


        if ( isCollectionEdgeType( edgeName ) ) {
            return getCollectionName( edgeName ) ;
        }

        return getConnectionType( edgeName ) ;
    }


    private static boolean isCollectionEdgeType( String type ) {
        return type.startsWith( EDGE_COLL_SUFFIX );
    }


    private static String getConnectionType( String edgeType ) {
        String[] parts = edgeType.split( "\\|" );
        return parts[1];
    }


    private static String getCollectionName( String edgeType ) {
        String[] parts = edgeType.split( "\\|" );
        return parts[1];
    }


}
