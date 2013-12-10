package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.astynax.fixes.CompositeBuilder;
import org.apache.usergrid.persistence.collection.astynax.fixes.CompositeParser;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;


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
        builder.addUUID( id.getUuid() );
        builder.addString( id.getType() );
    }


    @Override
    public Id fromComposite( final CompositeParser composite ) {
        final UUID uuid = composite.readUUID();
        final String type = composite.readString();

        return new SimpleId( uuid, type );
    }


    /** Get the singleton serializer */
    public static IdRowCompositeSerializer get() {
        return INSTANCE;
    }
}


