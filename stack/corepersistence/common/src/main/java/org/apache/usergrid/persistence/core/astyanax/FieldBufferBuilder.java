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


import java.util.UUID;

import com.netflix.astyanax.serializers.*;


/**
 * A builder pattern
 */
public class FieldBufferBuilder {


    private static final IntegerSerializer INTEGER_SERIALIZER = IntegerSerializer.get();
    private static final BytesArraySerializer BYTES_ARRAY_SERIALIZER = BytesArraySerializer.get();
    private static final ByteSerializer BYTE_SERIALIZER = ByteSerializer.get();
    private static final UUIDSerializer UUID_SERIALIZER = UUIDSerializer.get();
    private static final StringSerializer STRING_SERIALIZER = StringSerializer.get();


    private final FieldBuffer buffer;


    public FieldBufferBuilder( final int elementSize ) {
        buffer = new FieldBuffer( elementSize );
    }


    /**
     * Add the integer to the fields.
     * @param value
     * @return
     */
    public FieldBufferBuilder addInteger( final int value ) {
        buffer.add( INTEGER_SERIALIZER.toByteBuffer( value ) );
        return this;
    }


    /**
     * Add a UUID to the field buffer
     */
    public FieldBufferBuilder addUUID( final UUID value ) {
        buffer.add( UUID_SERIALIZER.toByteBuffer( value ) );
        return this;
    }

    /**
     * add a string to buffer
     * @param value
     * @return
     */
    public FieldBufferBuilder addString( final String value ) {
        buffer.add( STRING_SERIALIZER.toByteBuffer( value ) );
        return this;
    }



    /**
     * Add the byte array to the fieldbuilder
     * @param bytes
     * @return
     */
    public FieldBufferBuilder addBytes( final byte[] bytes ) {
        buffer.add( BYTES_ARRAY_SERIALIZER.toByteBuffer( bytes ) );
        return this;
    }


    /**
     * Add the bytes to the fieldBuilder
     * @param newByte
     * @return
     */
    public FieldBufferBuilder addByte( final byte newByte ) {
        buffer.add( BYTE_SERIALIZER.toByteBuffer( newByte ) );
        return this;
    }


    /**
     * Return the field buffer from the builder
     * @return
     */
    public FieldBuffer build(){
        return buffer;
    }
}
