package org.apache.usergrid.corepersistence;/*
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


import org.apache.usergrid.persistence.Schema;


/**
 * Utilises for constructing standard naming conventions for collections and connections
 */
public class CpNamingUtils {

    /**
     * Edge types for all types
     */
    static final String ALL_TYPES = "zzzalltypeszzz";

    /**
     * Edge types for collection suffix
     */
    static final String EDGE_COLL_SUFFIX = "zzzcollzzz";

    /**
     * Edge types for connection suffix
     */
    static final String EDGE_CONN_SUFFIX = "zzzconnzzz";


    static String getCollectionScopeNameFromEntityType( String type ) {
        String csn = EDGE_COLL_SUFFIX + Schema.defaultCollectionName( type );
        return csn.toLowerCase();
    }


    static String getCollectionScopeNameFromCollectionName( String name ) {
        String csn = EDGE_COLL_SUFFIX + name;
        return csn.toLowerCase();
    }


    static String getConnectionScopeName( String entityType, String connectionType ) {
        String csn = EDGE_CONN_SUFFIX + connectionType + entityType;
        return csn.toLowerCase();
    }


    static boolean isCollectionEdgeType( String type ) {
        return type.startsWith( EDGE_COLL_SUFFIX );
    }


    static boolean isConnectionEdgeType( String type ) {
        return type.startsWith( EDGE_CONN_SUFFIX );
    }


    static public String getConnectionType( String edgeType ) {
        String[] parts = edgeType.split( "\\|" );
        return parts[1];
    }


    static public String getCollectionName( String edgeType ) {
        String[] parts = edgeType.split( "\\|" );
        return parts[1];
    }


    static String getEdgeTypeFromConnectionType( String connectionType ) {

        if ( connectionType != null ) {
            String csn = EDGE_CONN_SUFFIX + "|" + connectionType;
            return csn;
        }

        return null;
    }


    static String getEdgeTypeFromCollectionName( String collectionName ) {

        if ( collectionName != null ) {
            String csn = EDGE_COLL_SUFFIX + "|" + collectionName;
            return csn;
        }


        return null;
    }
}
