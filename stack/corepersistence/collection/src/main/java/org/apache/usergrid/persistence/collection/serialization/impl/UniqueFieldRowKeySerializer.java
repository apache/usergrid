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
package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.UUID;

import org.apache.usergrid.persistence.core.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.FieldTypeName;
import org.apache.usergrid.persistence.model.field.FloatField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;

import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;

// TODO: replace with "real" serializer


/**
 * Serialize Field for use as part of row-key in Unique Values Column Family.
 */
public class UniqueFieldRowKeySerializer implements CompositeFieldSerializer<Field> {


    private static final UniqueFieldRowKeySerializer INSTANCE = new UniqueFieldRowKeySerializer();


    @Override
    public void toComposite( final CompositeBuilder builder, final Field field ) {


        final FieldTypeName fieldType = field.getTypeName();


        /**
         * Validation we only support a subset
         */
        switch ( fieldType ) {
            case BOOLEAN:
            case DOUBLE:
            case FLOAT:
            case INTEGER:
            case LONG:
            case STRING:
            case UUID:
                break;
            default:
                throw new RuntimeException(
                    String.format( "Type %s is not a supported type for unique values", fieldType ) );
        }


        builder.addString( fieldType.name() );

        builder.addString( field.getName() );

        //write all values as lowercase for normalization
        builder.addString( field.getValue().toString().toLowerCase() );
    }


    @Override
    public Field fromComposite( final CompositeParser composite ) {

        final String typeString = composite.readString();

        final String name = composite.readString();

        final String value = composite.readString();


        final FieldTypeName fieldType = FieldTypeName.valueOf( typeString );

        switch ( fieldType ) {
            case BOOLEAN:
                return new BooleanField( name, Boolean.parseBoolean( value ) );
            case DOUBLE:
                return new DoubleField( name, Double.parseDouble( value ) );
            case FLOAT:
                return new FloatField( name, Float.parseFloat( value ) );
            case INTEGER:
                return new IntegerField( name, Integer.parseInt( value ) );
            case LONG:
                return new LongField( name, Long.parseLong( value ) );
            case STRING:
                return new StringField( name, value );
            case UUID:
                return new UUIDField( name, UUID.fromString( value ) );
            default:
                throw new RuntimeException( "Unknown unique field type" );
        }
    }


    /**
     * Get the singleton serializer
     */
    public static UniqueFieldRowKeySerializer get() {
        return INSTANCE;
    }
}
