package org.apache.usergrid.persistence.collection.serialization.impl;


import org.junit.Test;

import org.apache.usergrid.persistence.collection.astynax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.collection.astynax.fixes.CompositeBuilder;
import org.apache.usergrid.persistence.collection.astynax.fixes.CompositeParser;
import org.apache.usergrid.persistence.collection.astynax.fixes.Composites;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

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

    @Test
    public void compositeSerializer(){
        final CompositeBuilder builder = Composites.newCompositeBuilder();
        builder.addUUID( UUIDGenerator.newTimeUUID() );
        builder.addString( "test" );

        final CompositeParser parser = Composites.newCompositeParser( builder.build() );
    }
}
