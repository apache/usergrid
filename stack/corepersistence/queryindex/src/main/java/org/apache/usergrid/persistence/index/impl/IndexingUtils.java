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


import java.io.IOException;
import java.util.UUID;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexIdentifier;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.elasticsearch.common.xcontent.XContentBuilder;


public class IndexingUtils {

    public static final String STRING_PREFIX = "su_";
    public static final String ANALYZED_STRING_PREFIX = "sa_";
    public static final String GEO_PREFIX = "go_";
    public static final String NUMBER_PREFIX = "nu_";
    public static final String BOOLEAN_PREFIX = "bu_";

    public static final String SPLITTER = "\\__";

    // These are not allowed in document type names: _ . , | #
    public static final String SEPARATOR = "__";


    //
    // Reserved UG fields.
    //

    public static final String ENTITY_CONTEXT_FIELDNAME = "ug_context";

    public static final String ENTITYID_ID_FIELDNAME = "ug_entityId";

    public static final String ENTITY_VERSION_FIELDNAME = "ug_entityVersion";


    /**
      * Create our sub scope.  This is the ownerUUID + type
      * @param scope
      * @return
      */
     public static String createContextName( IndexScope scope ) {
         StringBuilder sb = new StringBuilder();
         idString(sb, scope.getOwner());
         sb.append( SEPARATOR );
         sb.append( scope.getName() );
         return sb.toString();
     }


    /**
     * Append the id to the string
     * @param builder
     * @param id
     */
    public static final void idString(final StringBuilder builder, final Id id){
        builder.append( id.getUuid() ).append( SEPARATOR )
                .append(id.getType());
    }


    /**
     * Turn the id into a string
     * @param id
     * @return
     */
    public static final String idString(final Id id){
        final StringBuilder sb = new StringBuilder(  );
        idString(sb, id);
        return sb.toString();
    }


    /**
     * Create the facilities to retrieve an index name and alias name
     * @param fig
     * @param applicationScope
     * @return
     */
    public static IndexIdentifier createIndexIdentifier(IndexFig fig, ApplicationScope applicationScope) {
        return new IndexIdentifier(fig,applicationScope);
    }





    /**
     * Create the index doc from the given entity
     * @param entity
     * @return
     */
    public static String createIndexDocId(final Entity entity, final String context) {
        return createIndexDocId(entity.getId(), entity.getVersion(), context);
    }


    /**
     * Create the doc Id. This is the entitie's type + uuid + version
     * @param entityId
     * @param version
     * @para context The context it's indexed in
     * @return
     */
    public static String createIndexDocId(final Id entityId, final UUID version, final String context) {
        StringBuilder sb = new StringBuilder();
        idString(sb, entityId);
        sb.append( SEPARATOR );
        sb.append( version.toString() ).append( SEPARATOR );
        sb.append( context);
        return sb.toString();
    }


    /**
     * Build mappings for data to be indexed. Setup String fields as not_analyzed and analyzed,
     * where the analyzed field is named {name}_ug_analyzed
     *
     * @param builder Add JSON object to this builder.
     * @param type ElasticSearch type of entity.
     *
     * @return Content builder with JSON for mapping.
     *
     * @throws java.io.IOException On JSON generation error.
     */
    public static XContentBuilder createDoubleStringIndexMapping(
            XContentBuilder builder, String type ) throws IOException {

        builder = builder

            .startObject()

                .startObject(type)
                    /**  add routing  "_routing":{ "required":false,  "path":"ug_entityId" **/
                     .startObject("_routing").field("required",true).field("path",ENTITYID_ID_FIELDNAME).endObject()
                     .startArray("dynamic_templates")
                        // we need most specific mappings first since it's a stop on match algorithm

                        .startObject()

                            .startObject( "entity_id_template" )
                                .field( "match", IndexingUtils.ENTITYID_ID_FIELDNAME )
                                    .field( "match_mapping_type", "string" )
                                            .startObject( "mapping" ).field( "type", "string" )
                                                .field( "index", "not_analyzed" )
                                            .endObject()
                                    .endObject()
                                .endObject()

                            .startObject()
                            .startObject( "entity_context_template" )
                                .field( "match", IndexingUtils.ENTITY_CONTEXT_FIELDNAME )
                                .field( "match_mapping_type", "string" )
                                    .startObject( "mapping" )
                                        .field( "type", "string" )
                                        .field( "index", "not_analyzed" ).endObject()
                                    .endObject()
                            .endObject()

                            .startObject()
                            .startObject( "entity_version_template" )
                                .field( "match", IndexingUtils.ENTITY_VERSION_FIELDNAME )
                                        .field( "match_mapping_type", "string" )
                                            .startObject( "mapping" ).field( "type", "long" )
                                            .endObject()
                                        .endObject()
                                    .endObject()

                            // any string with field name that starts with sa_ gets analyzed
                            .startObject()
                                .startObject( "template_1" )
                                    .field( "match", ANALYZED_STRING_PREFIX + "*" )
                                    .field( "match_mapping_type", "string" ).startObject( "mapping" )
                                    .field( "type", "string" )
                                    .field( "index", "analyzed" )
                                .endObject()
                            .endObject()

                        .endObject()

                        // all other strings are not analyzed
                        .startObject()
                            .startObject( "template_2" )
                                //todo, should be string prefix, remove 2 field mapping
                                .field( "match", "*" )
                                .field( "match_mapping_type", "string" )
                                .startObject( "mapping" )
                                    .field( "type", "string" )
                                        .field( "index", "not_analyzed" )
                                .endObject()
                            .endObject()
                        .endObject()

                        // fields names starting with go_ get geo-indexed
                        .startObject()
                            .startObject( "template_3" )
                                .field( "match", GEO_PREFIX + "location" )
                                    .startObject( "mapping" )
            .field( "type", "geo_point" )
                                    .endObject()
                            .endObject()
                        .endObject()

                    .endArray()

                .endObject()

            .endObject();

        return builder;
    }



}
