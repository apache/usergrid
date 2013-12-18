package org.apache.usergrid.persistence.collection.serialization.impl;


import java.nio.ByteBuffer;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.astynax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.collection.astynax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.collection.astynax.ScopedRowKey;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
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
public class CollectionScopedRowKeySerializer<K> extends AbstractSerializer<ScopedRowKey<CollectionScope, K>> {


    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


    /**
     * The delegate serializer for the key
     */
    private final CompositeFieldSerializer<K> keySerializer;


    public CollectionScopedRowKeySerializer( final CompositeFieldSerializer<K> keySerializer ) {
        this.keySerializer = keySerializer;
    }


    @Override
    public ByteBuffer toByteBuffer( final ScopedRowKey<CollectionScope, K> scopedRowKey ) {

        final CompositeBuilder builder = Composites.newCompositeBuilder();

        //add the organization's id
        ID_SER.toComposite( builder, scopedRowKey.getScope().getOrganization() );

        //add the scope's owner id to the composite
        ID_SER.toComposite( builder, scopedRowKey.getScope().getOwner() );

        //add the scope's name
        builder.addString( scopedRowKey.getScope().getName() );

        //add the key type
        keySerializer.toComposite( builder, scopedRowKey.getKey() );

        return builder.build();
    }


    @Override
    public ScopedRowKey<CollectionScope, K> fromByteBuffer( final ByteBuffer byteBuffer ) {
        final CompositeParser parser = Composites.newCompositeParser( byteBuffer );

        //read back the id
        final Id orgId = ID_SER.fromComposite( parser );
        final Id scopeId = ID_SER.fromComposite( parser );
        final String scopeName = parser.readString();
        final K value = keySerializer.fromComposite( parser );

        return new ScopedRowKey<CollectionScope, K>( new CollectionScopeImpl( orgId, scopeId, scopeName ), value );
    }
}


