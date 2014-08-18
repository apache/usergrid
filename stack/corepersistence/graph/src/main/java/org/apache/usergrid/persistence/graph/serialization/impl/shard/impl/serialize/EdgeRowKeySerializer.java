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


import org.apache.usergrid.persistence.core.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeRowKey;
import org.apache.usergrid.persistence.model.entity.Id;

import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;


/**
 * Class to perform serialization for row keys from edges
 */

public class EdgeRowKeySerializer implements CompositeFieldSerializer<EdgeRowKey> {

    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


    @Override
    public void toComposite( final CompositeBuilder builder, final EdgeRowKey key ) {

        //add the row id to the composite
        ID_SER.toComposite( builder, key.sourceId );
        builder.addString( key.edgeType );
        ID_SER.toComposite( builder, key.targetId );
        builder.addLong( key.shardId );
    }


    @Override
    public EdgeRowKey fromComposite( final CompositeParser composite ) {

        final Id sourceId = ID_SER.fromComposite( composite );
        final String edgeType = composite.readString();
        final Id targetId = ID_SER.fromComposite( composite );
        final long shard = composite.readLong();

        return new EdgeRowKey( sourceId, edgeType, targetId, shard );
    }


}
