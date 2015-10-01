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


import java.nio.ByteBuffer;

import org.apache.usergrid.persistence.core.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.model.entity.Id;

import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.model.Composites;
import com.netflix.astyanax.serializers.AbstractSerializer;


/**
 * Serializer for serializing CollectionScope + any type into row keys
 */
public class CollectionScopedRowKeySerializer<K> 
    extends AbstractSerializer<ScopedRowKey<CollectionPrefixedKey<K>>> {

    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();

    /**
     * The delegate serializer for the key
     */
    private final CompositeFieldSerializer<K> keySerializer;


    public CollectionScopedRowKeySerializer( final CompositeFieldSerializer<K> ks ) {
        this.keySerializer = ks;
    }

    @Override
    public ByteBuffer toByteBuffer( final ScopedRowKey<CollectionPrefixedKey<K>> scopedRowKey ) {

        final CompositeBuilder builder = Composites.newCompositeBuilder();

        //add the organization's id
        ID_SER.toComposite( builder, scopedRowKey.getScope() );

        final CollectionPrefixedKey<K> key = scopedRowKey.getKey();

        //add the scope's owner id to the composite
        ID_SER.toComposite( builder, key.getOwner() );

        //add the scope's name
        builder.addString( key.getCollectionName() );

        //add the key type
        keySerializer.toComposite( builder, key.getSubKey() );

        //addOtherComponents( builder, scopedRowKey );

        return builder.build();
    }

    @Override
    public ScopedRowKey<CollectionPrefixedKey<K>> fromByteBuffer( final ByteBuffer byteBuffer ) {
        final CompositeParser parser = Composites.newCompositeParser( byteBuffer );

        //read back the id
        final Id orgId = ID_SER.fromComposite( parser );
        final Id scopeId = ID_SER.fromComposite( parser );
        final String scopeName = parser.readString();
        final K value = keySerializer.fromComposite( parser );


        final CollectionPrefixedKey<K> collectionPrefixedKey = new CollectionPrefixedKey<>( scopeName,  scopeId, value );


        return new ScopedRowKey<>( orgId, collectionPrefixedKey );
    }
}


