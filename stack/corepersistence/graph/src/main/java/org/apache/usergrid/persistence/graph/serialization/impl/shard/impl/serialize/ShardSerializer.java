/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.serialize;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.netflix.astyanax.model.DynamicComposite;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.BooleanSerializer;
import com.netflix.astyanax.serializers.ByteSerializer;
import com.netflix.astyanax.serializers.LongSerializer;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdge;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;

import java.nio.ByteBuffer;


public class ShardSerializer extends AbstractSerializer<Shard> {

    private static final BooleanSerializer BOOLEAN_SERIALIZER = BooleanSerializer.get();
    private static final LongSerializer LONG_SERIALIZER = LongSerializer.get();
    private static final EdgeSerializer EDGE_SERIALIZER = EdgeSerializer.INSTANCE;
    private static final ByteSerializer BYTE_SERIALIZER = ByteSerializer.get();


    public static final ShardSerializer INSTANCE = new ShardSerializer();


    @Override
    public ByteBuffer toByteBuffer(final Shard shard ) {

        DynamicComposite composite = new DynamicComposite();

        composite.addComponent( (byte) 2 , BYTE_SERIALIZER);
        composite.addComponent( shard.getShardIndex(), LONG_SERIALIZER);
        composite.addComponent( shard.getCreatedTime(), LONG_SERIALIZER);

        if(shard.getShardEnd().isPresent()) {
            composite.addComponent(shard.getShardEnd().get(), EDGE_SERIALIZER);
        }else{
            composite.addComponent(null, EDGE_SERIALIZER);
        }

        composite.addComponent( shard.isCompacted(), BOOLEAN_SERIALIZER);

        return composite.serialize();
    }


    @Override
    public Shard fromByteBuffer( final ByteBuffer byteBuffer ) {

        DynamicComposite composite = DynamicComposite.fromByteBuffer( byteBuffer );
        Preconditions.checkArgument( composite.size() == 5, "Composite should 5 elements" );


        final byte version = composite.get(0, BYTE_SERIALIZER);
        final long shardIndex = composite.get( 1, LONG_SERIALIZER );
        final long shardCreated = composite.get( 2, LONG_SERIALIZER );
        final DirectedEdge shardEnd = composite.get( 3, EDGE_SERIALIZER);
        final boolean isCompacted = composite.get( 4, BOOLEAN_SERIALIZER);


        final Shard shard = new Shard(shardIndex, shardCreated, isCompacted);
        shard.setShardEnd(Optional.fromNullable(shardEnd));
        return shard;

    }

}
