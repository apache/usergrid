/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.cassandra.index;


import java.nio.ByteBuffer;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.UUIDSerializer;


/**
 * Converts a UUID to bytes
 */
public class UUIDStartToBytes implements StartToBytes<UUID> {

    public static final UUIDStartToBytes INSTANCE = new UUIDStartToBytes();

    private UUIDStartToBytes(){}

    private static final UUIDSerializer UUID_SERIALIZER = UUIDSerializer.get();

    @Override
    public ByteBuffer toBytes( final UUID toBytes ) {
        return UUID_SERIALIZER.toByteBuffer( toBytes );
    }
}
