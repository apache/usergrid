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


import java.nio.ByteBuffer;

import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.core.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.scope.OrganizationScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;

import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.model.Composites;
import com.netflix.astyanax.serializers.AbstractSerializer;


/**
 * Serializer for serializing CollectionScope + any type into row keys
 *
 * @author tnine
 */
public class OrganizationScopedRowKeySerializer<K> extends AbstractSerializer<ScopedRowKey<OrganizationScope, K>> {


    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


    /**
     * The delegate serializer for the key
     */
    private final CompositeFieldSerializer<K> keySerializer;


    public OrganizationScopedRowKeySerializer( final CompositeFieldSerializer<K> keySerializer ) {
        this.keySerializer = keySerializer;
    }


    @Override
    public ByteBuffer toByteBuffer( final ScopedRowKey<OrganizationScope, K> scopedRowKey ) {

        final CompositeBuilder builder = Composites.newCompositeBuilder();

        //add the organization's id
        ID_SER.toComposite( builder, scopedRowKey.getScope().getOrganization() );

        //add the key type
        keySerializer.toComposite( builder, scopedRowKey.getKey() );

        return builder.build();
    }


    @Override
    public ScopedRowKey<OrganizationScope, K> fromByteBuffer( final ByteBuffer byteBuffer ) {
        final CompositeParser parser = Composites.newCompositeParser( byteBuffer );

        //read back the id
        final Id orgId = ID_SER.fromComposite( parser );

        final K value = keySerializer.fromComposite( parser );

        return new ScopedRowKey<OrganizationScope, K>( new OrganizationScopeImpl( orgId), value );
    }
}


