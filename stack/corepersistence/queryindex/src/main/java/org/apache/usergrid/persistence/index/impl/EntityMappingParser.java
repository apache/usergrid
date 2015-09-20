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

package org.apache.usergrid.persistence.index.impl;


import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.model.entity.EntityMap;


/**
 * Our parser that will parse our entity map data, and return a collection of all field objects
 *
 * TODO: Decide if we're really getting rid of the Entity field object.  If not, this can be much faster using a visitor
 * pattern on the Entity
 */
public class EntityMappingParser implements FieldParser {

    private static final Logger log = LoggerFactory.getLogger( EntityMappingParser.class );


    /**
     * Our stack for fields
     */
    private Stack<String> fieldStack = new Stack();

    /**
     * Keeps track fo our last field type.  Used for nested objects and nested collections
     */
    private Stack<Object> lastCollection = new Stack();

    /**
     * List of all field tuples to return
     */
    private List<EntityField> fields = new ArrayList<>();


    /**
     * Visit al the primitive values
     */
    private void visit( final String value ) {
        fields.add( EntityField.create( fieldStack.peek(), value.toLowerCase() ) );
    }



    /**
     * Visit al the primitive values
     */
    private void visit( final UUID value ) {
       visit(value.toString());
    }


    private void visit( final boolean value ) {
        fields.add( EntityField.create( fieldStack.peek(), value ) );
    }



    private void visit( final int value ) {
        fields.add( EntityField.create( fieldStack.peek(), value ) );
    }


    private void visit( final long value ) {
        fields.add( EntityField.create( fieldStack.peek(), value ) );
    }


    private void visit( final double value ) {
        fields.add( EntityField.create( fieldStack.peek(), value ) );
    }


    private void visit( final float value ) {
        fields.add( EntityField.create( fieldStack.peek(), value ) );
    }




    /**
     * Iterate over a collection
     */
    private void iterate( final Collection value ) {

        //we don't support indexing 2 dimensional arrays.  Short circuit with a warning so we can track operationally
        if(!lastCollection.isEmpty() && lastCollection.peek() instanceof Collection){
            log.warn( "Encountered 2 collections consecutively.  " +
                "N+1 dimensional arrays are unsupported, only arrays of depth 1 are supported" );
            return;
        }

        lastCollection.push( value );

        //fisit all the object element
        for ( final Object element : value ) {
            visitValue( element );
        }

        lastCollection.pop();
    }


    /**
     * visit a value
     */
    private void visitValue( final Object value ) {

        if ( value instanceof Map ) {
            //if it's a location, then create a location field.
            if ( EntityMap.isLocationField( (Map)value ) ) {
                Map<String,Object> map = ( Map ) value;
                Map<String,Object> location = new HashMap<>(2);
                //normalize location field to use lat/lon for es
                location.put("lat",map.get("latitude"));
                location.put("lon",map.get("longitude"));
                fields.add( EntityField.create( fieldStack.peek(), location) );
                return;
            }

            iterate( ( Map<String, ?> ) value );
        }

        //TODO figure out our nested array structure
        else if ( value instanceof Collection) {
            iterate( ( Collection ) value );
        }
        else {
            visitPrimitive( value );
        }
    }


    /**
     * Construct the correct primitive
     */
    private void visitPrimitive( final Object object ) {
        if ( object instanceof String ) {
            visit( ( String ) object );
            return;
        }

        if(object instanceof  UUID){
            visit((UUID) object);
            return;
        }

        if ( object instanceof Boolean ) {
            visit( ( Boolean ) object );
            return;
        }

        if ( object instanceof Integer ) {
            visit( ( Integer ) object );
            return;
        }

        if ( object instanceof Long ) {
            visit( ( Long ) object );
            return;
        }

        if ( object instanceof Float ) {
            visit( ( Float ) object );
            return;
        }

        if ( object instanceof Double ) {
            visit( ( Double ) object );
            return;
        }


    }


    /**
     * Iterate all entries in a map and map them
     * @param map
     */
    private void iterate( final Map<String, ?> map ) {

        for ( final Map.Entry<String, ?> jsonField : map.entrySet() ) {
            pushField( jsonField.getKey() );
            visitValue( jsonField.getValue() );
            popField();
        }
    }


    /**
     * Push a new fieldname on to the stack
     */
    private void pushField( final String fieldName ) {
        if ( fieldStack.isEmpty() ) {
            fieldStack.push( fieldName );
            return;
        }


        final String newFieldName = fieldStack.peek() + "." + fieldName;
        fieldStack.push( newFieldName );
    }


    /**
     * Pop a field name off the stack
     */
    private void popField() {
        fieldStack.pop();
    }


    /**
     * Parse the map field
     */
    public List<EntityField> parse( final Map<String, ?> map ) {
        iterate( map );

        return fields;
    }
}
