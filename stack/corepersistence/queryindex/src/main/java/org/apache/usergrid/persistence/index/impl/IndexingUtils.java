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

    public static final String ENTITYID_FIELDNAME = "entityId";

    public static final String DOC_ID_SEPARATOR = "|";
    public static final String DOC_ID_SEPARATOR_SPLITTER = "\\|";

    // These are not allowed in document type names: _ . , | #
    public static final String DOC_TYPE_SEPARATOR = "^";

    public static final String INDEX_NAME_SEPARATOR = "^";

    public static final String ENTITY_CONTEXT = "_context";

    /**
     * To be used when we want to search all types within a scope
     */
    public static final String ALL_TYPES = "ALL";


    /**
      * Create our sub scope.  This is the ownerUUID + type
      * @param scope
      * @return
      */
     public static String createContextName( IndexScope scope ) {
         StringBuilder sb = new StringBuilder();
         sb.append( scope.getOwner().getUuid() ).append(DOC_TYPE_SEPARATOR);
         sb.append( scope.getOwner().getType() ).append(DOC_TYPE_SEPARATOR);
         sb.append( scope.getName() );
         return sb.toString();
     }



    /**
     * Create the index name based on our prefix+appUUID+AppType
     * @param prefix
     * @param applicationScope
     * @return
     */
    public static String createIndexName(
            String prefix, ApplicationScope applicationScope) {
        StringBuilder sb = new StringBuilder();
        sb.append( prefix ).append(INDEX_NAME_SEPARATOR);
        sb.append( applicationScope.getApplication().getUuid() ).append(INDEX_NAME_SEPARATOR);
        sb.append( applicationScope.getApplication().getType() );
        return sb.toString();
    }



    /**
     * Create the index doc from the given entity
     * @param entity
     * @return
     */
    public static String createIndexDocId(final Entity entity, final String scopeType) {
        return createIndexDocId(entity.getId(), entity.getVersion(), scopeType);
    }


    /**
     * Create the doc Id. This is the entitie's type + uuid + version
     * @param entityId
     * @param version
     * @return
     */
    public static String createIndexDocId(final Id entityId, final UUID version, final String scopeType) {
        StringBuilder sb = new StringBuilder();
        sb.append( entityId.getUuid() ).append(DOC_ID_SEPARATOR);
        sb.append( entityId.getType() ).append(DOC_ID_SEPARATOR);
        sb.append( version.toString() ).append( DOC_ID_SEPARATOR );
        sb.append( scopeType);
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

                .startObject( type )

                    .startArray( "dynamic_templates" )

                        // any string with field name that starts with sa_ gets analyzed
                        .startObject()
                            .startObject( "template_1" )
                                .field( "match", ANALYZED_STRING_PREFIX + "*" )
                                .field( "match_mapping_type", "string" )
                                .startObject( "mapping" ).field( "type", "string" )
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

                //types for context direct string matching
                .startObject( "context_template" )
                        .field( "match", IndexingUtils.ENTITY_CONTEXT )
                        .field( "match_mapping_type", "string" )
                        .startObject( "mapping" )
                            .field( "type", "string" )
                            .field( "index", "not_analyzed" )
                        .endObject()
                    .endObject()
                .endObject()

                    .endArray()

                .endObject()

            .endObject();

        return builder;
    }

}
