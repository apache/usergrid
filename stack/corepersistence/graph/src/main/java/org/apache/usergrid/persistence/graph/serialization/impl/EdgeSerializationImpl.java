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


import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cassandra.db.marshal.BytesType;

import org.apache.usergrid.persistence.astyanax.IdColDynamicCompositeSerializer;
import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.collection.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.collection.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.collection.cassandra.ColumnTypes;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.impl.SimpleMarkedEdge;
import org.apache.usergrid.persistence.graph.serialization.CassandraConfig;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ColumnNameIterator;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ColumnParser;
import org.apache.usergrid.persistence.graph.serialization.util.EdgeHasher;
import org.apache.usergrid.persistence.graph.serialization.util.EdgeUtils;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.AbstractComposite;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.model.DynamicComposite;
import com.netflix.astyanax.query.RowQuery;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;
import com.netflix.astyanax.util.RangeBuilder;


/**
 *
 *
 */
@Singleton
public class EdgeSerializationImpl implements EdgeSerialization, Migration {


    //Row key with no type
    private static final RowSerializer ROW_SERIALIZER = new RowSerializer();

    //row key with target id type
    private static final RowTypeSerializer ROW_TYPE_SERIALIZER = new RowTypeSerializer();

    private static final EdgeRowKeySerializer EDGE_ROW_KEY_SERIALIZER = new EdgeRowKeySerializer();

    //Edge serializers
    private static final EdgeSerializer EDGE_SERIALIZER = new EdgeSerializer();


    // column families
    /**
     * Edges that are from the source node. The row key is the source node
     */
    private static final MultiTennantColumnFamily<OrganizationScope, RowKey, DirectedEdge> GRAPH_SOURCE_NODE_EDGES =
            new MultiTennantColumnFamily<OrganizationScope, RowKey, DirectedEdge>( "Graph_Source_Node_Edges",
                    new OrganizationScopedRowKeySerializer<RowKey>( ROW_SERIALIZER ), EDGE_SERIALIZER );


    /**
     * Edges that are incoming to the target node.  The target node is the row key
     */
    private static final MultiTennantColumnFamily<OrganizationScope, RowKey, DirectedEdge> GRAPH_TARGET_NODE_EDGES =
            new MultiTennantColumnFamily<OrganizationScope, RowKey, DirectedEdge>( "Graph_Target_Node_Edges",
                    new OrganizationScopedRowKeySerializer<RowKey>( ROW_SERIALIZER ), EDGE_SERIALIZER );


    /**
     * The edges that are from the source node with target type.  The source node is the row key.
     */
    private static final MultiTennantColumnFamily<OrganizationScope, RowKeyType, DirectedEdge>
            GRAPH_SOURCE_NODE_TARGET_TYPE =
            new MultiTennantColumnFamily<OrganizationScope, RowKeyType, DirectedEdge>( "Graph_Source_Node_Target_Type",
                    new OrganizationScopedRowKeySerializer<RowKeyType>( ROW_TYPE_SERIALIZER ), EDGE_SERIALIZER );


    /**
     * The edges that are to the target node with the source type.  The target node is the row key
     */
    private static final MultiTennantColumnFamily<OrganizationScope, RowKeyType, DirectedEdge>
            GRAPH_TARGET_NODE_SOURCE_TYPE =
            new MultiTennantColumnFamily<OrganizationScope, RowKeyType, DirectedEdge>( "Graph_Target_Node_Source_Type",
                    new OrganizationScopedRowKeySerializer<RowKeyType>( ROW_TYPE_SERIALIZER ), EDGE_SERIALIZER );


    /**
     * Get all graph edge versions
     */
    private static final MultiTennantColumnFamily<OrganizationScope, EdgeRowKey, UUID> GRAPH_EDGE_VERSIONS =
            new MultiTennantColumnFamily<OrganizationScope, EdgeRowKey, UUID>( "Graph_Edge_Versions",
                    new OrganizationScopedRowKeySerializer<EdgeRowKey>( EDGE_ROW_KEY_SERIALIZER ),
                    UUIDSerializer.get() );


    protected final Keyspace keyspace;
    protected final CassandraConfig cassandraConfig;
    protected final GraphFig graphFig;


    @Inject
    public EdgeSerializationImpl( final Keyspace keyspace, final CassandraConfig cassandraConfig,
                                  final GraphFig graphFig ) {
        this.keyspace = keyspace;
        this.cassandraConfig = cassandraConfig;
        this.graphFig = graphFig;
    }


    @Override
    public MutationBatch writeEdge( final OrganizationScope scope, final Edge edge ) {
        final MutationBatch batch = keyspace.prepareMutationBatch().withConsistencyLevel( cassandraConfig.getWriteCL());


        doWrite( scope, edge, new RowOp<RowKey>() {
            @Override
            public  void doWrite(
                    final MultiTennantColumnFamily<OrganizationScope, RowKey, DirectedEdge> columnFamily, final RowKey rowKey,
                    final DirectedEdge edge ) {
                batch.withRow( columnFamily, ScopedRowKey.fromKey( scope, rowKey ) ).putColumn( edge, false );
            }


            @Override
            public void doWrite( final MultiTennantColumnFamily<OrganizationScope, EdgeRowKey, UUID> columnFamily,
                                 final EdgeRowKey rowKey, final UUID version ) {
                batch.withRow( columnFamily, ScopedRowKey.fromKey( scope, rowKey ) ).putColumn( version, false );
            }
        } );


        return batch;
    }


    @Override
    public MutationBatch markEdge( final OrganizationScope scope, final Edge edge ) {
        final MutationBatch batch = keyspace.prepareMutationBatch().withConsistencyLevel( cassandraConfig.getWriteCL() );


        doWrite( scope, edge, new RowOp<RowKey>() {
            @Override
            public void doWrite(
                    final MultiTennantColumnFamily<OrganizationScope, RowKey, DirectedEdge> columnFamily, final RowKey rowKey,
                    final DirectedEdge edge ) {
                batch.withRow( columnFamily, ScopedRowKey.fromKey( scope, rowKey ) ).putColumn( edge, true );
            }


            @Override
            public void doWrite( final MultiTennantColumnFamily<OrganizationScope, EdgeRowKey, UUID> columnFamily,
                                 final EdgeRowKey rowKey, final UUID version ) {
                batch.withRow( columnFamily, ScopedRowKey.fromKey( scope, rowKey ) ).putColumn( version, true );
            }
        } );


        return batch;
    }


    @Override
    public MutationBatch deleteEdge( final OrganizationScope scope, final Edge edge ) {
        final MutationBatch batch = keyspace.prepareMutationBatch().withConsistencyLevel( cassandraConfig.getWriteCL() );


        doWrite( scope, edge, new RowOp<RowKey>() {
            @Override
            public void doWrite(
                    final MultiTennantColumnFamily<OrganizationScope, RowKey, DirectedEdge> columnFamily, final RowKey rowKey,
                    final DirectedEdge edge ) {
                batch.withRow( columnFamily, ScopedRowKey.fromKey( scope, rowKey ) ).deleteColumn( edge );
            }


            @Override
            public void doWrite( final MultiTennantColumnFamily<OrganizationScope, EdgeRowKey, UUID> columnFamily,
                                 final EdgeRowKey rowKey, final UUID version ) {
                batch.withRow( columnFamily, ScopedRowKey.fromKey( scope, rowKey ) ).deleteColumn( version );
            }
        } );


        return batch;
    }


    /**
     * EdgeWrite the edges internally
     *
     * @param scope The scope to encapsulate
     * @param edge The edge to write
     * @param op The row operation to invoke
     */
    private void doWrite( final OrganizationScope scope, final Edge edge, final RowOp op ) {
        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateEdge( edge );

        final Id sourceNodeId = edge.getSourceNode();
        final Id targetNodeId = edge.getTargetNode();
        final UUID version = edge.getVersion();
        final String type = edge.getType();


        /**
         * Key in the serializers based on the edge
         */

        final RowKey sourceRowKey = new RowKey( sourceNodeId, type );

        final RowKeyType sourceRowKeyType = new RowKeyType( sourceNodeId, type, targetNodeId );

        final DirectedEdge sourceEdge = new DirectedEdge( targetNodeId, version );


        final RowKey targetRowKey = new RowKey( targetNodeId, type );

        final RowKeyType targetRowKeyType = new RowKeyType( targetNodeId, type, sourceNodeId );

        final DirectedEdge targetEdge = new DirectedEdge( sourceNodeId, version );


        final EdgeRowKey edgeRowKey = new EdgeRowKey( sourceNodeId, type, targetNodeId );


        /**
         * write edges from source->target
         */

        op.doWrite( GRAPH_SOURCE_NODE_EDGES, sourceRowKey, sourceEdge );

        op.doWrite( GRAPH_SOURCE_NODE_TARGET_TYPE, sourceRowKeyType, sourceEdge );


        /**
         * write edges from target<-source
         */
        op.doWrite( GRAPH_TARGET_NODE_EDGES, targetRowKey, targetEdge );

        op.doWrite( GRAPH_TARGET_NODE_SOURCE_TYPE, targetRowKeyType, targetEdge );


        /**
         * Write this in the version log for this edge of source->target
         */
        op.doWrite( GRAPH_EDGE_VERSIONS, edgeRowKey, version );
    }


    @Override
    public Iterator<MarkedEdge> getEdgeVersions( final OrganizationScope scope, final SearchByEdge search ) {
        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateSearchByEdge( search );

        final Id targetId = search.targetNode();
        final Id sourceId = search.sourceNode();
        final String type = search.getType();
        final UUID maxVersion = search.getMaxVersion();

        return getEdges( GRAPH_SOURCE_NODE_EDGES, new EdgeSearcher<RowKey>( scope, search.last() ) {


            @Override
            public void setRange( final RangeBuilder builder ) {


                if ( last.isPresent() ) {
                    super.setRange( builder );
                    return;
                }

                //start seeking at a value < our max version
                final DirectedEdge last = new DirectedEdge( targetId, maxVersion );
                builder.setStart( last, EDGE_SERIALIZER );
            }


            @Override
            protected RowKey generateRowKey() {
                return new RowKey( sourceId, type );
            }


            @Override
            protected DirectedEdge getStartColumn( final Edge last ) {
                return new DirectedEdge( last.getTargetNode(), last.getVersion() );
            }


            @Override
            protected MarkedEdge createEdge( final DirectedEdge edge, final boolean marked ) {
                return new SimpleMarkedEdge( sourceId, type, edge.id, edge.version, marked );
            }
        } );
    }


    @Override
    public Iterator<MarkedEdge> getEdgesFromSource( final OrganizationScope scope, final SearchByEdgeType edgeType ) {

        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateSearchByEdgeType( edgeType );

        final Id sourceId = edgeType.getNode();
        final String type = edgeType.getType();

        return getEdges( GRAPH_SOURCE_NODE_EDGES, new EdgeSearcher<RowKey>( scope, edgeType.last() ) {
            @Override
            protected RowKey generateRowKey() {
                return new RowKey( sourceId, type );
            }


            @Override
            protected DirectedEdge getStartColumn( final Edge last ) {
                return new DirectedEdge( last.getTargetNode(), last.getVersion() );
            }


            @Override
            protected MarkedEdge createEdge( final DirectedEdge edge, final boolean marked ) {
                return new SimpleMarkedEdge( sourceId, type, edge.id, edge.version, marked );
            }
        } );
    }


    @Override
    public Iterator<MarkedEdge> getEdgesFromSourceByTargetType( final OrganizationScope scope,
                                                                final SearchByIdType edgeType ) {

        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateSearchByEdgeType( edgeType );

        final Id targetId = edgeType.getNode();
        final String type = edgeType.getType();
        final String targetType = edgeType.getIdType();

        return getEdges( GRAPH_SOURCE_NODE_TARGET_TYPE, new EdgeSearcher<RowKeyType>( scope, edgeType.last() ) {
            @Override
            protected RowKeyType generateRowKey() {
                return new RowKeyType( targetId, type, targetType );
            }


            @Override
            protected DirectedEdge getStartColumn( final Edge last ) {
                return new DirectedEdge( last.getTargetNode(), last.getVersion() );
            }


            @Override
            protected MarkedEdge createEdge( final DirectedEdge edge, final boolean marked ) {
                return new SimpleMarkedEdge( targetId, type, edge.id, edge.version, marked );
            }
        } );
    }


    @Override
    public Iterator<MarkedEdge> getEdgesToTarget( final OrganizationScope scope, final SearchByEdgeType edgeType ) {

        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateSearchByEdgeType( edgeType );

        final Id targetId = edgeType.getNode();
        final String type = edgeType.getType();

        return getEdges( GRAPH_TARGET_NODE_EDGES, new EdgeSearcher<RowKey>( scope, edgeType.last() ) {


            @Override
            protected RowKey generateRowKey() {
                return new RowKey( targetId, type );
            }


            @Override
            protected DirectedEdge getStartColumn( final Edge last ) {
                return new DirectedEdge( last.getSourceNode(), last.getVersion() );
            }


            @Override
            protected MarkedEdge createEdge( final DirectedEdge edge, final boolean marked ) {
                return new SimpleMarkedEdge( edge.id, type, targetId, edge.version, marked );
            }
        } );
    }


    @Override
    public Iterator<MarkedEdge> getEdgesToTargetBySourceType( final OrganizationScope scope,
                                                              final SearchByIdType edgeType ) {

        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateSearchByEdgeType( edgeType );

        final Id targetId = edgeType.getNode();
        final String sourceType = edgeType.getIdType();
        final String type = edgeType.getType();

        return getEdges( GRAPH_TARGET_NODE_SOURCE_TYPE, new EdgeSearcher<RowKeyType>( scope, edgeType.last() ) {
            @Override
            protected RowKeyType generateRowKey() {
                return new RowKeyType( targetId, type, sourceType );
            }


            @Override
            protected DirectedEdge getStartColumn( final Edge last ) {
                return new DirectedEdge( last.getTargetNode(), last.getVersion() );
            }


            @Override
            protected MarkedEdge createEdge( final DirectedEdge edge, final boolean marked ) {
                return new SimpleMarkedEdge( edge.id, type, targetId, edge.version, marked );
            }
        } );
    }


    /**
     * Get the edges with the specified criteria
     *
     * @param cf The column family to search
     * @param searcher The searcher to use to construct the queries
     *
     * @return An iterator of all Edge instance that match the criteria
     */
    private <R> Iterator<MarkedEdge> getEdges( MultiTennantColumnFamily<OrganizationScope, R, DirectedEdge> cf,
                                               final EdgeSearcher<R> searcher ) {


        /**
         * If the edge is present, we need to being seeking from this
         */

        final RangeBuilder rangeBuilder = new RangeBuilder().setLimit( cassandraConfig.getScanPageSize() );


        //set the range into the search
        searcher.setRange( rangeBuilder );

        final ScopedRowKey<OrganizationScope, R> rowKey = searcher.getRowKey();


        RowQuery<ScopedRowKey<OrganizationScope, R>, DirectedEdge> query =
                keyspace.prepareQuery( cf ).setConsistencyLevel( cassandraConfig.getReadCL() ).getKey( rowKey )
                        .autoPaginate( true ).withColumnRange( rangeBuilder.build() );


        return new ColumnNameIterator<DirectedEdge, MarkedEdge>( query, searcher, searcher.hasPage(), graphFig.getReadTimeout() );
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Arrays.asList( graphCf( GRAPH_SOURCE_NODE_EDGES ), graphCf( GRAPH_TARGET_NODE_EDGES ),
                graphCf( GRAPH_SOURCE_NODE_TARGET_TYPE ), graphCf( GRAPH_TARGET_NODE_SOURCE_TYPE ),
                new MultiTennantColumnFamilyDefinition( GRAPH_EDGE_VERSIONS, BytesType.class.getSimpleName(),
                        ColumnTypes.UUID_TYPE_REVERSED, BytesType.class.getSimpleName(),
                        MultiTennantColumnFamilyDefinition.CacheOption.KEYS ) );
    }


    /**
     * Helper to generate an edge definition by the type
     */
    private MultiTennantColumnFamilyDefinition graphCf( MultiTennantColumnFamily cf ) {
        return new MultiTennantColumnFamilyDefinition( cf, BytesType.class.getSimpleName(),
                ColumnTypes.DYNAMIC_COMPOSITE_TYPE, BytesType.class.getSimpleName(),
                MultiTennantColumnFamilyDefinition.CacheOption.KEYS );
    }


    /**
     * Internal class to represent edge data for serialization
     */
    private static class DirectedEdge {

        public final UUID version;
        public final Id id;


        private DirectedEdge( final Id id, final UUID version ) {
            this.version = version;
            this.id = id;
        }
    }


    /**
     * Serializes to a source->target edge Note that we cannot set the edge type on de-serialization.  Only the target
     * Id and version.
     */
    private static class EdgeSerializer extends AbstractSerializer<DirectedEdge> {

        private static final IdColDynamicCompositeSerializer ID_COL_SERIALIZER = IdColDynamicCompositeSerializer.get();

        private static final UUIDSerializer UUID_SERIALIZER = UUIDSerializer.get();


        @Override
        public ByteBuffer toByteBuffer( final DirectedEdge edge ) {
            final DynamicComposite colValue = createComposite( edge, AbstractComposite.ComponentEquality.EQUAL );

            return colValue.serialize();
        }


        @Override
        public DirectedEdge fromByteBuffer( final ByteBuffer byteBuffer ) {
            DynamicComposite composite = DynamicComposite.fromByteBuffer( byteBuffer );

            Preconditions.checkArgument( composite.size() == 3, "Composite should have 3 elements" );


            //return the version
            final UUID version = composite.get( 0, UUID_SERIALIZER );


            //parse our id
            final Id id = ID_COL_SERIALIZER.fromComposite( composite, 1 );


            return new DirectedEdge( id, version );
        }


        /**
         * Create the dynamic composite for this directed edge
         */
        private DynamicComposite createComposite( DirectedEdge edge, AbstractComposite.ComponentEquality equality ) {
            DynamicComposite composite = new DynamicComposite();

            //add our edge
            composite.addComponent( edge.version, UUID_SERIALIZER, ColumnTypes.UUID_TYPE_REVERSED, equality );


            ID_COL_SERIALIZER.toComposite( composite, edge.id );


            return composite;
        }
    }


    /**
     * Class that represents an edge row key
     */
    private static class RowKey {
        public final Id nodeId;
        public final long[] hash;


        /**
         * Create a row key with the node and the edgeType
         */
        public RowKey( Id nodeId, String edgeType ) {
            this( nodeId, EdgeHasher.createEdgeHash( edgeType ) );
        }


        /**
         * Create a new row key with the hash, should only be used in deserialization or internal callers.
         */
        protected RowKey( Id nodeId, long[] hash ) {
            this.nodeId = nodeId;
            this.hash = hash;
        }
    }


    /**
     * The row key with the additional type
     */
    private static class RowKeyType extends RowKey {

        /**
         * Create a row key with the node id in the row key, the edge type, and the type from the typeid
         *
         * @param nodeId The node id in the row key
         * @param edgeType The type of the edge
         * @param typeId The type of hte id
         */
        public RowKeyType( final Id nodeId, final String edgeType, final Id typeId ) {
            this( nodeId, edgeType, typeId.getType() );
        }


        /**
         * Create a row key with the node id in the row key, the edge type, adn the target type from the id
         */
        public RowKeyType( final Id nodeId, final String edgeType, final String targetType ) {
            super( nodeId, EdgeHasher.createEdgeHash( edgeType, targetType ) );
        }


        /**
         * Internal use in de-serializing.  Should only be used in this case or by internal callers
         */
        private RowKeyType( final Id nodeId, final long[] hash ) {
            super( nodeId, hash );
        }
    }


    /**
     * Used to store row keys by sourceId, targetId and edgeType
     */
    private static class EdgeRowKey {
        public final Id sourceId;
        public final Id targetId;
        public final String edgeType;


        private EdgeRowKey( final Id sourceId, final String edgeType, final Id targetId ) {
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.edgeType = edgeType;
        }
    }


    /**
     * Searcher to be used when performing the search.  Performs I/O transformation as well as parsing for the iterator
     */
    private static abstract class EdgeSearcher<R> implements ColumnParser<DirectedEdge, MarkedEdge> {

        protected final Optional<Edge> last;
        protected final OrganizationScope scope;


        protected EdgeSearcher( final OrganizationScope scope, final Optional<Edge> last ) {
            this.scope = scope;
            this.last = last;
        }


        /**
         * Set the range on a search
         */
        public void setRange( final RangeBuilder builder ) {

            //set our start range since it was supplied to us
            if ( last.isPresent() ) {
                DirectedEdge sourceEdge = getStartColumn( last.get() );


                builder.setStart( sourceEdge, EDGE_SERIALIZER );
            }
        }


        /**
         * Get the row key to be used for the search
         */
        public ScopedRowKey<OrganizationScope, R> getRowKey() {
            return ScopedRowKey.fromKey( scope, generateRowKey() );
        }


        public boolean hasPage() {
            return last.isPresent();
        }


        @Override
        public MarkedEdge parseColumn( final Column<DirectedEdge> column ) {
            final DirectedEdge edge = column.getName();

            return createEdge( edge, column.getBooleanValue() );
        }


        /**
         * Create a row key for this search to use
         */
        protected abstract R generateRowKey();


        /**
         * Set the start column to begin searching from.  The last is provided
         */
        protected abstract DirectedEdge getStartColumn( Edge last );


        /**
         * Create an edge to return to the user based on the directed edge provided
         *
         * @param edge The edge in the column name
         * @param marked The marked flag in the column value
         */
        protected abstract MarkedEdge createEdge( DirectedEdge edge, boolean marked );
    }


    /**
     * Class to perform serialization for row keys from edges
     */
    private static class RowSerializer implements CompositeFieldSerializer<RowKey> {

        private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


        @Override
        public void toComposite( final CompositeBuilder builder, final RowKey key ) {

            //add the row id to the composite
            ID_SER.toComposite( builder, key.nodeId );

            builder.addLong( key.hash[0] );
            builder.addLong( key.hash[1] );
        }


        @Override
        public RowKey fromComposite( final CompositeParser composite ) {

            final Id id = ID_SER.fromComposite( composite );
            long[] hash = new long[] { composite.readLong(), composite.readLong() };

            return new RowKey( id, hash );
        }
    }


    /**
     * Class to perform serialization for row keys from edges
     */
    private static class RowTypeSerializer implements CompositeFieldSerializer<RowKeyType> {

        private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


        @Override
        public void toComposite( final CompositeBuilder builder, final RowKeyType keyType ) {

            //add the row id to the composite
            ID_SER.toComposite( builder, keyType.nodeId );

            builder.addLong( keyType.hash[0] );
            builder.addLong( keyType.hash[1] );
        }


        @Override
        public RowKeyType fromComposite( final CompositeParser composite ) {

            final Id id = ID_SER.fromComposite( composite );
            long[] hash = new long[] { composite.readLong(), composite.readLong() };

            return new RowKeyType( id, hash );
        }
    }


    /**
     * Class to perform serialization for row keys from edges
     */
    private static class EdgeRowKeySerializer implements CompositeFieldSerializer<EdgeRowKey> {

        private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


        @Override
        public void toComposite( final CompositeBuilder builder, final EdgeRowKey key ) {

            //add the row id to the composite
            ID_SER.toComposite( builder, key.sourceId );
            builder.addString( key.edgeType );
            ID_SER.toComposite( builder, key.targetId );
        }


        @Override
        public EdgeRowKey fromComposite( final CompositeParser composite ) {

            final Id sourceId = ID_SER.fromComposite( composite );
            final String edgeType = composite.readString();
            final Id targetId = ID_SER.fromComposite( composite );

            return new EdgeRowKey( sourceId, edgeType, targetId );
        }
    }


    /**
     * Simple callback to perform puts and deletes with a common row setup code
     */
    private static interface RowOp<R> {

        /**
         * Write the edge with the given data
         */
        void doWrite(
                final MultiTennantColumnFamily<OrganizationScope, R, DirectedEdge> columnFamily, R rowKey,
                DirectedEdge edge );

        /**
         * Write the edge into the version cf
         */
        void doWrite( final MultiTennantColumnFamily<OrganizationScope, EdgeRowKey, UUID> columnFamily,
                      EdgeRowKey rowKey, UUID version );
    }
}
