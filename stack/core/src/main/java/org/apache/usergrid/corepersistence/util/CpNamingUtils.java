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

import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;


/**
 * Utilises for constructing standard naming conventions for collections and connections
 */
public class CpNamingUtils {

    /**
         * TODO: Why do we have 3?  Can we merge this into a single management app?  It would make administration much
         * easier and cleaner on the ES side
         *
         */

    /**
     * Edge types for collection suffix
     */
    public static final String EDGE_COLL_SUFFIX = "zzzcollzzz";

    /**
     * Edge types for connection suffix
     */
    public static final String EDGE_CONN_SUFFIX = "zzzconnzzz";
    /** The System Application where we store app and org metadata */
    public static final UUID SYSTEM_APP_ID =
            UUID.fromString("b6768a08-b5d5-11e3-a495-10ddb1de66c3");
    /** App where we store management info */
    public static final  UUID MANAGEMENT_APPLICATION_ID =
            UUID.fromString("b6768a08-b5d5-11e3-a495-11ddb1de66c8");
    /** TODO Do we need this in two-dot-o? */
    public static final  UUID DEFAULT_APPLICATION_ID =
            UUID.fromString("b6768a08-b5d5-11e3-a495-11ddb1de66c9");
    /**
     * The app infos entity object type. This holds the app name, appId, and org name
     */
    public static final String APPINFOS = "appinfos";

    public static final String DELETED_APPINFOS = "deleted_appinfos";

    /**
     * The name of the map that holds our entity id->type mapping
     */
    public static String TYPES_BY_UUID_MAP = "zzz_typesbyuuid_zzz";


    /**
     * Generate a collection scope for a collection within the application's Id for the given type
     * @param applicationId The applicationId that owns this entity
     * @param type The type in the collection
     * @return The collectionScope
     */
    public static CollectionScope getCollectionScopeNameFromEntityType(final Id applicationId, final String type){
       return  new CollectionScopeImpl( applicationId, applicationId, getCollectionScopeNameFromEntityType( type ) );
    }

    /**
     * Get the collection name from the entity/id type
     * @param type
     * @return
     */
    public static String getCollectionScopeNameFromEntityType( String type ) {
        String csn = EDGE_COLL_SUFFIX + Schema.defaultCollectionName( type );
        return csn.toLowerCase();
    }


    public static String getCollectionScopeNameFromCollectionName( String name ) {
        String csn = EDGE_COLL_SUFFIX + name;
        return csn.toLowerCase();
    }


    public static String getConnectionScopeName( String connectionType ) {
        String csn = EDGE_CONN_SUFFIX + connectionType ;
        return csn.toLowerCase();
    }


    public static boolean isCollectionEdgeType( String type ) {
        return type.startsWith( EDGE_COLL_SUFFIX );
    }


    public static boolean isConnectionEdgeType( String type ) {
        return type.startsWith( EDGE_CONN_SUFFIX );
    }


    static public String  getConnectionType( String edgeType ) {
        String[] parts = edgeType.split( "\\|" );
        return parts[1];
    }


    static public String getCollectionName( String edgeType ) {
        String[] parts = edgeType.split( "\\|" );
        return parts[1];
    }


    public static String getEdgeTypeFromConnectionType( String connectionType ) {

        if ( connectionType != null ) {
            String csn = EDGE_CONN_SUFFIX + "|" + connectionType;
            return csn;
        }

        return null;
    }


    public static String getEdgeTypeFromCollectionName( String collectionName ) {

        if ( collectionName != null ) {
            String csn = EDGE_COLL_SUFFIX + "|" + collectionName;
            return csn;
        }


        return null;
    }


    /**
     * Get the application scope from the given uuid
     * @param applicationId The applicationId
     */
    public static ApplicationScope getApplicationScope( UUID applicationId ) {

        // We can always generate a scope, it doesn't matter if  the application exists yet or not.
        final ApplicationScopeImpl scope = new ApplicationScopeImpl( generateApplicationId( applicationId ) );

        return scope;
    }


    /**
     * Generate an applicationId from the given UUID
     * @param applicationId  the applicationId
     *
     */
    public static Id generateApplicationId( UUID applicationId ) {
        return new SimpleId( applicationId, Application.ENTITY_TYPE );
    }


    /**
     * Get the map scope for the applicationId to store entity uuid to type mapping
     *
     * @param applicationId
     * @return
     */
    public static MapScope getEntityTypeMapScope( final Id applicationId ){
        return new MapScopeImpl(applicationId, CpNamingUtils.TYPES_BY_UUID_MAP );
    }
}
