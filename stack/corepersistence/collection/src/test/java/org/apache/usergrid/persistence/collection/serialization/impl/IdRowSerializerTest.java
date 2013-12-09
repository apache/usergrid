package org.apache.usergrid.persistence.collection.serialization.impl;


import java.nio.ByteBuffer;

import org.junit.Test;

import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import static org.junit.Assert.assertEquals;


/** @author tnine */
public class IdRowSerializerTest {

    @Test
    public void testSerialization() {

        Id testId = new SimpleId( "test" );

        IdRowSerializer rowSerializer = IdRowSerializer.get();

        ByteBuffer serialized = rowSerializer.toByteBuffer( testId );

        //now convert it back

        Id deserialized = rowSerializer.fromByteBuffer( serialized );

        assertEquals( "Serialization works correctly", testId, deserialized );
    }
}
