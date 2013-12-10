package org.apache.usergrid.persistence.collection.serialization.impl;


import java.nio.ByteBuffer;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.Scope;
import org.apache.usergrid.persistence.collection.astynax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.collection.astynax.ScopedRowKey;
import org.apache.usergrid.persistence.collection.impl.ScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import static org.junit.Assert.assertEquals;


/** @author tnine */
public class ScopedRowKeySerializerTest {

    @Test
    public void testSerialization() {

        final Id testId = new SimpleId( "scopeType" );
        final String name = "scopeName";
        final Id testKey = new SimpleId( "testKey" );

        final Scope scope = new ScopeImpl( testId, name );
        final ScopedRowKey<Id> rowKey = new ScopedRowKey<Id>( scope, testKey );


        ScopedRowKeySerializer<Id> scopedRowKeySerializer = new ScopedRowKeySerializer<Id>( IdRowCompositeSerializer
                .get() );


        ByteBuffer buff = scopedRowKeySerializer.toByteBuffer( rowKey );


        ScopedRowKey<Id> parsedRowKey = scopedRowKeySerializer.fromByteBuffer( buff );

        assertEquals("Row key serialized correctly", rowKey, parsedRowKey);

    }
}
