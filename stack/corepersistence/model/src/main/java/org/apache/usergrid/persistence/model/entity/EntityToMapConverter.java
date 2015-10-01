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


import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.apache.usergrid.persistence.model.field.*;
import org.apache.usergrid.persistence.model.field.value.EntityObject;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * abstract conversion to Map<String,Object> form EntityObject
 */
public class EntityToMapConverter {
    public static final String LAT = "latitude";
    public static final String LON = "longitude";
    private final JsonFactory jsonFactory = new JsonFactory();
    private final ObjectMapper objectMapper = new ObjectMapper(jsonFactory).registerModule(new GuavaModule());
    private static final Map<String,Boolean> corruptedTypes = getCorruptedTypes();

    /**
     * Convert Entity to Map, adding version_ug_field and a {name}_ug_analyzed field for each StringField.
     */



    /**
     * hacky impl, for outbound implementations longitude needs to be \ "longitude" and not "lon"
     * @param entityObject
     * @return
     */
    public EntityMap toMap( EntityObject entityObject ) {
        EntityMap map = new EntityMap();
        return toMap(entityObject, map);
    }

    private EntityMap toMap( EntityObject entity, EntityMap entityMap ) {
        for ( Field field : entity.getFields() ) {
            if ( field instanceof ListField || field instanceof ArrayField  || field instanceof SetField) {
                Collection list = ( Collection ) field.getValue();
                entityMap.put( field.getName(), processCollection( list )  );
            }
            else if ( field instanceof EntityObjectField ) {
                EntityObject eo = ( EntityObject ) field.getValue();
                entityMap.put( field.getName(), toMap( eo ) ); // recursion
            }
            else if ( field instanceof LocationField ) {
                LocationField locField = ( LocationField ) field;
                Map<String, Object> locMap = new HashMap<String, Object>();

                // field names lat and lon trigger ElasticSearch geo location
                locMap.put( LAT, locField.getValue().getLatitude() );
                locMap.put( LON, locField.getValue().getLongitude() );
                entityMap.put( field.getName(), locMap );
            }
            else if ( field instanceof ByteArrayField ) {

                ByteArrayField bf = ( ByteArrayField ) field;
                if( corruptedTypes.containsKey(bf.getClassinfo().getName()) ){
                    //do not deserialize this contains Query and Query has changed
                    continue;
                }
                byte[] serilizedObj = bf.getValue();
                Object o;
                try {
                    o = objectMapper.readValue( serilizedObj, bf.getClassinfo() );
                }
                catch ( IOException e ) {
                    throw new RuntimeException( "Can't deserialize object from field:"
                        + field.getName()+ " classinfo: " + bf.getClassinfo()
                        + " byteArray of length:" + serilizedObj.length
                        , e );
                }
                entityMap.put( bf.getName(), o );
            }else if (field instanceof SerializedObjectField) {
                SerializedObjectField bf = (SerializedObjectField) field;

                String serilizedObj = bf.getValue();
                Object o;
                try {
                    o = objectMapper.readValue(serilizedObj, bf.getClassinfo());
                } catch (IOException e) {
                    throw new RuntimeException("Can't deserialize object " + serilizedObj, e);
                }
                entityMap.put(bf.getName(), o);
            } else {
                entityMap.put(field.getName(), field.getValue());
            }
        }

        return entityMap;
    }

    /**
     * Process the collection for our map
     * @param c
     * @return
     */
    private List<?> processCollection( Collection c ) {
        if ( c.isEmpty() ) {
            return Collections.emptyList();
        }

        List processed = new ArrayList(c.size());


        for(final Object element: c){
            processed.add( processCollectionElement( element ) );
        }

        return processed;
    }


    /**
     * Process each instance of data in our collection
     * @param element
     * @return
     */
    private Object processCollectionElement( final Object element ) {
        if ( element instanceof EntityObject ) {

            return toMap((EntityObject) element);
        }

        //recurse into another list structure (2d + arrays)
        if (element instanceof ListField || element instanceof ArrayField  || element instanceof SetField){
            return processCollection( ( Collection ) ( ( AbstractField ) element ).getValue() );
        }

        if ( element instanceof List || element instanceof Set ) {

            return processCollection( ( Collection ) element ); // recursion;
        }

        return element;
    }

    public static Map<String,Boolean> getCorruptedTypes() {
        Map<String,Boolean> typeMap = new HashMap<>() ;
        typeMap.put("org.apache.usergrid.persistence.PathQuery",true);
        return typeMap;
    }
}
