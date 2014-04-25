package org.apache.usergrid.persistence.collection.serialization.impl;


import org.junit.Test;

import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.model.Composites;

import static org.junit.Assert.assertEquals;


/** @author tnine */
public class IdRowSerializerTest {

    @Test
    public void testSerialization() {

        Id testId = new SimpleId( "test" );

        IdRowCompositeSerializer rowSerializer = IdRowCompositeSerializer.get();


        final CompositeBuilder builder = Composites.newCompositeBuilder();

        rowSerializer.toComposite( builder, testId );


        final CompositeParser parser = Composites.newCompositeParser( builder.build() );

        //now convert it back

        Id deserialized = rowSerializer.fromComposite( parser );

        assertEquals( "Serialization works correctly", testId, deserialized );
    }


}
