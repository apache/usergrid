/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.index.impl;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.EntityMap;
import org.apache.usergrid.persistence.model.entity.Id;

import static org.apache.usergrid.persistence.index.impl.IndexingUtils.APPLICATION_ID_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.EDGE_NAME_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.EDGE_NODE_ID_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.EDGE_NODE_TYPE_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.EDGE_SEARCH_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.EDGE_TIMESTAMP_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.ENTITY_FIELDS;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.ENTITY_ID_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.ENTITY_SIZE_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.ENTITY_TYPE_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.ENTITY_VERSION_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.applicationId;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.entityId;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.getType;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.nodeId;


/**
 * Convert a CP entity to an elasticsearch document
 */
public class EntityToMapConverter {
    /**
     * Set the entity as a map with the context
     *
     * @param applicationScope
     * @param entity The entity
     * @param indexEdge The edge this entity is indexed on
     */
    public static Map<String, Object> convert(ApplicationScope applicationScope, final IndexEdge indexEdge, final Entity entity) {



        final Map<String, Object> outputEntity = new HashMap<>();


        final Id entityId = entity.getId();

        /***
         * Add our static fields for easier admin/debugging/reporting
         ****/

        outputEntity.put( ENTITY_ID_FIELDNAME, entityId( entityId ) );

        outputEntity.put( ENTITY_VERSION_FIELDNAME, entity.getVersion() );

        outputEntity.put( ENTITY_TYPE_FIELDNAME, getType( applicationScope, entityId));

        outputEntity.put( APPLICATION_ID_FIELDNAME, applicationId( applicationScope.getApplication() ) );

        outputEntity.put( EDGE_NODE_ID_FIELDNAME, nodeId( indexEdge.getNodeId() ) );

        outputEntity.put( EDGE_NODE_TYPE_FIELDNAME, indexEdge.getNodeType() );

        outputEntity.put( EDGE_NAME_FIELDNAME, indexEdge.getEdgeName()  );

        outputEntity.put( EDGE_TIMESTAMP_FIELDNAME, indexEdge.getTimestamp()  );

        outputEntity.put( ENTITY_SIZE_FIELDNAME, entity.getSize() );

        //add the context for filtering later
        outputEntity.put( EDGE_SEARCH_FIELDNAME, IndexingUtils.createContextName( applicationScope, indexEdge ) );

        //migrate the entity to map since we're ultimately going to use maps once we get rid of the Field objects
        final EntityMap entityMap = EntityMap.fromEntity( entity );

        //now visit our entity
        final FieldParser parser = new EntityMappingParser();

        final List<EntityField> fieldsToIndex =   parser.parse( entityMap );


        //add our fields
        outputEntity.put( ENTITY_FIELDS, fieldsToIndex );


        return outputEntity;
    }

}
