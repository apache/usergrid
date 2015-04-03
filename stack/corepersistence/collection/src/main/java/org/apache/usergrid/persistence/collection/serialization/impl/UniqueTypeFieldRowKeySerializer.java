/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.collection.serialization.impl;

import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import org.apache.usergrid.persistence.core.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.model.field.Field;

/**
 * Row key implementation for unique type fields.  Used by uniquevalueserializationstratv2
 */
public class UniqueTypeFieldRowKeySerializer  implements CompositeFieldSerializer<TypeField>{

    private static final UniqueTypeFieldRowKeySerializer INSTANCE = new UniqueTypeFieldRowKeySerializer();

    private final UniqueFieldRowKeySerializer innerSerializer;

    public UniqueTypeFieldRowKeySerializer(){
        innerSerializer = UniqueFieldRowKeySerializer.get();
    }
    @Override
    public void toComposite(CompositeBuilder builder, TypeField value) {
        builder.addString(value.getType());
        innerSerializer.toComposite(builder,value.getField());
    }

    @Override
    public TypeField fromComposite(CompositeParser composite) {
        String type = composite.readString();
        Field field = innerSerializer.fromComposite(composite);

        return new TypeField(type,field);
    }


    /**
     * Get the singleton serializer
     */
    public static UniqueTypeFieldRowKeySerializer get() {
        return INSTANCE;
    }
}
