/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.usergrid.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.usergrid.persistence.index.impl.EntityCollectionIndexImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.EntityObjectField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.ListField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author ApigeeCorporation
 */
public class EntityMapUtils {
   
    /**
     * Test of mapToEntity method, of class EntityUtils.
     */
    @Test
    public void testMapToEntityRoundTrip() throws IOException {

        InputStream is = this.getClass().getResourceAsStream( "/sample-large.json" );
        ObjectMapper mapper = new ObjectMapper();
        List<Object> contacts = mapper.readValue( is, new TypeReference<List<Object>>() {} );

        for ( Object o : contacts ) {

            Map<String, Object> map1 = (Map<String, Object>)o;

            // convert map to entity
            Entity entity1 = EntityMapUtils.mapToEntity( "testscope", map1 );

            // convert entity back to map
            Map map2 = EntityCollectionIndexImpl.entityToMap( entity1 );

            // the two maps should be the same except for the two new _ug_analyzed properties
            Map diff = Maps.difference( map1, map2 ).entriesDiffering();
            assertEquals( 2, diff.size() );
        }
    }

    
    public static Entity mapToEntity( String scope, Map<String, Object> item ) {
        return mapToEntity( scope, null, item );
    }


    public static Entity mapToEntity( String scope, Entity entity, Map<String, Object> map ) {

        if ( entity == null ) {
            entity = new Entity();
        }

        for ( String fieldName : map.keySet() ) {

            Object value = map.get( fieldName );

            if ( value instanceof String ) {
                entity.setField( new StringField(fieldName, (String)value ));

            } else if ( value instanceof Boolean ) {
                entity.setField( new BooleanField(fieldName, (Boolean)value ));
                        
            } else if ( value instanceof Integer ) {
                entity.setField( new IntegerField(fieldName, (Integer)value ));

            } else if ( value instanceof Double ) {
                entity.setField( new DoubleField(fieldName, (Double)value ));

            } else if ( value instanceof Long ) {
                entity.setField( new LongField(fieldName, (Long)value ));

            } else if ( value instanceof List) {
                entity.setField( listToListField( scope, fieldName, (List)value ));

            } else if ( value instanceof Map ) {
                entity.setField( new EntityObjectField( fieldName, 
                    mapToEntity( scope, (Map<String, Object>)value ))); // recursion

            } else {
                throw new RuntimeException("Unknown type " + value.getClass().getName());
            }
        }

        return entity;
    }

    
    private static ListField listToListField( String scope, String fieldName, List list ) {

        if (list.isEmpty()) {
            return new ListField( fieldName );
        }

        Object sample = list.get(0);

        if ( sample instanceof Map ) {
            return new ListField<Entity>( fieldName, processListForField( scope, list ));

        } else if ( sample instanceof List ) {
            return new ListField<List>( fieldName, processListForField( scope, list ));
            
        } else if ( sample instanceof String ) {
            return new ListField<String>( fieldName, (List<String>)list );
                    
        } else if ( sample instanceof Boolean ) {
            return new ListField<Boolean>( fieldName, (List<Boolean>)list );
                    
        } else if ( sample instanceof Integer ) {
            return new ListField<Integer>( fieldName, (List<Integer>)list );

        } else if ( sample instanceof Double ) {
            return new ListField<Double>( fieldName, (List<Double>)list );

        } else if ( sample instanceof Long ) {
            return new ListField<Long>( fieldName, (List<Long>)list );

        } else {
            throw new RuntimeException("Unknown type " + sample.getClass().getName());
        }
    }

    
    private static List processListForField( String scope, List list ) {
        if ( list.isEmpty() ) {
            return list;
        }
        Object sample = list.get(0);

        if ( sample instanceof Map ) {
            List<Entity> newList = new ArrayList<Entity>();
            for ( Map<String, Object> map : (List<Map<String, Object>>)list ) {
                newList.add( mapToEntity( scope, map ) );
            }
            return newList;

        } else if ( sample instanceof List ) {
            return processListForField( scope, list ); // recursion
            
        } else { 
            return list;
        } 
    }


}
