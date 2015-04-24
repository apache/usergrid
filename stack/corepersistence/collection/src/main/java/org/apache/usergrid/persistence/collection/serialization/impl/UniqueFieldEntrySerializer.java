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
package org.apache.usergrid.persistence.collection.serialization.impl;


import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.usergrid.persistence.core.astyanax.ColumnTypes;
import org.apache.usergrid.persistence.core.astyanax.DynamicCompositeParserImpl;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.FieldTypeName;
import org.apache.usergrid.persistence.model.field.FloatField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;

import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.model.DynamicComposite;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;


/**
 * Serialize a unique field into a column name
 */
public class UniqueFieldEntrySerializer extends AbstractSerializer<UniqueFieldEntry> {


    private static final UUIDSerializer UUID_SERIALIZER = UUIDSerializer.get();
    private static final StringSerializer STRING_SERIALIZER = StringSerializer.get();
    private static final UniqueFieldEntrySerializer INSTANCE = new UniqueFieldEntrySerializer();




    @Override
    public ByteBuffer toByteBuffer( final UniqueFieldEntry value ) {


        final UUID version = value.getVersion();
        final Field<?> field = value.getField();

        final FieldTypeName fieldType = field.getTypeName();
        final String fieldValue = field.getValue().toString().toLowerCase();


        DynamicComposite composite = new DynamicComposite(  );

        //we want to sort ascending to descending by version
        composite.addComponent( version,  UUID_SERIALIZER, ColumnTypes.UUID_TYPE_REVERSED);
        composite.addComponent( field.getName(), STRING_SERIALIZER );
        composite.addComponent( fieldValue, STRING_SERIALIZER );
        composite.addComponent( fieldType.name() , STRING_SERIALIZER);

        return composite.serialize();
    }


    @Override
    public UniqueFieldEntry fromByteBuffer( final ByteBuffer byteBuffer ) {

        final CompositeParser composite = new DynamicCompositeParserImpl( byteBuffer );

        final UUID version = composite.readUUID();

        final String name = composite.readString();

        final String value = composite.readString();

        final String typeString = composite.readString();


        final FieldTypeName fieldType = FieldTypeName.valueOf( typeString );

        final Field<?> field;

        switch ( fieldType ) {
            case BOOLEAN:
                field = new BooleanField( name, Boolean.parseBoolean( value ) );
                break;
            case DOUBLE:
                field = new DoubleField( name, Double.parseDouble( value ) );
                break;
            case FLOAT:
               field = new FloatField( name, Float.parseFloat(  value ));
               break;
            case INTEGER:
                field = new IntegerField( name, Integer.parseInt( value ) );
                break;
            case LONG:
                field = new LongField( name, Long.parseLong( value ) );
                break;
            case STRING:
                field = new StringField( name, value );
                break;
            case UUID:
                field = new UUIDField( name, UUID.fromString( value ) );
                break;
            default:
                throw new RuntimeException( "Unknown unique field type" );
        }

        return new UniqueFieldEntry( version, field );
    }


    /**
     * Get the singleton serializer
     */
    public static UniqueFieldEntrySerializer get() {
        return INSTANCE;
    }
}
