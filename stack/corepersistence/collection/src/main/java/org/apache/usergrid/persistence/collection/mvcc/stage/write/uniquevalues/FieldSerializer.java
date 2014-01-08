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
package org.apache.usergrid.persistence.collection.mvcc.stage.write.uniquevalues;

import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import java.util.UUID;
import org.apache.usergrid.persistence.collection.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;

// TODO: replace with "real" serializer

/**
 * Serialize Field for use as part of row-key in Unique Values Column Family.
 */
public class FieldSerializer implements CompositeFieldSerializer<Field> {

    public enum FieldType {
        BOOLEAN_FIELD,
        DOUBLE_FIELD,
        INTEGER_FIELD,
        LONG_FIELD,
        STRING_FIELD,
        UUID_FIELD
    };

    private static final FieldSerializer INSTANCE = new FieldSerializer();

    private FieldSerializer() {}

    @Override
    public void toComposite( final CompositeBuilder builder, final Field field ) {

        builder.addString( field.getName() );

        // TODO: use the real field value serializer(s) here? Store hash instead?
        builder.addString( field.getValue().toString() );
         
        final String simpleName = field.getClass().getSimpleName();
        final String fieldType = simpleName.substring(0, simpleName.length() - "Field".length());

        builder.addString( fieldType );
    }

    @Override
    public Field fromComposite( final CompositeParser composite ) {

        final String name = composite.readString();
        final String value = composite.readString();

        final FieldType fieldType = FieldType.valueOf(composite.readString());

        switch (fieldType) {
            case DOUBLE_FIELD: 
                return new DoubleField(name, Double.parseDouble(value));
            case INTEGER_FIELD: 
                return new IntegerField(name, Integer.parseInt(value));
            case LONG_FIELD: 
                return new LongField(name, Long.parseLong(value));
            case STRING_FIELD: 
                return new StringField(name, value);
            case UUID_FIELD: 
                return new UUIDField(name, UUID.fromString(value));
            default:
                throw new RuntimeException("Unknown unique field type");
        }
    }


    /**
     * Get the singleton serializer
     */
    public static FieldSerializer get() {
        return INSTANCE;
    }
}
