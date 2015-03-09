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
package org.apache.usergrid.persistence.model.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.usergrid.persistence.model.field.*;
import org.apache.usergrid.persistence.model.field.value.EntityObject;

import java.io.IOException;
import java.util.*;

/**
 * abstract conversion to Map<String,Object> form EntityObject
 */
public class EntityToMapConverter{
    public static ObjectMapper objectMapper = new ObjectMapper(  );
    /**
     * Convert Entity to Map, adding version_ug_field and a {name}_ug_analyzed field for each
     * StringField.
     */

    public EntityMap toMap(EntityObject entityObject) {
        EntityMap map = null;
        if(entityObject instanceof Entity){
            Entity entity = (Entity)entityObject;
            map =  new EntityMap(entity.getId(),entity.getVersion());
        }else{
            map = new EntityMap();
        }
        return toMap(entityObject,map);
    }


    public EntityMap toMap(EntityObject entity,EntityMap entityMap) {

        for (Object f : entity.getFields().toArray()) {
            Field field = (Field) f;

            if (f instanceof ListField || f instanceof ArrayField) {
                List list = (List) field.getValue();
                entityMap.put(field.getName(),
                        new ArrayList( processCollectionForMap(list)));

            } else if (f instanceof SetField) {
                Set set = (Set) field.getValue();
                entityMap.put(field.getName(),
                        new ArrayList( processCollectionForMap(set)));

            } else if (f instanceof EntityObjectField) {
                EntityObject eo = (EntityObject) field.getValue();
                entityMap.put( field.getName(), toMap(eo)); // recursion

            } else if (f instanceof StringField) {
                entityMap.put(field.getName(), ((String) field.getValue()));

            } else if (f instanceof LocationField) {
                LocationField locField = (LocationField) f;
                Map<String, Object> locMap = new HashMap<String, Object>();

                // field names lat and lon trigger ElasticSearch geo location
                locMap.put("lat", locField.getValue().getLatitude());
                locMap.put("lon", locField.getValue().getLongitude());
                entityMap.put( field.getName(), field.getValue());

            } else if (f instanceof ByteArrayField) {
                ByteArrayField bf = ( ByteArrayField ) f;

                byte[] serilizedObj =  bf.getValue();
                Object o;
                try {
                    o = objectMapper.readValue( serilizedObj, bf.getClassinfo() );
                }
                catch ( IOException e ) {
                    throw new RuntimeException( "Can't deserialize object ",e );
                }
                entityMap.put( bf.getName(), o );
            }
            else {
                entityMap.put( field.getName(), field.getValue());
            }
        }

        return entityMap;
    }
    private Collection processCollectionForMap(Collection c) {
        if (c.isEmpty()) {
            return c;
        }
        List processed = new ArrayList();
        Object sample = c.iterator().next();

        if (sample instanceof Entity) {
            for (Object o : c.toArray()) {
                Entity e = (Entity) o;
                processed.add(toMap(e));
            }

        } else if (sample instanceof List) {
            for (Object o : c.toArray()) {
                List list = (List) o;
                processed.add(processCollectionForMap(list)); // recursion;
            }

        } else if (sample instanceof Set) {
            for (Object o : c.toArray()) {
                Set set = (Set) o;
                processed.add(processCollectionForMap(set)); // recursion;
            }

        } else {
            for (Object o : c.toArray()) {
                processed.add(o);
            }
        }
        return processed;
    }
}
