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
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


/**
 * Tests the builder, parser, and serialization of the entities
 */
public class FieldBufferSerializerTest {

    /**
     * Perform a very simple serialization of the 3 field types
     */
    @Test
    public void simpleSerializer() {

        final byte setByte = 1;
        final int setInteger = 200;
        final byte[] setByteArray = generateByteArray( 1000 );

        FieldBufferBuilder builder = new FieldBufferBuilder( 3 );

        builder.addByte( setByte );
        builder.addInteger( setInteger );
        builder.addBytes( setByteArray );


        final FieldBufferSerializer serializer = new FieldBufferSerializer();

        final ByteBuffer serialized = serializer.toByteBuffer( builder.build() );


        final FieldBuffer parsed = serializer.fromByteBuffer( serialized );

        FieldBufferParser parser = new FieldBufferParser( parsed );

        final byte returnedByte = parser.readByte();

        assertEquals( "Bytes should be equal", setByte, returnedByte );

        final int returnedInt = parser.readInteger();

        assertEquals( "Integer should be equal", setInteger, returnedInt );

        final byte[] returnedByteArray = parser.readBytes();

        assertArrayEquals( "arrays should be equal", setByteArray, returnedByteArray );
    }


    @Test
    public void largerThanUnsignedShorts() {
        final int maxShortSize = 65535;

        final int lengthOfArray = maxShortSize + 1;

        final byte setByte = 2;
        final int setInteger = 400;


        final byte[] setByteArray = generateByteArray( lengthOfArray );

        FieldBufferBuilder builder = new FieldBufferBuilder( 1 );

        builder.addBytes( setByteArray );
        builder.addByte( setByte );
        builder.addInteger( setInteger );

        final FieldBufferSerializer serializer = new FieldBufferSerializer();

        final ByteBuffer serialized = serializer.toByteBuffer( builder.build() );


        final FieldBuffer parsed = serializer.fromByteBuffer( serialized );

        FieldBufferParser parser = new FieldBufferParser( parsed );

        final byte[] returnedArray = parser.readBytes();

        assertArrayEquals( setByteArray, returnedArray );

        final byte returnedByte = parser.readByte();

        assertEquals( "Bytes should be equal", setByte, returnedByte );

        final int returnedInt = parser.readInteger();

        assertEquals( "Integer should be equal", setInteger, returnedInt );
    }


    private byte[] generateByteArray( final int length ) {
        final byte[] data = new byte[length];

        //set it to something other than 0 so we can be sure we're allocating correctly on parse
        Arrays.fill( data, ( byte ) 1 );

        return data;
    }
}
