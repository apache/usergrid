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

package org.apache.usergrid.persistence.graph.serialization.impl;


import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.DynamicCompositeType;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.collection.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.collection.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.SearchIdType;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ColumnNameIterator;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.StringColumnParser;
import org.apache.usergrid.persistence.graph.serialization.util.EdgeUtils;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ByteBufferRange;
import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.query.RowQuery;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.util.RangeBuilder;


/**
 * Class to perform all edge metadata I/O
 */
@Singleton
public class EdgeMetadataSerializationImpl implements EdgeMetadataSerialization, Migration {

    private static final byte[] HOLDER = new byte[] { 0 };

    //TODO, make this a config property?
    private static final int PAGE_SIZE = 100;

    //row key serializers
    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();
    private static final OrganizationScopedRowKeySerializer<Id> ROW_KEY_SER =
            new OrganizationScopedRowKeySerializer<Id>( ID_SER );

    private static final StringSerializer STRING_SERIALIZER = StringSerializer.get();

    private static final EdgeTypeRowCompositeSerializer EDGE_SER = new EdgeTypeRowCompositeSerializer();
    private static final OrganizationScopedRowKeySerializer<EdgeIdTypeKey> EDGE_TYPE_ROW_KEY =
            new OrganizationScopedRowKeySerializer<EdgeIdTypeKey>( EDGE_SER );

    private static final StringColumnParser PARSER = StringColumnParser.get();


    private static final MultiTennantColumnFamily<OrganizationScope, Id, String> CF_TARGET_EDGE_TYPES =
            new MultiTennantColumnFamily<OrganizationScope, Id, String>( "Graph_Target_Edge_Types", ROW_KEY_SER,
                    STRING_SERIALIZER );

    private static final MultiTennantColumnFamily<OrganizationScope, Id, String> CF_SOURCE_EDGE_TYPES =
            new MultiTennantColumnFamily<OrganizationScope, Id, String>( "Graph_Source_Edge_Types", ROW_KEY_SER,
                    STRING_SERIALIZER );

    //all target id types for source edge type
    private static final MultiTennantColumnFamily<OrganizationScope, EdgeIdTypeKey, String> CF_TARGET_EDGE_ID_TYPES =
            new MultiTennantColumnFamily<OrganizationScope, EdgeIdTypeKey, String>( "Graph_Target_Edge_Id_Types",
                    EDGE_TYPE_ROW_KEY, STRING_SERIALIZER );


    //all source id types for target edge type
    private static final MultiTennantColumnFamily<OrganizationScope, EdgeIdTypeKey, String> CF_SOURCE_EDGE_ID_TYPES =
            new MultiTennantColumnFamily<OrganizationScope, EdgeIdTypeKey, String>( "Graph_Source_Edge_Id_Types",
                    EDGE_TYPE_ROW_KEY, STRING_SERIALIZER );


    protected final Keyspace keyspace;


    @Inject
    public EdgeMetadataSerializationImpl( final Keyspace keyspace ) {
        this.keyspace = keyspace;
    }


    @Override
    public MutationBatch writeEdge( final OrganizationScope scope, final Edge edge ) {

        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateEdge( edge );


        MutationBatch batch = keyspace.prepareMutationBatch();

        final Id source = edge.getSourceNode();
        final Id target = edge.getTargetNode();
        final String edgeType = edge.getType();
        final long timestamp = edge.getVersion().timestamp();


        //add source->target edge type to meta data
        final ScopedRowKey<OrganizationScope, Id> sourceKey = new ScopedRowKey<OrganizationScope, Id>( scope, source );

        batch.withRow( CF_TARGET_EDGE_TYPES, sourceKey ).setTimestamp( timestamp ).putColumn( edgeType, HOLDER );


        //write source->target edge type and id type to meta data
        final ScopedRowKey<OrganizationScope, EdgeIdTypeKey> sourceTypeKey =
                new ScopedRowKey<OrganizationScope, EdgeIdTypeKey>( scope, new EdgeIdTypeKey( source, edgeType ) );


        batch.withRow( CF_TARGET_EDGE_ID_TYPES, sourceTypeKey ).setTimestamp( timestamp )
             .putColumn( target.getType(), HOLDER );


        //write target<--source edge type meta data
        final ScopedRowKey<OrganizationScope, Id> targetKey = new ScopedRowKey<OrganizationScope, Id>( scope, target );


        batch.withRow( CF_SOURCE_EDGE_TYPES, targetKey ).setTimestamp( timestamp ).putColumn( edgeType, HOLDER );


        //write target<--source edge type and id type to meta data
        final ScopedRowKey<OrganizationScope, EdgeIdTypeKey> targetTypeKey =
                new ScopedRowKey<OrganizationScope, EdgeIdTypeKey>( scope, new EdgeIdTypeKey( target, edgeType ) );


        batch.withRow( CF_SOURCE_EDGE_ID_TYPES, targetTypeKey ).setTimestamp( timestamp )
             .putColumn( source.getType(), HOLDER );


        return batch;
    }


    @Override
    public MutationBatch removeTargetEdgeType( final OrganizationScope scope, final Edge edge ) {
        return removeEdgeType( scope, edge.getSourceNode(), edge.getType(), edge.getVersion(), CF_TARGET_EDGE_TYPES );
    }


    @Override
    public MutationBatch removeTargetIdType( final OrganizationScope scope, final Edge edge ) {
        return removeIdType( scope, edge.getSourceNode(), edge.getTargetNode(), edge.getType(), edge.getVersion(),
                CF_TARGET_EDGE_ID_TYPES );
    }


    @Override
    public MutationBatch removeSourceEdgeType( final OrganizationScope scope, final Edge edge ) {
        return removeEdgeType( scope, edge.getTargetNode(), edge.getType(), edge.getVersion(), CF_SOURCE_EDGE_TYPES );
    }


    /**
     * Remove the edge
     * @param scope The scope
     * @param rowKeyId The id to use in the row key
     * @param edgeType The edge type
     * @param version The version of the edge
     * @param cf The column family
     */
    private MutationBatch removeEdgeType( final OrganizationScope scope, final Id rowKeyId, final String edgeType,
                                          final UUID version,
                                          final MultiTennantColumnFamily<OrganizationScope, Id, String> cf ) {
        final long timestamp = version.timestamp();


        //write target<--source edge type meta data
        final ScopedRowKey<OrganizationScope, Id> targetKey =
                new ScopedRowKey<OrganizationScope, Id>( scope, rowKeyId );


        final MutationBatch batch = keyspace.prepareMutationBatch();


        batch.withRow( cf, targetKey ).setTimestamp( timestamp ).deleteColumn( edgeType );

        return batch;
    }


    @Override
    public MutationBatch removeSourceIdType( final OrganizationScope scope, final Edge edge ) {
        return removeIdType( scope, edge.getTargetNode(), edge.getSourceNode(), edge.getType(), edge.getVersion(),
                CF_SOURCE_EDGE_ID_TYPES );
    }


    /**
     * Remove the id type
     * @param scope The scope to use
     * @param rowId The id to use in the row key
     * @param colId The id type to use in the column
     * @param edgeType The edge type to use in the column
     * @param version The version to use on the column
     * @param cf The column family to use
     * @return A populated mutation with the remove operations
     */
    private MutationBatch removeIdType( final OrganizationScope scope, final Id rowId, final Id colId,
                                        final String edgeType, final UUID version,
                                        final MultiTennantColumnFamily<OrganizationScope, EdgeIdTypeKey, String> cf ) {

        MutationBatch batch = keyspace.prepareMutationBatch();


        final long timestamp = version.timestamp();


        //write target<--source edge type and id type to meta data
        final ScopedRowKey<OrganizationScope, EdgeIdTypeKey> targetTypeKey =
                new ScopedRowKey<OrganizationScope, EdgeIdTypeKey>( scope, new EdgeIdTypeKey( rowId, edgeType ) );


        batch.withRow( cf, targetTypeKey ).setTimestamp( timestamp ).deleteColumn( colId.getType() );

        return batch;
    }


    @Override
    public Iterator<String> getTargetEdgeTypes( final OrganizationScope scope, final SearchEdgeType search ) {
        return getEdgeTypes( scope, search, CF_TARGET_EDGE_TYPES );
    }


    @Override
    public Iterator<String> getTargetIdTypes( final OrganizationScope scope, final SearchIdType search ) {
        return getIdTypes( scope, search, CF_TARGET_EDGE_ID_TYPES );
    }


    @Override
    public Iterator<String> getSourceEdgeTypes( final OrganizationScope scope, final SearchEdgeType search ) {
        return getEdgeTypes( scope, search, CF_SOURCE_EDGE_TYPES );
    }


    /**
     * Get the edge types from the search criteria.
     *
     * @param scope The org scope
     * @param search The edge type search info
     * @param cf The column family to execute on
     */
    private Iterator<String> getEdgeTypes( final OrganizationScope scope, final SearchEdgeType search,
                                           final MultiTennantColumnFamily<OrganizationScope, Id, String> cf ) {
        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateSearchEdgeType( search );


        final ScopedRowKey<OrganizationScope, Id> sourceKey =
                new ScopedRowKey<OrganizationScope, Id>( scope, search.getNode() );


        //resume from the last if specified.  Also set the range
        final ByteBufferRange searchRange =
                new RangeBuilder().setLimit( PAGE_SIZE ).setStart( search.getLast().orNull() ).build();

        RowQuery<ScopedRowKey<OrganizationScope, Id>, String> query =
                keyspace.prepareQuery( cf ).getKey( sourceKey ).autoPaginate( true ).withColumnRange( searchRange );

        try {
            return new ColumnNameIterator<String, String>( query.execute().getResult().iterator(), PARSER );
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to connect to cassandra", e );
        }
    }


    @Override
    public Iterator<String> getSourceIdTypes( final OrganizationScope scope, final SearchIdType search ) {
        return getIdTypes( scope, search, CF_SOURCE_EDGE_ID_TYPES );
    }


    /**
     * Get the id types from the specified column family
     *
     * @param scope The organization scope to use
     * @param search The search criteria
     * @param cf The column family to search
     */
    public Iterator<String> getIdTypes( final OrganizationScope scope, final SearchIdType search,
                                        final MultiTennantColumnFamily<OrganizationScope, EdgeIdTypeKey, String> cf ) {
        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateSearchEdgeIdType( search );


        final ScopedRowKey<OrganizationScope, EdgeIdTypeKey> sourceTypeKey =
                new ScopedRowKey<OrganizationScope, EdgeIdTypeKey>( scope,
                        new EdgeIdTypeKey( search.getNode(), search.getEdgeType() ) );


        //resume from the last if specified.  Also set the range
        final ByteBufferRange searchRange =
                new RangeBuilder().setLimit( PAGE_SIZE ).setStart( search.getLast().orNull() ).build();

        RowQuery<ScopedRowKey<OrganizationScope, EdgeIdTypeKey>, String> query =
                keyspace.prepareQuery( cf ).getKey( sourceTypeKey ).autoPaginate( true ).withColumnRange( searchRange );

        try {
            return new ColumnNameIterator<String, String>( query.execute().getResult().iterator(), PARSER );
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to connect to cassandra", e );
        }
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Arrays.asList( graphCf( CF_TARGET_EDGE_TYPES ), graphCf( CF_SOURCE_EDGE_TYPES ),
                graphCf( CF_TARGET_EDGE_ID_TYPES ), graphCf( CF_SOURCE_EDGE_ID_TYPES ) );
    }


    /**
     * Helper to generate an edge definition by the type
     */
    private MultiTennantColumnFamilyDefinition graphCf( MultiTennantColumnFamily cf ) {
        return new MultiTennantColumnFamilyDefinition( cf, DynamicCompositeType.class.getSimpleName(),
                BytesType.class.getSimpleName(), BytesType.class.getSimpleName() );
    }


    /**
     * Inner class to serialize and edgeIdTypeKey
     */
    private static class EdgeTypeRowCompositeSerializer implements CompositeFieldSerializer<EdgeIdTypeKey> {


        private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


        @Override
        public void toComposite( final CompositeBuilder builder, final EdgeIdTypeKey value ) {
            ID_SER.toComposite( builder, value.node );

            builder.addString( value.edgeType );
        }


        @Override
        public EdgeIdTypeKey fromComposite( final CompositeParser composite ) {
            final Id id = ID_SER.fromComposite( composite );

            final String edgeType = composite.readString();

            return new EdgeIdTypeKey( id, edgeType );
        }
    }


    /**
     * Simple key object for I/O
     */
    private static class EdgeIdTypeKey {
        private final Id node;
        private final String edgeType;


        private EdgeIdTypeKey( final Id node, final String edgeType ) {
            this.node = node;
            this.edgeType = edgeType;
        }
    }
}
