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

import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;


/**
 * Serializer for serializing ids into rows
 *
 * @author tnine
 */
public class IdRowCompositeSerializer implements CompositeFieldSerializer<Id> {


    private static final IdRowCompositeSerializer INSTANCE = new IdRowCompositeSerializer();


    private IdRowCompositeSerializer() {}


    @Override
    public void toComposite( final CompositeBuilder builder, final Id id ) {
        // NOTE that this order is important.  Our default behavior is to order by 
        // Time UUID and NOT by type, so we want our UUID written BEFORE the string type
        builder.addUUID( id.getUuid() );
        builder.addString( id.getType() );
    }


    @Override
    public Id fromComposite( final CompositeParser composite ) {
        final UUID uuid = composite.readUUID();
        final String type = composite.readString();

        return new SimpleId( uuid, type );
    }



    /**
     * Get the singleton serializer
     */
    public static IdRowCompositeSerializer get() {
        return INSTANCE;
    }
}


