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
package org.apache.usergrid.persistence.core.astyanax;


import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.common.base.Preconditions;
import com.netflix.astyanax.model.DynamicComposite;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;


/**
 * Serializer for serializing ids into rows
 *
 * @author tnine
 */
public class IdColDynamicCompositeSerializer implements DynamicCompositeFieldSerializer<Id> {


    private static final StringSerializer STRING_SERIALIZER = StringSerializer.get();
    private static final UUIDSerializer UUID_SERIALIZER = UUIDSerializer.get();

    private static final IdColDynamicCompositeSerializer INSTANCE = 
            new IdColDynamicCompositeSerializer();


    private IdColDynamicCompositeSerializer() {

    }


    /**
     * Get the singleton serializer
     */
    public static IdColDynamicCompositeSerializer get() {
        return INSTANCE;
    }


    @Override
    public void toComposite( final DynamicComposite composite, final Id value ) {
        composite.addComponent( value.getUuid(), UUID_SERIALIZER );
        composite.addComponent( value.getType(), STRING_SERIALIZER );
    }


    @Override
    public Id fromComposite( final DynamicComposite composite, int startIndex ) {


        Preconditions.checkArgument( composite.size() >= startIndex+2, "Composite must contain a next element for uuid and type" );

        final UUID uuid = composite.get(startIndex, UUID_SERIALIZER );

        final String type = composite.get(startIndex +1, STRING_SERIALIZER);

        return new SimpleId(uuid, type );

    }



}


