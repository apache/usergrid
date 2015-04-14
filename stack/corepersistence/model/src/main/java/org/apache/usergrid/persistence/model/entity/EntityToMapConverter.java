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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.usergrid.persistence.model.field.AbstractField;
import org.apache.usergrid.persistence.model.field.ArrayField;
import org.apache.usergrid.persistence.model.field.ByteArrayField;
import org.apache.usergrid.persistence.model.field.EntityObjectField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.ListField;
import org.apache.usergrid.persistence.model.field.LocationField;
import org.apache.usergrid.persistence.model.field.SetField;
import org.apache.usergrid.persistence.model.field.value.EntityObject;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * abstract conversion to Map<String,Object> form EntityObject
 */
public class EntityToMapConverter {
    public static final String LAT = "lat";
    public static final String LON = "lon";

    public static ObjectMapper objectMapper = new ObjectMapper();

    private final Stack<String> fieldName = new Stack();

    /**
     * Convert Entity to Map, adding version_ug_field and a {name}_ug_analyzed field for each StringField.
     */

    public EntityMap toMap( EntityObject entityObject ) {
        EntityMap map = null;
        if ( entityObject instanceof Entity ) {
            Entity entity = ( Entity ) entityObject;
            map = new EntityMap( entity.getId(), entity.getVersion() );
        }
        else {
            map = new EntityMap();
        }
        return toMap( entityObject, map );
    }


    public EntityMap toMap( EntityObject entity, EntityMap entityMap ) {

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

                byte[] serilizedObj = bf.getValue();
                Object o;
                try {
                    o = objectMapper.readValue( serilizedObj, bf.getClassinfo() );
                }
                catch ( IOException e ) {
                    throw new RuntimeException( "Can't deserialize object ", e );
                }
                entityMap.put( bf.getName(), o );
            }
            else {
                entityMap.put( field.getName(), field.getValue() );
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

            return toMap( ( EntityObject ) element );
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
}
