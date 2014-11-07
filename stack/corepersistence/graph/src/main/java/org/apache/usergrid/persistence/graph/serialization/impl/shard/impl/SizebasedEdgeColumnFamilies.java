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
package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import java.util.Arrays;
import java.util.Collection;

import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.DynamicCompositeType;

import org.apache.usergrid.persistence.core.astyanax.ColumnTypes;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdge;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeColumnFamilies;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeRowKey;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.RowKey;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.RowKeyType;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.serialize.EdgeRowKeySerializer;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.serialize.EdgeSerializer;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.serialize.RowSerializer;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.serialize.RowTypeSerializer;

import com.netflix.astyanax.serializers.LongSerializer;

import static org.apache.usergrid.persistence.core.astyanax.ColumnTypes.LONG_TYPE_REVERSED;
import static org.apache.usergrid.persistence.core.astyanax.ColumnTypes.UUID_TYPE_REVERSED;


/**
 * Implementation of size based column family
 */
public class SizebasedEdgeColumnFamilies implements EdgeColumnFamilies {


    //Row key with no type
    private static final RowSerializer ROW_SERIALIZER = new RowSerializer();

    //row key with target id type
    private static final RowTypeSerializer ROW_TYPE_SERIALIZER = new RowTypeSerializer();

    private static final EdgeRowKeySerializer EDGE_ROW_KEY_SERIALIZER = new EdgeRowKeySerializer();

    //Edge serializers
    private static final EdgeSerializer EDGE_SERIALIZER = new EdgeSerializer();

    private static final LongSerializer LONG_SERIALIZER = LongSerializer.get();

    private static final String EDGE_DYNAMIC_COMPOSITE_TYPE =
            //we purposefully associate lower case "l" and "u" with reversed types.  This way we can use
            //the default serialization in Astayanax, but get reverse order in cassandra
            DynamicCompositeType.class.getSimpleName() + "(s=>UTF8Type,l=>" + LONG_TYPE_REVERSED + ",u=>"
                    + UUID_TYPE_REVERSED + ")";


    //initialize the CF's from our implementation
    private static final MultiTennantColumnFamily<ScopedRowKey<RowKey>, DirectedEdge> SOURCE_NODE_EDGES =
            new MultiTennantColumnFamily<>( "Graph_Source_Node_Edges",
                    new ScopedRowKeySerializer<>( ROW_SERIALIZER ), EDGE_SERIALIZER );


    private static final MultiTennantColumnFamily<ScopedRowKey<RowKey>, DirectedEdge> TARGET_NODE_EDGES =
            new MultiTennantColumnFamily<>( "Graph_Target_Node_Edges",
                    new ScopedRowKeySerializer<>( ROW_SERIALIZER ), EDGE_SERIALIZER );


    private static final MultiTennantColumnFamily<ScopedRowKey<RowKeyType>, DirectedEdge> SOURCE_NODE_TARGET_TYPE =
            new MultiTennantColumnFamily<>( "Graph_Source_Node_Target_Type",
                    new ScopedRowKeySerializer<>( ROW_TYPE_SERIALIZER ), EDGE_SERIALIZER );


    /**
     * The edges that are to the target node with the source type.  The target node is the row key
     */
    private static final MultiTennantColumnFamily<ScopedRowKey<RowKeyType>, DirectedEdge> TARGET_NODE_SOURCE_TYPE =
            new MultiTennantColumnFamily<>( "Graph_Target_Node_Source_Type",
                    new ScopedRowKeySerializer<>( ROW_TYPE_SERIALIZER ), EDGE_SERIALIZER );


    private static final MultiTennantColumnFamily<ScopedRowKey<EdgeRowKey>, Long> EDGE_VERSIONS =
            new MultiTennantColumnFamily<>( "Graph_Edge_Versions",
                    new ScopedRowKeySerializer<>( EDGE_ROW_KEY_SERIALIZER ), LONG_SERIALIZER );


    @Override
    public MultiTennantColumnFamily<ScopedRowKey<RowKey>, DirectedEdge> getSourceNodeCfName() {
        return SOURCE_NODE_EDGES;
    }


    @Override
    public MultiTennantColumnFamily<ScopedRowKey<RowKey>, DirectedEdge> getTargetNodeCfName() {
        return TARGET_NODE_EDGES;
    }


    @Override
    public MultiTennantColumnFamily<ScopedRowKey<RowKeyType>, DirectedEdge> getSourceNodeTargetTypeCfName() {
        return SOURCE_NODE_TARGET_TYPE;
    }


    @Override
    public MultiTennantColumnFamily<ScopedRowKey<RowKeyType>, DirectedEdge> getTargetNodeSourceTypeCfName() {
        return TARGET_NODE_SOURCE_TYPE;
    }


    @Override
    public MultiTennantColumnFamily<ScopedRowKey<EdgeRowKey>, Long> getGraphEdgeVersions() {
        return EDGE_VERSIONS;
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Arrays
                .asList( graphCf( SOURCE_NODE_EDGES ), graphCf( TARGET_NODE_EDGES ), graphCf( SOURCE_NODE_TARGET_TYPE ),
                        graphCf( TARGET_NODE_SOURCE_TYPE ),
                        new MultiTennantColumnFamilyDefinition( EDGE_VERSIONS, BytesType.class.getSimpleName(),
                                ColumnTypes.LONG_TYPE_REVERSED, BytesType.class.getSimpleName(),
                                MultiTennantColumnFamilyDefinition.CacheOption.KEYS ) );
    }


    /**
     * Helper to generate an edge definition by the type
     */
    private MultiTennantColumnFamilyDefinition graphCf( MultiTennantColumnFamily cf ) {
        return new MultiTennantColumnFamilyDefinition( cf, BytesType.class.getSimpleName(), EDGE_DYNAMIC_COMPOSITE_TYPE,
                BytesType.class.getSimpleName(), MultiTennantColumnFamilyDefinition.CacheOption.KEYS );
    }
}
