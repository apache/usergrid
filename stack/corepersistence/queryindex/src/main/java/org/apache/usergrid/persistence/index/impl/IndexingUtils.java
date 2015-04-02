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
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.elasticsearch.common.xcontent.XContentBuilder;

import org.apache.commons.lang.NotImplementedException;


public class IndexingUtils {

    public static final String STRING_PREFIX = "su_";
    public static final String ANALYZED_STRING_PREFIX = "sa_";
    public static final String GEO_PREFIX = "go_";
    public static final String LONG_PREFIX = "long_";
    public static final String DOUBLE_PREFIX = "long_";

    public static final String BOOLEAN_PREFIX = "bu_";
    public static final String ARRAY_PREFIX = "ar_";
    public static final String SET_PREFIX = "set_";
    public static final String EO_PREFIX = "eo_";

    public static final String SPLITTER = "\\__";

    // These are not allowed in document type names: _ . , | #
    public static final String SEPARATOR = "__";


    //
    // Reserved UG fields.
    //
    public static final String APPLICATION_ID_FIELDNAME = "ug_applicationId";

    public static final String ENTITY_CONTEXT_FIELDNAME = "ug_context";

    public static final String ENTITYID_ID_FIELDNAME = "ug_entityId";

    public static final String ENTITY_VERSION_FIELDNAME = "ug_entityVersion";

    public static final String DOC_VALUES_KEY = "doc_values";

    /**
      * Create our sub scope.  This is the ownerUUID + type
      * @param scope
      * @return
      */
     public static String createContextName(final ApplicationScope applicationScope, final SearchEdge scope ) {
         StringBuilder sb = new StringBuilder();
         idString(sb,applicationScope.getApplication());
         sb.append(SEPARATOR);
         idString(sb, scope.getNodeId());
         sb.append( SEPARATOR );
         sb.append( scope.getEdgeName() );
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
     * Create the index doc from the given entity
     * @param entity
     * @return
     */
    public static String createIndexDocId(final ApplicationScope applicationScope, final Entity entity, final String context) {
        return createIndexDocId(applicationScope, entity.getId(), entity.getVersion(), context);
    }


    /**
     * Create the doc Id. This is the entitie's type + uuid + version
     * @param entityId
     * @param version
     * @para context The context it's indexed in
     * @return
     */
    public static String createIndexDocId( final ApplicationScope applicationScope, final Id entityId, final UUID version, final String context) {

        StringBuilder sb = new StringBuilder();
        idString( applicationScope.getApplication() );
        sb.append( SEPARATOR );
        idString(sb, entityId);
        sb.append( SEPARATOR );
        sb.append( version.toString() ).append( SEPARATOR );
        sb.append( context);
        return sb.toString();
    }




    public static String getType(ApplicationScope applicationScope, Id entityId) {
        return getType(applicationScope,entityId.getType());
    }

    public static String getType(ApplicationScope applicationScope, String type) {
        return idString(applicationScope.getApplication()) + SEPARATOR + type;
    }
}
