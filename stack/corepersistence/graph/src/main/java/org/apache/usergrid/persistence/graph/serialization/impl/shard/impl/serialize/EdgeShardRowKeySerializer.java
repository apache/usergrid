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


import org.apache.usergrid.persistence.core.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeType;
import org.apache.usergrid.persistence.model.entity.Id;

import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;


public class EdgeShardRowKeySerializer implements CompositeFieldSerializer<DirectedEdgeMeta> {

    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();

    public static final EdgeShardRowKeySerializer INSTANCE = new EdgeShardRowKeySerializer();


    @Override
    public void toComposite( final CompositeBuilder builder, final DirectedEdgeMeta meta ) {


        final DirectedEdgeMeta.NodeMeta[] nodeMeta = meta.getNodes();

        //add the stored value
        builder.addInteger( meta.getType().getStorageValue() );

        final int length = nodeMeta.length;

        builder.addInteger( length );


        for ( DirectedEdgeMeta.NodeMeta node : nodeMeta ) {
            ID_SER.toComposite( builder, node.getId() );
            builder.addInteger( node.getNodeType().getStorageValue() );
        }

        final String[] edgeTypes = meta.getTypes();

        builder.addInteger( edgeTypes.length );

        for ( String type : edgeTypes ) {
            builder.addString( type );
        }
    }


    @Override
    public DirectedEdgeMeta fromComposite( final CompositeParser composite ) {


        final int storageType = composite.readInteger();

        final DirectedEdgeMeta.MetaType metaType = DirectedEdgeMeta.MetaType.fromStorage( storageType );

        final int idLength = composite.readInteger();

        final DirectedEdgeMeta.NodeMeta[] nodePairs = new DirectedEdgeMeta.NodeMeta[idLength];


        for ( int i = 0; i < idLength; i++ ) {
            final Id sourceId = ID_SER.fromComposite( composite );

            final NodeType type = NodeType.get( composite.readInteger() );

            nodePairs[i] = new DirectedEdgeMeta.NodeMeta( sourceId, type );
        }


        final int length = composite.readInteger();

        String[] types = new String[length];

        for ( int i = 0; i < length; i++ ) {
            types[i] = composite.readString();
        }

        return  DirectedEdgeMeta.fromStorage( metaType, nodePairs, types );
    }


}
