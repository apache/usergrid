/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */


package org.apache.usergrid.corepersistence.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.model.collection.SchemaManager;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.EntityToMapConverter;
import org.apache.usergrid.persistence.model.entity.MapToEntityConverter;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.*;
import org.apache.usergrid.persistence.model.field.value.EntityObject;
import org.apache.usergrid.persistence.model.field.value.Location;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utilities for converting entities to/from maps suitable for Core Persistence.
 * Aware of unique properties via Schema.
 */
public class CpEntityMapUtils {


    private static final MapToEntityConverter mapConverter = new MapToEntityConverter();
    private static final EntityToMapConverter entityConverter = new EntityToMapConverter();

    /**
     * Convert a usergrid 1.0 entity into a usergrid 2.0 entity
     */
    public static org.apache.usergrid.persistence.model.entity.Entity entityToCpEntity(
        org.apache.usergrid.persistence.Entity entity, UUID importId ) {

        UUID uuid = importId != null ? importId : entity.getUuid();

        org.apache.usergrid.persistence.model.entity.Entity cpEntity =
            new org.apache.usergrid.persistence.model.entity.Entity( new SimpleId( uuid, entity.getType() ) );

        cpEntity = CpEntityMapUtils.fromMap( cpEntity, entity.getProperties(), entity.getType(), true );

        cpEntity = CpEntityMapUtils.fromMap( cpEntity, entity.getDynamicProperties(), entity.getType(), true );

        return cpEntity;
    }


    public static Entity fromMap( Map<String, Object> map, String entityType, boolean topLevel ) {
        return fromMap(null, map, entityType, topLevel);
    }

    public static Entity fromMap(
            Entity entity, Map<String, Object> map, String entityType, boolean topLevel ) {

        if ( entity == null ) {
            entity = new Entity();
        }

        SchemaManager schemaManager = Schema.getDefaultSchema();
        return mapConverter.fromMap(entity,map, schemaManager, entityType,topLevel);
    }




    /**
     * Convert Entity to Map, adding version_ug_field and a {name}_ug_analyzed field for each
     * StringField.
     */
    public static Map toMap(EntityObject entity) {

        return entityConverter.toMap(entity);

    }

}
