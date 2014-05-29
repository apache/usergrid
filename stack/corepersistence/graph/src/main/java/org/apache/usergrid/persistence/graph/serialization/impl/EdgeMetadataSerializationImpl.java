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
import org.apache.cassandra.db.marshal.UTF8Type;

import org.apache.usergrid.persistence.core.astyanax.OrganizationScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.migration.Migration;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.SearchIdType;
import org.apache.usergrid.persistence.core.astyanax.CassandraConfig;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.core.astyanax.ColumnNameIterator;
import org.apache.usergrid.persistence.core.astyanax.StringColumnParser;
import org.apache.usergrid.persistence.graph.serialization.util.EdgeUtils;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
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


    //row key serializers
    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();
    private static final OrganizationScopedRowKeySerializer<Id> ROW_KEY_SER =
            new OrganizationScopedRowKeySerializer<Id>( ID_SER );

    private static final StringSerializer STRING_SERIALIZER = StringSerializer.get();

    private static final EdgeTypeRowCompositeSerializer EDGE_SER = new EdgeTypeRowCompositeSerializer();
    private static final OrganizationScopedRowKeySerializer<EdgeIdTypeKey> EDGE_TYPE_ROW_KEY =
            new OrganizationScopedRowKeySerializer<EdgeIdTypeKey>( EDGE_SER );

    private static final StringColumnParser PARSER = StringColumnParser.get();


    /**
     * CFs where the row key contains the source node id
     */
    private static final MultiTennantColumnFamily<ApplicationScope, Id, String> CF_SOURCE_EDGE_TYPES =
            new MultiTennantColumnFamily<ApplicationScope, Id, String>( "Graph_Source_Edge_Types", ROW_KEY_SER,
                    STRING_SERIALIZER );

    //all target id types for source edge type
    private static final MultiTennantColumnFamily<ApplicationScope, EdgeIdTypeKey, String> CF_SOURCE_EDGE_ID_TYPES =
            new MultiTennantColumnFamily<ApplicationScope, EdgeIdTypeKey, String>( "Graph_Source_Edge_Id_Types",
                    EDGE_TYPE_ROW_KEY, STRING_SERIALIZER );

    /**
     * CFs where the row key is the target node id
     */
    private static final MultiTennantColumnFamily<ApplicationScope, Id, String> CF_TARGET_EDGE_TYPES =
            new MultiTennantColumnFamily<ApplicationScope, Id, String>( "Graph_Target_Edge_Types", ROW_KEY_SER,
                    STRING_SERIALIZER );


    //all source id types for target edge type
    private static final MultiTennantColumnFamily<ApplicationScope, EdgeIdTypeKey, String> CF_TARGET_EDGE_ID_TYPES =
            new MultiTennantColumnFamily<ApplicationScope, EdgeIdTypeKey, String>( "Graph_Target_Edge_Id_Types",
                    EDGE_TYPE_ROW_KEY, STRING_SERIALIZER );


    protected final Keyspace keyspace;
    private final CassandraConfig cassandraConfig;
    private final GraphFig graphFig;


    @Inject
    public EdgeMetadataSerializationImpl( final Keyspace keyspace, final CassandraConfig cassandraConfig,
                                          final GraphFig graphFig ) {

        Preconditions.checkNotNull( "cassandraConfig is required", cassandraConfig );
        Preconditions.checkNotNull( "consistencyFig is required", graphFig );
        Preconditions.checkNotNull( "keyspace is required", keyspace );

        this.keyspace = keyspace;
        this.cassandraConfig = cassandraConfig;
        this.graphFig = graphFig;
    }


    @Override
    public MutationBatch writeEdge( final ApplicationScope scope, final Edge edge ) {

        ValidationUtils.validateApplicationScope( scope );
        EdgeUtils.validateEdge( edge );




        final Id source = edge.getSourceNode();
        final Id target = edge.getTargetNode();
        final String edgeType = edge.getType();
        final long timestamp = edge.getTimestamp();

        final MutationBatch batch = keyspace.prepareMutationBatch().withConsistencyLevel( cassandraConfig.getWriteCL() ).withTimestamp( timestamp );


        //add source->target edge type to meta data
        final ScopedRowKey<ApplicationScope, Id> sourceKey = new ScopedRowKey<ApplicationScope, Id>( scope, source );

        batch.withRow( CF_SOURCE_EDGE_TYPES, sourceKey ).putColumn( edgeType, HOLDER );


        //write source->target edge type and id type to meta data
        EdgeIdTypeKey tk = new EdgeIdTypeKey( source, edgeType );
        final ScopedRowKey<ApplicationScope, EdgeIdTypeKey> sourceTypeKey =
                new ScopedRowKey<ApplicationScope, EdgeIdTypeKey>( scope, tk );


        batch.withRow( CF_SOURCE_EDGE_ID_TYPES, sourceTypeKey )
             .putColumn( target.getType(), HOLDER );


        //write target<--source edge type meta data
        final ScopedRowKey<ApplicationScope, Id> targetKey = new ScopedRowKey<ApplicationScope, Id>( scope, target );


        batch.withRow( CF_TARGET_EDGE_TYPES, targetKey ).putColumn( edgeType, HOLDER );


        //write target<--source edge type and id type to meta data
        final ScopedRowKey<ApplicationScope, EdgeIdTypeKey> targetTypeKey =
                new ScopedRowKey<ApplicationScope, EdgeIdTypeKey>( scope, new EdgeIdTypeKey( target, edgeType ) );


        batch.withRow( CF_TARGET_EDGE_ID_TYPES, targetTypeKey )
             .putColumn( source.getType(), HOLDER );


        return batch;
    }


    @Override
    public MutationBatch removeEdgeTypeFromSource( final ApplicationScope scope, final Edge edge ) {
        return removeEdgeTypeFromSource( scope, edge.getSourceNode(), edge.getType(), edge.getTimestamp() );
    }


    @Override
    public MutationBatch removeEdgeTypeFromSource( final ApplicationScope scope, final Id sourceNode,
                                                   final String type, final long version ) {
        return removeEdgeType( scope, sourceNode, type, version, CF_SOURCE_EDGE_TYPES );
    }


    @Override
    public MutationBatch removeIdTypeFromSource( final ApplicationScope scope, final Edge edge ) {
        return removeIdTypeFromSource( scope, edge.getSourceNode(), edge.getType(), edge.getTargetNode().getType(),
                edge.getTimestamp() );
    }


    @Override
    public MutationBatch removeIdTypeFromSource( final ApplicationScope scope, final Id sourceNode, final String type,
                                                 final String idType, final long version ) {
        return removeIdType( scope, sourceNode, idType, type, version, CF_SOURCE_EDGE_ID_TYPES );
    }


    @Override
    public MutationBatch removeEdgeTypeToTarget( final ApplicationScope scope, final Edge edge ) {
        return removeEdgeTypeToTarget( scope, edge.getTargetNode(), edge.getType(), edge.getTimestamp() );
    }


    @Override
    public MutationBatch removeEdgeTypeToTarget( final ApplicationScope scope, final Id targetNode, final String type,
                                                 final long version ) {
        return removeEdgeType( scope, targetNode, type, version, CF_TARGET_EDGE_TYPES );
    }


    @Override
    public MutationBatch removeIdTypeToTarget( final ApplicationScope scope, final Edge edge ) {
        return removeIdTypeToTarget( scope, edge.getTargetNode(), edge.getType(), edge.getSourceNode().getType(),
                edge.getTimestamp() );
    }


    @Override
    public MutationBatch removeIdTypeToTarget( final ApplicationScope scope, final Id targetNode, final String type,
                                               final String idType, final long version ) {
        return removeIdType( scope, targetNode, idType, type, version, CF_TARGET_EDGE_ID_TYPES );
    }


    /**
     * Remove the edge
     *
     * @param scope The scope
     * @param rowKeyId The id to use in the row key
     * @param edgeType The edge type
     * @param version The version of the edge
     * @param cf The column family
     */
    private MutationBatch removeEdgeType( final ApplicationScope scope, final Id rowKeyId, final String edgeType,
                                          final long version,
                                          final MultiTennantColumnFamily<ApplicationScope, Id, String> cf ) {




        //write target<--source edge type meta data
        final ScopedRowKey<ApplicationScope, Id> rowKey = new ScopedRowKey<ApplicationScope, Id>( scope, rowKeyId );

        final MutationBatch batch = keyspace.prepareMutationBatch().withTimestamp( version );

        batch.withRow( cf, rowKey ).deleteColumn( edgeType );

        return batch;
    }


    /**
     * Remove the id type
     *
     * @param scope The scope to use
     * @param rowId The id to use in the row key
     * @param idType The id type to use in the column
     * @param edgeType The edge type to use in the column
     * @param version The version to use on the column
     * @param cf The column family to use
     *
     * @return A populated mutation with the remove operations
     */
    private MutationBatch removeIdType( final ApplicationScope scope, final Id rowId, final String idType,
                                        final String edgeType, final long version,
                                        final MultiTennantColumnFamily<ApplicationScope, EdgeIdTypeKey, String> cf ) {


          final   MutationBatch batch = keyspace.prepareMutationBatch().withTimestamp( version );



        //write target<--source edge type and id type to meta data
        final ScopedRowKey<ApplicationScope, EdgeIdTypeKey> rowKey =
                new ScopedRowKey<ApplicationScope, EdgeIdTypeKey>( scope, new EdgeIdTypeKey( rowId, edgeType ) );


        batch.withRow( cf, rowKey ).deleteColumn( idType );

        return batch;
    }


    @Override
    public Iterator<String> getEdgeTypesFromSource( final ApplicationScope scope, final SearchEdgeType search ) {
        return getEdgeTypes( scope, search, CF_SOURCE_EDGE_TYPES );
    }


    @Override
    public Iterator<String> getIdTypesFromSource( final ApplicationScope scope, final SearchIdType search ) {
        return getIdTypes( scope, search, CF_SOURCE_EDGE_ID_TYPES );
    }


    @Override
    public Iterator<String> getEdgeTypesToTarget( final ApplicationScope scope, final SearchEdgeType search ) {
        return getEdgeTypes( scope, search, CF_TARGET_EDGE_TYPES );
    }


    /**
     * Get the edge types from the search criteria.
     *
     * @param scope The org scope
     * @param search The edge type search info
     * @param cf The column family to execute on
     */
    private Iterator<String> getEdgeTypes( final ApplicationScope scope, final SearchEdgeType search,
                                           final MultiTennantColumnFamily<ApplicationScope, Id, String> cf ) {
        ValidationUtils.validateApplicationScope( scope );
        EdgeUtils.validateSearchEdgeType( search );


        final ScopedRowKey<ApplicationScope, Id> sourceKey =
                new ScopedRowKey<ApplicationScope, Id>( scope, search.getNode() );


        //resume from the last if specified.  Also set the range


        final RangeBuilder rangeBuilder =
                new RangeBuilder().setLimit( graphFig.getScanPageSize() ).setStart( search.getLast().or( "" ) );

        RowQuery<ScopedRowKey<ApplicationScope, Id>, String> query =
                keyspace.prepareQuery( cf ).getKey( sourceKey ).autoPaginate( true )
                        .withColumnRange( rangeBuilder.build() );

        return new ColumnNameIterator<String, String>( query, PARSER, search.getLast().isPresent());
    }


    @Override
    public Iterator<String> getIdTypesToTarget( final ApplicationScope scope, final SearchIdType search ) {
        return getIdTypes( scope, search, CF_TARGET_EDGE_ID_TYPES );
    }


    /**
     * Get the id types from the specified column family
     *
     * @param scope The organization scope to use
     * @param search The search criteria
     * @param cf The column family to search
     */
    public Iterator<String> getIdTypes( final ApplicationScope scope, final SearchIdType search,
                                        final MultiTennantColumnFamily<ApplicationScope, EdgeIdTypeKey, String> cf ) {
        ValidationUtils.validateApplicationScope( scope );
        EdgeUtils.validateSearchEdgeIdType( search );


        final ScopedRowKey<ApplicationScope, EdgeIdTypeKey> sourceTypeKey =
                new ScopedRowKey<ApplicationScope, EdgeIdTypeKey>( scope,
                        new EdgeIdTypeKey( search.getNode(), search.getEdgeType() ) );


        //resume from the last if specified.  Also set the range
        final ByteBufferRange searchRange =
                new RangeBuilder().setLimit( graphFig.getScanPageSize() ).setStart( search.getLast().or( "" ) )
                                  .build();

        RowQuery<ScopedRowKey<ApplicationScope, EdgeIdTypeKey>, String> query =
                keyspace.prepareQuery( cf ).getKey( sourceTypeKey ).autoPaginate( true ).withColumnRange( searchRange );


        return new ColumnNameIterator<String, String>( query, PARSER, search.getLast().isPresent());
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Arrays.asList( graphCf( CF_SOURCE_EDGE_TYPES ), graphCf( CF_TARGET_EDGE_TYPES ),
                graphCf( CF_SOURCE_EDGE_ID_TYPES ), graphCf( CF_TARGET_EDGE_ID_TYPES ) );
    }


    /**
     * Helper to generate an edge definition by the type
     */
    private MultiTennantColumnFamilyDefinition graphCf( MultiTennantColumnFamily cf ) {
        return new MultiTennantColumnFamilyDefinition( cf, BytesType.class.getSimpleName(),
                UTF8Type.class.getSimpleName(), BytesType.class.getSimpleName(),
                MultiTennantColumnFamilyDefinition.CacheOption.KEYS );
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
