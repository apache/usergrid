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

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.*;
import org.apache.usergrid.persistence.model.field.value.EntityObject;

import java.util.*;

import static org.apache.usergrid.persistence.index.impl.IndexingUtils.*;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.STRING_PREFIX;

/**
 * Classy class class.
 */
public class EntityToMapConverter {
    /**
     * Set the entity as a map with the context
     *
     * @param entity The entity
     * @param context The context this entity appears in
     */
    public static Map convert(ApplicationScope applicationScope, final Entity entity, final String context ) {
        final Map entityMap = entityToMap( entity );

        //add the context for filtering later
        entityMap.put( ENTITY_CONTEXT_FIELDNAME, context );

        //but the fieldname we have to prefix because we use query equality to seek this later.
        // TODO see if we can make this more declarative
        entityMap.put( ENTITYID_ID_FIELDNAME, IndexingUtils.idString(entity.getId()).toLowerCase() );

        entityMap.put( APPLICATION_ID_FIELDNAME, idString(applicationScope.getApplication()) );


        return entityMap;
    }


    /**
     * Convert Entity to Map and Adding prefixes for types:
     * <pre>
     * su_ - String unanalyzed field
     * sa_ - String analyzed field
     * go_ - Location field nu_ - Number field
     * bu_ - Boolean field
     * </pre>
     */
    private static Map entityToMap( EntityObject entity ) {

        Map<String, Object> entityMap = new HashMap<String, Object>();

        for ( Object f : entity.getFields().toArray() ) {

            Field field = ( Field ) f;

            if ( f instanceof ListField) {
                List list = ( List ) field.getValue();
                entityMap.put(field.getName().toLowerCase(),
                        new ArrayList( processCollectionForMap( list ) ) );

                if ( !list.isEmpty() ) {
                    if ( list.get( 0 ) instanceof String ) {
                        entityMap.put( ANALYZED_STRING_PREFIX + field.getName().toLowerCase(),
                                new ArrayList( processCollectionForMap( list ) ) );
                    }
                }
            }
            else if ( f instanceof SetField) {
                Set set = ( Set ) field.getValue();
                entityMap.put( field.getName().toLowerCase(),
                        new ArrayList( processCollectionForMap( set ) ) );
            }
            else if ( f instanceof EntityObjectField) {
                EntityObject eo = ( EntityObject ) field.getValue();
                entityMap.put(  field.getName().toLowerCase(), entityToMap(eo) ); // recursion
            }
            else if ( f instanceof StringField ) {

                // index in lower case because Usergrid queries are case insensitive
                entityMap.put( ANALYZED_STRING_PREFIX + field.getName().toLowerCase(),
                        ( ( String ) field.getValue() ).toLowerCase() );
                entityMap.put( STRING_PREFIX + field.getName().toLowerCase(),
                        ( ( String ) field.getValue() ).toLowerCase() );
            }
            else if ( f instanceof LocationField ) {
                LocationField locField = ( LocationField ) f;
                Map<String, Object> locMap = new HashMap<String, Object>();

                // field names lat and lon trigger ElasticSearch geo location
                locMap.put( "lat", locField.getValue().getLatitude() );
                locMap.put( "lon", locField.getValue().getLongitude() );
                entityMap.put( GEO_PREFIX + field.getName().toLowerCase(), locMap );
            }
            else if( f instanceof DoubleField || f instanceof  FloatField){
                entityMap.put( DOUBLE_PREFIX + field.getName().toLowerCase(), field.getValue() );
            }
            else if( f instanceof LongField || f instanceof IntegerField){
                entityMap.put( LONG_PREFIX + field.getName().toLowerCase(), field.getValue() );
            }
            else if ( f instanceof BooleanField ) {

                entityMap.put( BOOLEAN_PREFIX + field.getName().toLowerCase(), field.getValue() );
            }
            else if ( f instanceof UUIDField ) {

                entityMap.put( STRING_PREFIX + field.getName().toLowerCase(),
                        field.getValue().toString().toLowerCase() );
            }
            else {
                entityMap.put( field.getName().toLowerCase(), field.getValue() );
            }
        }

        return entityMap;
    }


    private static Collection processCollectionForMap( final Collection c ) {
        if ( c.isEmpty() ) {
            return c;
        }
        List processed = new ArrayList();
        Object sample = c.iterator().next();

        if ( sample instanceof Entity ) {
            for ( Object o : c.toArray() ) {
                Entity e = ( Entity ) o;
                processed.add( entityToMap( e ) );
            }
        }
        else if ( sample instanceof List ) {
            for ( Object o : c.toArray() ) {
                List list = ( List ) o;
                processed.add( processCollectionForMap( list ) ); // recursion;
            }
        }
        else if ( sample instanceof Set ) {
            for ( Object o : c.toArray() ) {
                Set set = ( Set ) o;
                processed.add( processCollectionForMap( set ) ); // recursion;
            }
        }
        else {
            for ( Object o : c.toArray() ) {
                processed.add( o );
            }
        }
        return processed;
    }
}
