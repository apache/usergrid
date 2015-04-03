/*
 *
 *  *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *  *
 *
 */

package org.apache.usergrid.persistence.index.impl;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.usergrid.persistence.model.entity.EntityMap;
import org.apache.usergrid.persistence.model.field.value.Location;


/**
 * Our parser that will parse our entity map data, and return a collection of all field objects
 *
 * TODO: Decide if we're really getting rid of the Entity field object.  If not, this can be much faster using a visitor
 * pattern on the Entity
 */
public class EntityMappingParser implements FieldParser {


    /**
     * Our stack for fields
     */
    private Stack<String> fieldStack = new Stack();

    private List<EntityField> fields = new ArrayList<>();


    private void visit( final String value ) {
        fields.add( EntityField.create( fieldStack.peek(), value ) );
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




    private void visit( final Map<String, ?> value ) {
        //it's a location field, index it as such
        if ( EntityMap.isLocationField( value ) ) {
//            fields.add( EntityField.create( fieldStack.peek(), value ) );
            return;
        }

        iterate( value );
    }


    /**
     * Iterate over a collection
     */
    private void iterate( final Collection value ) {

        //no op
        if ( value.size() == 0 ) {
            return;
        }

        //fisit all the object element
        for ( final Object element : value ) {
            visitValue( element );
        }
    }


    /**
     * visit a value
     */
    private void visitValue( final Object value ) {

        if ( isMap( value ) ) {
            //if it's a location, then create a location field.
            if ( EntityMap.isLocationField( (Map)value ) ) {
                fields.add( EntityField.create( fieldStack.peek(), ( Map ) value ) );
                return;
            }

            iterate( ( Map<String, ?> ) value );
        }

        //TODO figure out our nested array structure
        else if ( isCollection( value ) ) {
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





    private void iterate( final Map<String, ?> map ) {

        for ( final Map.Entry<String, ?> jsonField : map.entrySet() ) {
            pushField( jsonField.getKey() );
            visitValue( jsonField.getValue() );
            popField();
        }
    }


    /**
     * Return true if it's a map
     */
    private boolean isMap( final Object value ) {
        return value instanceof Map;
    }


    /**
     * Return true if it's a collection
     */
    private boolean isCollection( final Object value ) {
        return value instanceof Collection;
    }


    /**
     * Return true if this is a primitive (inverse of isMap and isCollection)
     */
    private boolean isPrimitive( final Object value ) {
        return !isMap( value ) && !isCollection( value );
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
