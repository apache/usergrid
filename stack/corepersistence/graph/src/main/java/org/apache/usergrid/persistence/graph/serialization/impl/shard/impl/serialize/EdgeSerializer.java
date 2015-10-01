/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.serialize;


import java.nio.ByteBuffer;

import org.apache.usergrid.persistence.core.astyanax.IdColDynamicCompositeSerializer;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdge;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.netflix.astyanax.model.DynamicComposite;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.LongSerializer;


/**
 * Serializes to a source->target edge Note that we cannot set the edge type on de-serialization.  Only the target Id
 * and version.
 */
public class EdgeSerializer extends AbstractSerializer<DirectedEdge> {

    private static final IdColDynamicCompositeSerializer ID_COL_SERIALIZER = IdColDynamicCompositeSerializer.get();
    private static final LongSerializer LONG_SERIALIZER = LongSerializer.get();

    public static final EdgeSerializer INSTANCE = new EdgeSerializer();


    @Override
    public ByteBuffer toByteBuffer( final DirectedEdge edge ) {

        DynamicComposite composite = new DynamicComposite();

        composite.addComponent( edge.timestamp, LONG_SERIALIZER );

        ID_COL_SERIALIZER.toComposite( composite, edge.id );

        return composite.serialize();
    }


    @Override
    public DirectedEdge fromByteBuffer( final ByteBuffer byteBuffer ) {
        DynamicComposite composite = DynamicComposite.fromByteBuffer( byteBuffer );

        Preconditions.checkArgument( composite.size() == 3, "Composite should have 3 elements" );


        //return the version
        final long timestamp = composite.get( 0, LONG_SERIALIZER );


        //parse our id
        final Id id = ID_COL_SERIALIZER.fromComposite( composite, 1 );


        return new DirectedEdge( id, timestamp );
    }


    /**
     * Create a scan range that represents the timestamp of the edge
     * @param timestamp
     * @return
     */
    public ByteBuffer fromTimeRange( final long timestamp ) {
        DynamicComposite composite = new DynamicComposite();

        composite.addComponent( timestamp, LONG_SERIALIZER );


        return composite.serialize();
    }
}
