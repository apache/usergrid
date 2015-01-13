/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.List;

import com.netflix.astyanax.connectionpool.exceptions.SerializationException;
import com.netflix.astyanax.serializers.AbstractSerializer;


/**
 * Serializer that tests serializing field buggers
 */
public class FieldBufferSerializer extends AbstractSerializer<FieldBuffer> {


    private static final FieldBufferSerializer INSTANCE = new FieldBufferSerializer();

    private static final byte VERSION = 0;


    public static FieldBufferSerializer get() {
        return INSTANCE;
    }


    @Override
    public ByteBuffer toByteBuffer( final FieldBuffer obj ) {
        //create an output stream
        final List<ByteBuffer> fields = obj.getFields();
        final int fieldCount = fields.size();

        //we start with 9.  1 byte for version, 4 bytes for total length, 4 bytes for the field count
        int totalLength = 9;

        for ( ByteBuffer fieldData : fields ) {
            final int bufferLength = fieldData.remaining();
            totalLength += 4 + bufferLength;
        }

        //now we have our total length, allocate it.

        ByteBuffer buffer = ByteBuffer.allocate( totalLength );

        buffer.put( VERSION );
        buffer.putInt( totalLength );
        buffer.putInt( fieldCount );

        for ( ByteBuffer fieldData : fields ) {
            final int bufferLength = fieldData.limit();

            buffer.putInt( bufferLength );
            buffer.put( fieldData );
        }

        buffer.rewind();
        return buffer;
    }


    @Override
    public FieldBuffer fromByteBuffer( final ByteBuffer byteBuffer ) {

        final int totalSize = byteBuffer.remaining();
        final byte version = byteBuffer.get();


        //not what we expected, throw an ex
        if ( version != VERSION ) {
            throw new SerializationException(
                    "Unable to de-serialze. Expected version " + VERSION + " but was version " + version );
        }

        final int expectedTotalSize = byteBuffer.getInt();

        if ( totalSize != expectedTotalSize ) {
            throw new SerializationException(
                    "The total size we expected was different.  Stored total size is " + expectedTotalSize
                            + " but actual buffer size is " + totalSize );
        }


        final int numberFields = byteBuffer.getInt();

        final FieldBuffer buffer = new FieldBuffer( numberFields );


        for ( int i = 0; i < numberFields; i++ ) {
            final int bufferLength = byteBuffer.getInt();

            final int startPosition = byteBuffer.position();
            final int newLimit = startPosition + bufferLength;

            //now read in the length and add it to our fieldBuffer


            //we do this so we don't actually copy the underlying buff again. Instead we duplicate it
            //and change our start and limits, to ensure we only read the field data when parsing
            final ByteBuffer fieldData = byteBuffer.duplicate();

            //set the limit that's the end of the field on the duplicate so that we won't read beyond this value
            fieldData.limit( newLimit );

            //advance our own internal buffer so that we can read the next field
            byteBuffer.position(newLimit);



            buffer.add( fieldData );
        }


        return buffer;
    }
}
