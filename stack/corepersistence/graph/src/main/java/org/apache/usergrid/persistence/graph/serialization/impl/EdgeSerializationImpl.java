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
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
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
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
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

    //TODO, make this a config property?
    private static final int PAGE_SIZE = 100;

    //holder to put data into col value
    private static final byte[] HOLDER = new byte[] { 0 };

    //Row key with no type
    private static final RowSerializer ROW_SERIALIZER = new RowSerializer();

    //row key with target id type
    private static final RowTypeSerializer ROW_TYPE_SERIALIZER = new RowTypeSerializer();

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


    protected final Keyspace keyspace;


    @Inject
    public EdgeSerializationImpl( final Keyspace keyspace ) {
        this.keyspace = keyspace;
    }


    @Override
    public MutationBatch writeEdge( final OrganizationScope scope, final Edge edge ) {
        final MutationBatch batch = keyspace.prepareMutationBatch();


        doWrite( scope, edge, new RowOp() {
            @Override
            public void doWrite( final MultiTennantColumnFamily columnFamily, final Object rowKey,
                                 final DirectedEdge edge ) {

                batch.withRow( columnFamily, ScopedRowKey.fromKey( scope, rowKey ) ).putColumn( edge, HOLDER );
            }
        } );


        return batch;
    }


    @Override
    public MutationBatch deleteEdge( final OrganizationScope scope, final Edge edge ) {
        final MutationBatch batch = keyspace.prepareMutationBatch();


        doWrite( scope, edge, new RowOp() {
            @Override
            public void doWrite( final MultiTennantColumnFamily columnFamily, final Object rowKey,
                                 final DirectedEdge edge ) {

                batch.withRow( columnFamily, ScopedRowKey.fromKey( scope, rowKey ) ).deleteColumn( edge );
            }
        } );


        return batch;
    }



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


        /**
         * Write edges from target<-source
         */


        op.doWrite( GRAPH_SOURCE_NODE_EDGES, sourceRowKey, sourceEdge );

        op.doWrite( GRAPH_SOURCE_NODE_TARGET_TYPE, sourceRowKeyType, sourceEdge );

        op.doWrite( GRAPH_TARGET_NODE_EDGES, targetRowKey, targetEdge );

        op.doWrite( GRAPH_TARGET_NODE_SOURCE_TYPE, targetRowKeyType, targetEdge );
    }



    @Override
    public Iterator<Edge> getEdgeFromSource( final OrganizationScope scope, final SearchByEdge search ) {
        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateSearchByEdge( search );

        final Id sourceId = search.sourceNode();
        final Id targetId = search.targetNode();
        final String type = search.getType();
        final UUID maxVersion = search.getMaxVersion();
        final Optional<Edge> last = search.last();

        return getEdges( GRAPH_SOURCE_NODE_EDGES, new EdgeSearcher<RowKey>() {

            @Override
            public void setRange( final RangeBuilder builder ) {

                //set our start range since it was supplied to us
                if ( search.last().isPresent() ) {

                    final Edge edge = search.last().get();
                    DirectedEdge sourceEdge = new DirectedEdge( edge.getTargetNode(), edge.getVersion() );

                    builder.setStart( sourceEdge, EDGE_SERIALIZER );
                }


                //                final DirectedEdge last = new DirectedEdge( targetId, maxVersion );
                //                final ByteBuffer colValue = EDGE_SERIALIZER.createSearchEdgeInclusive( last );
                //                builder.setEnd( colValue );
            }


            @Override
            public ScopedRowKey<OrganizationScope, RowKey> getRowKey() {
                final RowKey sourceRowKey = new RowKey( sourceId, type );

                return ScopedRowKey.fromKey( scope, sourceRowKey );
            }


            @Override
            public boolean hasPage() {
                return search.last().isPresent();
            }


            @Override
            public Edge parseColumn( final Column<DirectedEdge> column ) {
                final DirectedEdge edge = column.getName();

                return new SimpleEdge( sourceId, type, edge.id, edge.version );
            }
        } );
    }


    @Override
    public Iterator<Edge> getEdgesFromSource( final OrganizationScope scope, final SearchByEdgeType edgeType ) {

        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateSearchByEdgeType( edgeType );

        final Id sourceId = edgeType.getNode();
        final String type = edgeType.getType();

        return getEdges( GRAPH_SOURCE_NODE_EDGES, new EdgeSearcher<RowKey>() {

            @Override
            public void setRange( final RangeBuilder builder ) {

                //set our start range since it was supplied to us
                if ( edgeType.last().isPresent() ) {

                    final Edge edge = edgeType.last().get();
                    DirectedEdge sourceEdge = new DirectedEdge( edge.getTargetNode(), edge.getVersion() );

                    builder.setStart( sourceEdge, EDGE_SERIALIZER );
                }
            }


            @Override
            public ScopedRowKey<OrganizationScope, RowKey> getRowKey() {
                final RowKey sourceRowKey = new RowKey( sourceId, type );

                return ScopedRowKey.fromKey( scope, sourceRowKey );
            }


            @Override
            public boolean hasPage() {
                return edgeType.last().isPresent();
            }


            @Override
            public Edge parseColumn( final Column<DirectedEdge> column ) {
                final DirectedEdge edge = column.getName();

                return new SimpleEdge( sourceId, type, edge.id, edge.version );
            }
        } );
    }


    @Override
    public Iterator<Edge> getEdgesFromSourceByTargetType( final OrganizationScope scope,
                                                          final SearchByIdType edgeType ) {

        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateSearchByEdgeType( edgeType );

        final Id targetId = edgeType.getNode();
        final String type = edgeType.getType();
        final String targetType = edgeType.getIdType();

        return getEdges( GRAPH_SOURCE_NODE_TARGET_TYPE, new EdgeSearcher<RowKeyType>() {

            @Override
            public void setRange( final RangeBuilder builder ) {

                //set our start range since it was supplied to us
                if ( edgeType.last().isPresent() ) {

                    final Edge edge = edgeType.last().get();
                    DirectedEdge sourceEdge = new DirectedEdge( edge.getTargetNode(), edge.getVersion() );

                    builder.setStart( sourceEdge, EDGE_SERIALIZER );
                }
            }


            @Override
            public ScopedRowKey<OrganizationScope, RowKeyType> getRowKey() {
                final RowKeyType sourceRowKey = new RowKeyType( targetId, type, targetType );

                return ScopedRowKey.fromKey( scope, sourceRowKey );
            }


            @Override
            public boolean hasPage() {
                return edgeType.last().isPresent();
            }


            @Override
            public Edge parseColumn( final Column<DirectedEdge> column ) {
                final DirectedEdge edge = column.getName();

                return new SimpleEdge( targetId, type, edge.id, edge.version );
            }
        } );
    }


    @Override
    public Iterator<Edge> getEdgesToTarget( final OrganizationScope scope, final SearchByEdgeType edgeType ) {

        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateSearchByEdgeType( edgeType );

        final Id targetId = edgeType.getNode();
        final String type = edgeType.getType();

        return getEdges( GRAPH_TARGET_NODE_EDGES, new EdgeSearcher<RowKey>() {

            @Override
            public void setRange( final RangeBuilder builder ) {

                //set our start range since it was supplied to us
                if ( edgeType.last().isPresent() ) {

                    final Edge edge = edgeType.last().get();
                    DirectedEdge sourceEdge = new DirectedEdge( edge.getSourceNode(), edge.getVersion() );

                    builder.setStart( sourceEdge, EDGE_SERIALIZER );
                }
            }


            @Override
            public ScopedRowKey<OrganizationScope, RowKey> getRowKey() {
                final RowKey sourceRowKey = new RowKey( targetId, type );

                return ScopedRowKey.fromKey( scope, sourceRowKey );
            }


            @Override
            public boolean hasPage() {
                return edgeType.last().isPresent();
            }


            @Override
            public Edge parseColumn( final Column<DirectedEdge> column ) {
                final DirectedEdge edge = column.getName();

                return new SimpleEdge( edge.id, type, targetId, edge.version );
            }
        } );
    }


    @Override
    public Iterator<Edge> getEdgeToTarget( final OrganizationScope scope, final SearchByEdge search ) {
        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateSearchByEdge( search );

        final Id sourceId = search.sourceNode();
        final Id targetId = search.targetNode();
        final String type = search.getType();
        final UUID maxVersion = search.getMaxVersion();
        final Optional<Edge> last = search.last();

        return getEdges( GRAPH_TARGET_NODE_EDGES, new EdgeSearcher<RowKey>() {

            @Override
            public void setRange( final RangeBuilder builder ) {

                //set our start range since it was supplied to us
                if ( last.isPresent() ) {

                    final Edge edge = last.get();
                    DirectedEdge sourceEdge = new DirectedEdge( edge.getSourceNode(), edge.getVersion() );

                    builder.setStart( sourceEdge, EDGE_SERIALIZER );
                }


                final DirectedEdge last = new DirectedEdge( sourceId, maxVersion );
                final ByteBuffer colValue = EDGE_SERIALIZER.createSearchEdgeInclusive( last );
                builder.setEnd( colValue );
            }


            @Override
            public ScopedRowKey<OrganizationScope, RowKey> getRowKey() {
                final RowKey sourceRowKey = new RowKey( targetId, type );

                return ScopedRowKey.fromKey( scope, sourceRowKey );
            }


            @Override
            public boolean hasPage() {
                return last.isPresent();
            }


            @Override
            public Edge parseColumn( final Column<DirectedEdge> column ) {
                final DirectedEdge edge = column.getName();

                return new SimpleEdge( edge.id, type, targetId, edge.version );
            }
        } );
    }


    @Override
    public Iterator<Edge> getEdgesToTargetBySourceType( final OrganizationScope scope, final SearchByIdType edgeType ) {

        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateSearchByEdgeType( edgeType );

        final Id targetId = edgeType.getNode();
        final String sourceType = edgeType.getIdType();
        final String type = edgeType.getType();

        return getEdges( GRAPH_TARGET_NODE_SOURCE_TYPE, new EdgeSearcher<RowKeyType>() {

            @Override
            public void setRange( final RangeBuilder builder ) {

                //set our start range since it was supplied to us
                if ( edgeType.last().isPresent() ) {

                    final Edge edge = edgeType.last().get();
                    DirectedEdge sourceEdge = new DirectedEdge( edge.getTargetNode(), edge.getVersion() );

                    builder.setStart( sourceEdge, EDGE_SERIALIZER );
                }
            }


            @Override
            public ScopedRowKey<OrganizationScope, RowKeyType> getRowKey() {
                final RowKeyType sourceRowKey = new RowKeyType( targetId, type, sourceType );

                return ScopedRowKey.fromKey( scope, sourceRowKey );
            }


            @Override
            public boolean hasPage() {
                return edgeType.last().isPresent();
            }


            @Override
            public Edge parseColumn( final Column<DirectedEdge> column ) {
                final DirectedEdge edge = column.getName();

                return new SimpleEdge( targetId, type, edge.id, edge.version );
            }
        } );
    }


    /**
     * Get the edges with the specified criteria
     *
     * @param scope The organization scope
     * @param cf The column familiy to search
     *
     * @return An iterator of all Edge instance that match the criteria
     */
    private <R> Iterator<Edge> getEdges( MultiTennantColumnFamily<OrganizationScope, R, DirectedEdge> cf,
                                         final EdgeSearcher<R> searcher ) {


        /**
         * If the edge is present, we need to being seeking from this
         */

        final RangeBuilder rangeBuilder = new RangeBuilder().setLimit( PAGE_SIZE );


        //set the range into the search
        searcher.setRange( rangeBuilder );

        final ScopedRowKey<OrganizationScope, R> rowKey = searcher.getRowKey();


        RowQuery<ScopedRowKey<OrganizationScope, R>, DirectedEdge> query =
                keyspace.prepareQuery( cf ).getKey( rowKey ).autoPaginate( true )
                        .withColumnRange( rangeBuilder.build() );


        try {
            return new ColumnNameIterator<DirectedEdge, Edge>( query.execute().getResult().iterator(), searcher,
                    searcher.hasPage() );
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to connect to cassandra", e );
        }
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Arrays.asList( graphCf( GRAPH_SOURCE_NODE_EDGES ), graphCf( GRAPH_TARGET_NODE_EDGES ),
                graphCf( GRAPH_SOURCE_NODE_TARGET_TYPE ), graphCf( GRAPH_TARGET_NODE_SOURCE_TYPE ) );
    }


    /**
     * Helper to generate an edge definition by the type
     */
    private MultiTennantColumnFamilyDefinition graphCf( MultiTennantColumnFamily cf ) {
        return new MultiTennantColumnFamilyDefinition( cf, ColumnTypes.DYNAMIC_COMPOSITE_TYPE,
                BytesType.class.getSimpleName(), BytesType.class.getSimpleName() );
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


            //parse our id
            final Id id = ID_COL_SERIALIZER.fromComposite( composite, 0 );

            //return the version
            final UUID version = composite.get( 2, UUID_SERIALIZER );


            return new DirectedEdge( id, version );
        }


        /**
         * Creates a column value that will include the directed Edge as it's last element.  I.E all edges returned from
         * the scan will have the node id and a version <= the specified directed edge version
         */
        public ByteBuffer createSearchEdgeInclusive( DirectedEdge edge ) {
            final DynamicComposite colValue =
                    createComposite( edge, AbstractComposite.ComponentEquality.GREATER_THAN_EQUAL );

            return colValue.serialize();
        }


        /**
         * Create the dynamic composite for this directed edge
         */
        private DynamicComposite createComposite( DirectedEdge edge, AbstractComposite.ComponentEquality equality ) {
            DynamicComposite composite = new DynamicComposite();

            ID_COL_SERIALIZER.toComposite( composite, edge.id );

            //add our edge
            composite.addComponent( edge.version, UUID_SERIALIZER, equality );

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
     * Simple parser to parse column names
     */
    private interface EdgeSearcher<R> extends ColumnParser<DirectedEdge, Edge> {


        /**
         * Set the range on a search
         */
        public void setRange( RangeBuilder builder );

        /**
         * Get the row key to be used for the search
         */
        public abstract ScopedRowKey<OrganizationScope, R> getRowKey();

        /**
         * Return true if the search has a page value, meaning we should skip the first value
         *
         * @return true if there is an element in the page
         */
        public boolean hasPage();


        //        protected abstract Edge buildEdge( final Id rowKeyId, final String edgeType, final Id colId,
        //                                           final UUID version );
    }

    //
    //    /**
    //     * Creator from source->target
    //     */
    //    private static class SourceEdgeColumnParser extends EdgeSearcher {
    //
    //
    //        /**
    //         * Default constructor. Needs to take the id and edge used in the search
    //         */
    //        protected SourceEdgeColumnParser( final SearchByEdgeType searchByEdgeType ) {
    //            super( searchByEdgeType );
    //        }
    //
    //
    //        @Override
    //        protected Edge buildEdge( final Id rowKeyId, final String edgeType, final Id colId, final UUID version ) {
    //            return new SimpleEdge( rowKeyId, edgeType, colId, version );
    //        }
    //    }


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
     * Serializes the row key by sourceId, edge type
     */
    private static class SourceRowSerializer extends RowSerializer {

        private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


        protected long[] hash( Edge e ) {
            return EdgeHasher.createEdgeHash( e.getType() );
        }


        protected Id getRowId( Edge e ) {
            return e.getSourceNode();
        }
    }


    /**
     * Serializes the row key by sourceId, edge type, and target node type
     */
    private static class SourceRowTypeSerializer extends SourceRowSerializer {

        @Override
        protected long[] hash( final Edge e ) {
            return EdgeHasher.createEdgeHash( e.getType(), e.getTargetNode() );
        }
    }


    /**
     * Serializes the row key by targetId, edge type
     */
    private static class TargetRowSerializer extends RowSerializer {

        private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


        protected long[] hash( Edge e ) {
            return EdgeHasher.createEdgeHash( e.getType() );
        }


        protected Id getRowId( Edge e ) {
            return e.getTargetNode();
        }
    }


    /**
     * Serializes the row key by targetId, edge type, and source node type
     */
    private static class TargetRowTypeSerializer extends SourceRowSerializer {

        @Override
        protected long[] hash( final Edge e ) {
            return EdgeHasher.createEdgeHash( e.getType(), e.getSourceNode() );
        }
    }


    /**
     * Simple callback to perform puts and deletes with a common row setup code
     */
    private static interface RowOp<R> {

        void doWrite( final MultiTennantColumnFamily<OrganizationScope, R, DirectedEdge> columnFamily, R rowKey,
                      DirectedEdge edge );
    }
}
