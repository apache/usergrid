package org.apache.usergrid.persistence.collection.serialization.impl;


import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.StringSerializer;


/**
 * Serializer for serializing ids into rows
 * @author tnine */
public class IdRowSerializer extends AbstractSerializer<Id> {


    private static final IdRowSerializer INSTANCE = new IdRowSerializer();

    private static final StringSerializer SER = StringSerializer.get();

    //Number of characters in the UUID String
    private static final int UUID_STR_LENGTH = 36;

    private IdRowSerializer(){}

    @Override
    public ByteBuffer toByteBuffer( final Id obj ) {
        String value = new StringBuilder().append( obj.getUuid().toString() ).append(":").append( obj.getType() ).toString();

        return SER.toByteBuffer( value );
    }


    @Override
    public Id fromByteBuffer( final ByteBuffer byteBuffer ) {

        final String totalString = SER.fromByteBuffer( byteBuffer );

        final String uuid = totalString.substring( 0, UUID_STR_LENGTH );
        final String type = totalString.substring( UUID_STR_LENGTH+1 );

        return new SimpleId( UUID.fromString( uuid ), type );
    }



    /**
     * Get the singleton serializer
     * @return
     */
    public static IdRowSerializer get(){
        return INSTANCE;
    }
}


