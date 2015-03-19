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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.netflix.astyanax.serializers.ByteSerializer;
import com.netflix.astyanax.serializers.BytesArraySerializer;
import com.netflix.astyanax.serializers.IntegerSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;


/**
 * A parser for our field buffer
 */
public class FieldBufferParser {


    private static final IntegerSerializer INTEGER_SERIALIZER = IntegerSerializer.get();
    private static final BytesArraySerializer BYTES_ARRAY_SERIALIZER = BytesArraySerializer.get();
    private static final ByteSerializer BYTE_SERIALIZER = ByteSerializer.get();
    private static final UUIDSerializer UUID_SERIALIZER = UUIDSerializer.get();

    private final Iterator<ByteBuffer> fields;


    public FieldBufferParser( final FieldBuffer buffer ) {
        fields = buffer.getFields().iterator();
    }


    /**
     * Return the value as an integer
     */
    public int readInteger() {
        return INTEGER_SERIALIZER.fromByteBuffer( getNext() );
    }


    /**
     * Return the value as a byte array
     */
    public byte[] readBytes() {
        return BYTES_ARRAY_SERIALIZER.fromByteBuffer( getNext() );
    }


    /**
     * return the next vlaue as a byte
     */
    public byte readByte() {
        return BYTE_SERIALIZER.fromByteBuffer( getNext() );
    }


    /**
     * Return the next 16 bytes asa  UUID
     * @return
     */
    public UUID readUUID() {
        return UUID_SERIALIZER.fromByteBuffer( getNext() );
    }

    private ByteBuffer getNext() {
        if ( !fields.hasNext() ) {
            throw new NoSuchElementException( "No more elements to return" );
        }

        return fields.next();
    }


}
