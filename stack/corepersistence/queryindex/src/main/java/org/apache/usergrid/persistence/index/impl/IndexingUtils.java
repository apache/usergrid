package org.apache.usergrid.persistence.index.impl;/*
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
import org.apache.usergrid.persistence.index.CandidateResult;
import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;


public class IndexingUtils {


    // These are not allowed in document type names: _ . , | #
    public static final String FIELD_SEPERATOR = "__";

    public static final String ID_SEPERATOR = "_";


    /**
     * Entity type in ES we put everything into
     */
    public static final String ES_ENTITY_TYPE = "entity";

    /**
     * Reserved UG fields in the document
     */
    public static final String APPLICATION_ID_FIELDNAME = "applicationId";

    public static final String ENTITY_ID_FIELDNAME = "entityId";

    public static final String ENTITY_VERSION_FIELDNAME = "entityVersion";

    public static final String ENTITY_TYPE_FIELDNAME = "entityType";

    public static final String EDGE_NODE_ID_FIELDNAME = "edgeNodeId";

    public static final String EDGE_NAME_FIELDNAME = "edgeName";

    public static final String EDGE_NODE_TYPE_FIELDNAME = "edgeType";

    public static final String EDGE_TIMESTAMP_FIELDNAME = "edgeTimestamp";

    public static final String EDGE_SEARCH_FIELDNAME = "edgeSearch";

    public static final String ENTITY_FIELDS = "fields";

    /**
     * Reserved field types in our document
     *
     * We use longs for ints, and doubles for floats to avoid runtime type conflicts
     */
    public static final String FIELD_NAME = "name";
    public static final String FIELD_BOOLEAN = "boolean";
    public static final String FIELD_LONG = "long";
    public static final String FIELD_DOUBLE = "double";
    public static final String FIELD_LOCATION = "location";
    public static final String FIELD_STRING = "string";
    public static final String FIELD_UUID = "uuid";


    /**
     * All search/sort values
     */
    public static final String FIELD_NAME_NESTED = ENTITY_FIELDS + "." + FIELD_NAME;
    public static final String FIELD_BOOLEAN_NESTED = ENTITY_FIELDS + "." + FIELD_BOOLEAN;
    public static final String FIELD_LONG_NESTED = ENTITY_FIELDS + "." + FIELD_LONG;
    public static final String FIELD_DOUBLE_NESTED = ENTITY_FIELDS + "." + FIELD_DOUBLE;
    public static final String FIELD_LOCATION_NESTED = ENTITY_FIELDS + "." + FIELD_LOCATION;
    public static final String FIELD_STRING_NESTED = ENTITY_FIELDS + "." + FIELD_STRING;
    public static final String FIELD_UUID_NESTED = ENTITY_FIELDS + "." + FIELD_UUID;
    public static final String FIELD_STRING_NESTED_UNANALYZED = FIELD_STRING_NESTED + ".exact";






    /**
     * Create our sub scope.  This is the ownerUUID + type
     */
    public static String createContextName( final ApplicationScope applicationScope, final SearchEdge scope ) {
        StringBuilder sb = new StringBuilder();
        idString( sb, applicationScope.getApplication() );
        sb.append( FIELD_SEPERATOR );
        idString( sb, scope.getNodeId() );
        sb.append( FIELD_SEPERATOR );
        sb.append( scope.getEdgeName() );
        sb.append( FIELD_SEPERATOR );
        sb.append( scope.getNodeType() );
        return sb.toString();
    }


    /**
     * Append the id to the string
     */
    public static final void idString( final StringBuilder builder, final Id id ) {
        builder.append( id.getUuid() ).append( ID_SEPERATOR ).append( id.getType().toLowerCase() );
    }


    /**
     * Turn the id into a string
     */
    public static final String idString( final Id id ) {
        final StringBuilder sb = new StringBuilder();
        idString( sb, id );
        return sb.toString();
    }


    /**
     * Create the index doc from the given entity
     */
    public static String createIndexDocId( final ApplicationScope applicationScope, final Entity entity,
                                           final IndexEdge indexEdge ) {
        return createIndexDocId( applicationScope, entity.getId(), entity.getVersion(), indexEdge );
    }


    /**
     * Create the doc Id. This is the entitie's type + uuid + version
     */
    public static String createIndexDocId( final ApplicationScope applicationScope, final Id entityId,
                                           final UUID version, final SearchEdge searchEdge ) {

        StringBuilder sb = new StringBuilder();
        idString( sb, applicationScope.getApplication() );
        sb.append( FIELD_SEPERATOR );
        idString( sb, entityId );
        sb.append( FIELD_SEPERATOR );
        sb.append( version.toString() );

        sb.append( FIELD_SEPERATOR );

        idString( searchEdge.getNodeId() );

        sb.append( FIELD_SEPERATOR );
        sb.append( searchEdge.getEdgeName() );
        sb.append( FIELD_SEPERATOR );
        sb.append( searchEdge.getNodeType() );

        return sb.toString();
    }


    /**
     * Parse the document id into a candidate result
     */
    public static CandidateResult parseIndexDocId( final String documentId ) {

        String[] idparts = documentId.split( FIELD_SEPERATOR );
        String entityIdString = idparts[1];
        String version = idparts[2];

        final String[] entityIdParts = entityIdString.split( ID_SEPERATOR );

        Id entityId = new SimpleId( UUID.fromString( entityIdParts[0] ), entityIdParts[1] );

        return new CandidateResult( entityId, UUID.fromString( version ) );
    }


    public static String getType( ApplicationScope applicationScope, Id entityId ) {
        return getType( applicationScope, entityId.getType() );
    }


    public static String getType( ApplicationScope applicationScope, String type ) {
        return idString( applicationScope.getApplication() ) + FIELD_SEPERATOR + type;
    }
}
