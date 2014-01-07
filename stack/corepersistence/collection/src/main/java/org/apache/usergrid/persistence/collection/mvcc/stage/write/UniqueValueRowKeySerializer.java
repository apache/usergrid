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
package org.apache.usergrid.persistence.collection.mvcc.stage.write;

import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.model.Composites;
import com.netflix.astyanax.serializers.AbstractSerializer;
import java.nio.ByteBuffer;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.collection.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

/**
 * Serializer for serializing a UniqueValue field to a composite key.
 */
public class UniqueValueRowKeySerializer extends AbstractSerializer<ScopedRowKey<CollectionScope, Field>> {

    private static final UniqueValueFieldSerializer FIELD_SER = UniqueValueFieldSerializer.get();

    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();

    @Override
    public ByteBuffer toByteBuffer( final ScopedRowKey<CollectionScope, Field> scopedRowKey ) {

        final CompositeBuilder builder = Composites.newCompositeBuilder();

        //add the organization's id
        ID_SER.toComposite( builder, scopedRowKey.getScope().getOrganization() );

        //add the scope's owner id to the composite
        ID_SER.toComposite( builder, scopedRowKey.getScope().getOwner() );

        //add the scope's name
        builder.addString( scopedRowKey.getScope().getName() );

        FIELD_SER.toComposite( builder, scopedRowKey.getKey() );
         
        return builder.build();
    }


    @Override
    public ScopedRowKey<CollectionScope, Field> fromByteBuffer( final ByteBuffer byteBuffer ) {
        final CompositeParser parser = Composites.newCompositeParser( byteBuffer );

        //read back the id
        final Id orgId = ID_SER.fromComposite( parser );
        final Id scopeId = ID_SER.fromComposite( parser );
        final String scopeName = parser.readString();
        final Field field = FIELD_SER.fromComposite( parser );

        return new ScopedRowKey<CollectionScope, Field>( 
                new CollectionScopeImpl( orgId, scopeId, scopeName ), field );
    }
}
