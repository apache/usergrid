package org.apache.usergrid.persistence.collection.serialization.impl;


import java.nio.ByteBuffer;

import org.apache.usergrid.persistence.collection.astynax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.collection.astynax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.collection.astynax.ScopedRowKey;
import org.apache.usergrid.persistence.collection.astynax.fixes.CompositeBuilder;
import org.apache.usergrid.persistence.collection.astynax.fixes.CompositeParser;
import org.apache.usergrid.persistence.collection.astynax.fixes.Composites;
import org.apache.usergrid.persistence.collection.impl.ScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;

import com.netflix.astyanax.serializers.AbstractSerializer;


/**
 * Serializer for serializing Scope + any type into row keys
 *
 * @author tnine
 */
public class ScopedRowKeySerializer<K> extends AbstractSerializer<ScopedRowKey<K>> {


    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


    /** The delegate serializer for the key */
    private final CompositeFieldSerializer<K> keySerializer;


    public ScopedRowKeySerializer( final CompositeFieldSerializer<K> keySerializer ) {
        this.keySerializer = keySerializer;
    }


    @Override
    public ByteBuffer toByteBuffer( final ScopedRowKey<K> scopedRowKey ) {

        final CompositeBuilder builder = Composites.newCompositeBuilder();

        //add the scope's id to the composite
        ID_SER.toComposite( builder, scopedRowKey.getScope().getOwner() );

        //add the scope's name
        builder.addString( scopedRowKey.getScope().getName() );

        //add the key type
        keySerializer.toComposite( builder, scopedRowKey.getKey() );

        return builder.build();
    }


    @Override
    public ScopedRowKey<K> fromByteBuffer( final ByteBuffer byteBuffer ) {
        final CompositeParser parser = Composites.newCompositeParser( byteBuffer );

        //read back the id
        final Id scopeId = ID_SER.fromComposite( parser );
        final String scopeName = parser.readString();
        final K value = keySerializer.fromComposite( parser );

        return new ScopedRowKey<K>( new ScopeImpl( scopeId, scopeName ), value );
    }
}


