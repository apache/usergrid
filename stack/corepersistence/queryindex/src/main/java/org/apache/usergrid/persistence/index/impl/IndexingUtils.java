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
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;


public class IndexingUtils {


    public static final String STRING_PREFIX = "su_";
    public static final String ANALYZED_STRING_PREFIX = "sa_";
    public static final String GEO_PREFIX = "go_";
    public static final String NUMBER_PREFIX = "nu_";
    public static final String BOOLEAN_PREFIX = "bu_";

    public static final String ENTITYID_FIELDNAME = "zzz_entityid_zzz";

    public static final String DOC_ID_SEPARATOR = "|";
    public static final String DOC_ID_SEPARATOR_SPLITTER = "\\|";

    // These are not allowed in document type names: _ . , | #
    public static final String DOC_TYPE_SEPARATOR = "^";
    public static final String DOC_TYPE_SEPARATOR_SPLITTER = "\\^";

    public static final String INDEX_NAME_SEPARATOR = "^";


    /**
      * Create our sub scope.  This is the ownerUUID + type
      * @param scope
      * @return
      */
     public static String createCollectionScopeTypeName( IndexScope scope ) {
         StringBuilder sb = new StringBuilder();
         String sep = DOC_TYPE_SEPARATOR;
         sb.append( scope.getApplication().getUuid() ).append(sep);
         sb.append( scope.getApplication().getType() ).append(sep);
         sb.append( scope.getOwner().getUuid() ).append(sep);
         sb.append( scope.getOwner().getType() ).append(sep);
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
        String sep = INDEX_NAME_SEPARATOR;
        sb.append( prefix ).append(sep);
        sb.append( applicationScope.getApplication().getUuid() ).append(sep);
        sb.append( applicationScope.getApplication().getType() );
        return sb.toString();
    }



    /**
     * Create the index doc from the given entity
     * @param entity
     * @return
     */
    public static String createIndexDocId(Entity entity) {
        return createIndexDocId(entity.getId(), entity.getVersion());
    }


    /**
     * Create the doc Id. This is the entitie's type + uuid + version
     * @param entityId
     * @param version
     * @return
     */
    public static String createIndexDocId(Id entityId, UUID version) {
        StringBuilder sb = new StringBuilder();
        String sep = DOC_ID_SEPARATOR;
        sb.append( entityId.getUuid() ).append(sep);
        sb.append( entityId.getType() ).append(sep);
        sb.append( version.toString() );
        return sb.toString();
    }


}
