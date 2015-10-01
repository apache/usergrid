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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.CandidateResult;
import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.common.base.Preconditions;


public class IndexingUtils {


    /**
     * Regular expression for uuids
     */
    public static final String UUID_REX =
        "([A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12})";

    public static final String TYPE_REX = "(.+)";


    private static final String APPID_NAME = "appId";

    private static final String ENTITY_NAME = "entityId";

    private static final String NODEID_NAME = "nodeId";

    private static final String VERSION_NAME = "version";
    private static final String EDGE_NAME = "edgeName";

    private static final String NODE_TYPE_NAME = "nodeType";
    private static final String ENTITY_TYPE_NAME = "entityType";


    //the document Id will have 9 groups
    private static final String DOCUMENT_ID_REGEX =
        "appId\\(" + UUID_REX + "," + TYPE_REX + "\\)\\.entityId\\(" + UUID_REX + "," + TYPE_REX + "\\)\\.version\\(" + UUID_REX
            + "\\)\\.nodeId\\(" + UUID_REX + "," + TYPE_REX + "\\)\\.edgeName\\(" + TYPE_REX + "\\)\\.nodeType\\(" + TYPE_REX + "\\)";


    private static final Pattern DOCUMENT_PATTERN = Pattern.compile( DOCUMENT_ID_REGEX );

    // These are not allowed in document type names: _ . , | #
    public static final String FIELD_SEPERATOR = ".";

    public static final String ID_SEPERATOR = ",";


    /**
     * Entity type in ES we put everything into
     */
    public static final String ES_ENTITY_TYPE = "entity";

    /**
     * Reserved UG fields in the document
     */
    public static final String APPLICATION_ID_FIELDNAME = "applicationId";

    public static final String ENTITY_ID_FIELDNAME = "entityId";

    public static final String ENTITY_SIZE_FIELDNAME = "entitySize";

    public static final String ENTITY_VERSION_FIELDNAME = "entityVersion";

    public static final String ENTITY_TYPE_FIELDNAME = "entityType";

    public static final String EDGE_NODE_ID_FIELDNAME = "nodeId";

    public static final String EDGE_NAME_FIELDNAME = "edgeName";

    public static final String EDGE_NODE_TYPE_FIELDNAME = "entityNodeType";

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
     *
     * TODO make this format more readable and parsable
     */
    public static String createContextName( final ApplicationScope applicationScope, final SearchEdge scope ) {
        StringBuilder sb = new StringBuilder();
        idString( sb, APPID_NAME, applicationScope.getApplication() );
        sb.append( FIELD_SEPERATOR );
        idString( sb, NODEID_NAME, scope.getNodeId() );
        sb.append( FIELD_SEPERATOR );
        appendField( sb, EDGE_NAME, scope.getEdgeName() );
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
        idString( sb, APPID_NAME, applicationScope.getApplication() );
        sb.append( FIELD_SEPERATOR );
        idString( sb, ENTITY_ID_FIELDNAME, entityId );
        sb.append( FIELD_SEPERATOR );
        appendField( sb, VERSION_NAME, version.toString() );
        sb.append( FIELD_SEPERATOR );
        idString( sb, NODEID_NAME, searchEdge.getNodeId() );
        sb.append( FIELD_SEPERATOR );
        appendField( sb, EDGE_NAME, searchEdge.getEdgeName() );
        sb.append( FIELD_SEPERATOR );
        appendField( sb, NODE_TYPE_NAME, searchEdge.getNodeType().name() );

        return sb.toString();
    }


    public static final String entityId( final Id id ) {
        return idString( ENTITY_NAME, id );
    }


    public static final String applicationId( final Id id ) {
        return idString( APPID_NAME, id );
    }


    public static final String nodeId( final Id id ) {
        return idString( NODEID_NAME, id );
    }


    /**
     * Construct and Id string with the specified type for the id provided.
     */
    private static final String idString( final String type, final Id id ) {
        final StringBuilder stringBuilder = new StringBuilder();

        idString( stringBuilder, type, id );

        return stringBuilder.toString();
    }


    /**
     * Append the id to the string
     */
    private static final void idString( final StringBuilder builder, final String type, final Id id ) {
        builder.append( type ).append( "(" ).append( id.getUuid() ).append( ID_SEPERATOR )
               .append( id.getType().toLowerCase() ).append( ")" );
    }


    /**
     * Append a field
     */
    private static void appendField( final StringBuilder builder, final String type, final String value ) {
        builder.append( type ).append( "(" ).append( value ).append( ")" );
    }


    /**
     * Parse the document id into a candidate result
     */
    public static CandidateResult parseIndexDocId( final String documentId ) {


        final Matcher matcher = DOCUMENT_PATTERN.matcher( documentId );

        Preconditions.checkArgument( matcher.matches(), "Pattern for document id did not match expected format" );
        Preconditions.checkArgument( matcher.groupCount() == 9, "9 groups expected in the pattern" );

        //Other fields can be parsed using groups.  The groups start at value 1, group 0 is the entire match
        final String entityUUID = matcher.group( 3 );
        final String entityType = matcher.group( 4 );

        final String versionUUID = matcher.group( 5 );


        Id entityId = new SimpleId( UUID.fromString( entityUUID ), entityType );

        return new CandidateResult( entityId, UUID.fromString( versionUUID ) );
    }


    /**
     * Get the entity type
     */
    public static String getType( ApplicationScope applicationScope, Id entityId ) {
        return getType( applicationScope, entityId.getType() );
    }


    public static String getType( ApplicationScope applicationScope, String type ) {

        StringBuilder sb = new StringBuilder();

        idString( sb, APPID_NAME, applicationScope.getApplication() );
        sb.append( FIELD_SEPERATOR );
        sb.append( ENTITY_TYPE_NAME).append("(" ).append( type ).append( ")" );
        return sb.toString();
    }
}
