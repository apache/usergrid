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
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cassandra.db.marshal.BytesType;

import org.apache.usergrid.persistence.core.astyanax.CassandraConfig;
import org.apache.usergrid.persistence.core.astyanax.ColumnTypes;
import org.apache.usergrid.persistence.core.astyanax.OrganizationScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.astyanax.ColumnNameIterator;
import org.apache.usergrid.persistence.core.astyanax.ColumnParser;
import org.apache.usergrid.persistence.core.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.core.astyanax.IdColDynamicCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.migration.Migration;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.impl.SimpleMarkedEdge;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardStrategy;
import org.apache.usergrid.persistence.graph.serialization.util.EdgeHasher;
import org.apache.usergrid.persistence.graph.serialization.util.EdgeUtils;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.model.AbstractComposite;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.model.DynamicComposite;
import com.netflix.astyanax.query.RowQuery;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.LongSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;
import com.netflix.astyanax.util.RangeBuilder;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 *  Serialization for edges.  Delegates partitioning to the sharding strategy.
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

    private static final LongSerializer LONG_SERIALIZER = LongSerializer.get();


    /**
     * Get all graph edge versions
     */
    private final MultiTennantColumnFamily<ApplicationScope, EdgeRowKey, Long> graphEdgeVersionsCf;


    // column families
    /**
     * Edges that are from the source node. The row key is the source node
     */
    private final MultiTennantColumnFamily<ApplicationScope, RowKey, DirectedEdge> sourceNodeEdgesCf;


    /**
     * Edges that are incoming to the target node.  The target node is the row key
     */
    private final MultiTennantColumnFamily<ApplicationScope, RowKey, DirectedEdge> targetNodeEdgesCf;

    /**
     * The edges that are from the source node with target type.  The source node is the row key.
     */
    private final MultiTennantColumnFamily<ApplicationScope, RowKeyType, DirectedEdge> sourceNodeTargetTypeCf;


    /**
     * The edges that are to the target node with the source type.  The target node is the row key
     */
    private final MultiTennantColumnFamily<ApplicationScope, RowKeyType, DirectedEdge> targetNodeSourceTypeCf;

    protected final Keyspace keyspace;
    protected final CassandraConfig cassandraConfig;
    protected final GraphFig graphFig;
    protected final EdgeShardStrategy edgeShardStrategy;


    @Inject
    public EdgeSerializationImpl( final Keyspace keyspace, final CassandraConfig cassandraConfig,
                                  final GraphFig graphFig, final EdgeShardStrategy edgeShardStrategy ) {

        checkNotNull( "keyspace required", keyspace );
        checkNotNull( "cassandraConfig required", cassandraConfig );
        checkNotNull( "consistencyFig required", graphFig );
        checkNotNull( "sourceNodeCfName required", edgeShardStrategy.getSourceNodeCfName() );
        checkNotNull( "targetNodeCfName required", edgeShardStrategy.getTargetNodeCfName() );
        checkNotNull( "sourceNodeTargetTypeCfName required", edgeShardStrategy.getSourceNodeTargetTypeCfName() );
        checkNotNull( "targetNodeSourceTypeCfName required", edgeShardStrategy.getTargetNodeSourceTypeCfName() );


        this.keyspace = keyspace;
        this.cassandraConfig = cassandraConfig;
        this.graphFig = graphFig;
        this.edgeShardStrategy = edgeShardStrategy;

        //initialize the CF's from our implementation
        sourceNodeEdgesCf = new MultiTennantColumnFamily<ApplicationScope, RowKey, DirectedEdge>(
                edgeShardStrategy.getSourceNodeCfName(),
                new OrganizationScopedRowKeySerializer<RowKey>( ROW_SERIALIZER ), EDGE_SERIALIZER );


        targetNodeEdgesCf = new MultiTennantColumnFamily<ApplicationScope, RowKey, DirectedEdge>(
                edgeShardStrategy.getTargetNodeCfName(),
                new OrganizationScopedRowKeySerializer<RowKey>( ROW_SERIALIZER ), EDGE_SERIALIZER );


        sourceNodeTargetTypeCf = new MultiTennantColumnFamily<ApplicationScope, RowKeyType, DirectedEdge>(
                edgeShardStrategy.getSourceNodeTargetTypeCfName(),
                new OrganizationScopedRowKeySerializer<RowKeyType>( ROW_TYPE_SERIALIZER ), EDGE_SERIALIZER );


        /**
         * The edges that are to the target node with the source type.  The target node is the row key
         */
        targetNodeSourceTypeCf = new MultiTennantColumnFamily<ApplicationScope, RowKeyType, DirectedEdge>(
                edgeShardStrategy.getTargetNodeSourceTypeCfName(),
                new OrganizationScopedRowKeySerializer<RowKeyType>( ROW_TYPE_SERIALIZER ), EDGE_SERIALIZER );

        graphEdgeVersionsCf = new MultiTennantColumnFamily<ApplicationScope, EdgeRowKey, Long>(
                edgeShardStrategy.getGraphEdgeVersions(),
                new OrganizationScopedRowKeySerializer<EdgeRowKey>( EDGE_ROW_KEY_SERIALIZER ), LONG_SERIALIZER );
    }


    @Override
    public MutationBatch writeEdge( final ApplicationScope scope, final MarkedEdge markedEdge, final UUID timestamp ) {
        ValidationUtils.validateApplicationScope( scope );
        EdgeUtils.validateEdge( markedEdge );
        ValidationUtils.verifyTimeUuid( timestamp, "timestamp" );


        final MutationBatch batch =
                keyspace.prepareMutationBatch().withConsistencyLevel( cassandraConfig.getWriteCL() ).withTimestamp( timestamp.timestamp() );

        final boolean isDeleted = markedEdge.isDeleted();


        doWrite( scope, markedEdge, new RowOp<RowKey>() {
            @Override
            public void writeEdge( final MultiTennantColumnFamily<ApplicationScope, RowKey, DirectedEdge> columnFamily,
                                   final RowKey rowKey, final DirectedEdge edge ) {
                batch.withRow( columnFamily, ScopedRowKey.fromKey( scope, rowKey ) ).putColumn( edge, isDeleted );
            }


            @Override
            public void countEdge( final Id rowId, final long shardId, final String... types ) {
                edgeShardStrategy.increment( scope, rowId, shardId, 1l, types );
            }


            @Override
            public void writeVersion( final MultiTennantColumnFamily<ApplicationScope, EdgeRowKey, Long> columnFamily,
                                      final EdgeRowKey rowKey, final long timestamp) {
                batch.withRow( columnFamily, ScopedRowKey.fromKey( scope, rowKey ) ).putColumn( timestamp, isDeleted );
            }
        } );


        return batch;
    }


    @Override
    public MutationBatch deleteEdge( final ApplicationScope scope, final MarkedEdge markedEdge, final UUID timestamp ) {
        ValidationUtils.validateApplicationScope( scope );
        EdgeUtils.validateEdge( markedEdge );
        ValidationUtils.verifyTimeUuid( timestamp, "timestamp" );


        final MutationBatch batch =
                keyspace.prepareMutationBatch().withConsistencyLevel( cassandraConfig.getWriteCL() ).withTimestamp( timestamp.timestamp() );


        doWrite( scope, markedEdge, new RowOp<RowKey>() {
            @Override
            public void writeEdge( final MultiTennantColumnFamily<ApplicationScope, RowKey, DirectedEdge> columnFamily,
                                   final RowKey rowKey, final DirectedEdge edge ) {
                batch.withRow( columnFamily, ScopedRowKey.fromKey( scope, rowKey ) ).deleteColumn( edge );
            }


            @Override
            public void countEdge( final Id rowId, final long shardId, final String... types ) {
                edgeShardStrategy.increment( scope, rowId, shardId, -1, types );
            }


            @Override
            public void writeVersion( final MultiTennantColumnFamily<ApplicationScope, EdgeRowKey, Long> columnFamily,
                                      final EdgeRowKey rowKey, final long timestamp) {
                batch.withRow( columnFamily, ScopedRowKey.fromKey( scope, rowKey ) ).deleteColumn( timestamp );
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
    private void doWrite( final ApplicationScope scope, final MarkedEdge edge, final RowOp op ) {
        ValidationUtils.validateApplicationScope( scope );
        EdgeUtils.validateEdge( edge );

        final Id sourceNodeId = edge.getSourceNode();
        final Id targetNodeId = edge.getTargetNode();
        final long timestamp= edge.getTimestamp();
        final String type = edge.getType();


        /**
         * Key in the serializers based on the edge
         */

        final RowKey sourceRowKey =
                new RowKey( sourceNodeId, type, edgeShardStrategy.getWriteShard( scope, sourceNodeId, timestamp, type ) );

        final RowKeyType sourceRowKeyType = new RowKeyType( sourceNodeId, type, targetNodeId,
                edgeShardStrategy.getWriteShard( scope, sourceNodeId, timestamp, type, targetNodeId.getType() ) );

        final DirectedEdge sourceEdge = new DirectedEdge( targetNodeId, timestamp );


        final RowKey targetRowKey =
                new RowKey( targetNodeId, type, edgeShardStrategy.getWriteShard( scope, targetNodeId, timestamp, type ) );

        final RowKeyType targetRowKeyType = new RowKeyType( targetNodeId, type, sourceNodeId,
                edgeShardStrategy.getWriteShard( scope, targetNodeId, timestamp, type, sourceNodeId.getType() ) );

        final DirectedEdge targetEdge = new DirectedEdge( sourceNodeId, timestamp );


        final EdgeRowKey edgeRowKey = new EdgeRowKey( sourceNodeId, type, targetNodeId, edgeShardStrategy
                .getWriteShard( scope, sourceNodeId, timestamp, type, targetNodeId.getUuid().toString(),
                        targetNodeId.getType() ) );


        /**
         * write edges from source->target
         */

        op.writeEdge( sourceNodeEdgesCf, sourceRowKey, sourceEdge );

        op.writeEdge( sourceNodeTargetTypeCf, sourceRowKeyType, sourceEdge );


        /**
         * write edges from target<-source
         */
        op.writeEdge( targetNodeEdgesCf, targetRowKey, targetEdge );

        op.writeEdge( targetNodeSourceTypeCf, targetRowKeyType, targetEdge );


        /**
         * Write this in the timestamp log for this edge of source->target
         */
        op.writeVersion( graphEdgeVersionsCf, edgeRowKey, timestamp );
    }


    @Override
    public Iterator<MarkedEdge> getEdgeVersions( final ApplicationScope scope, final SearchByEdge search ) {
        ValidationUtils.validateApplicationScope( scope );
        EdgeUtils.validateSearchByEdge( search );

        final Id targetId = search.targetNode();
        final Id sourceId = search.sourceNode();
        final String type = search.getType();
        final long maxTimestamp = search.getMaxTimestamp();

        final EdgeSearcher<EdgeRowKey, Long, MarkedEdge> searcher =
                new EdgeSearcher<EdgeRowKey, Long, MarkedEdge>( scope, maxTimestamp, search.last(),
                        edgeShardStrategy.getReadShards( scope, sourceId, maxTimestamp, type ) ) {

                    @Override
                    protected Serializer<Long> getSerializer() {
                        return LONG_SERIALIZER;
                    }


                    @Override
                    public void setRange( final RangeBuilder builder ) {


                        if ( last.isPresent() ) {
                            super.setRange( builder );
                            return;
                        }

                        //start seeking at a value < our max version
                        builder.setStart( maxTimestamp );
                    }


                    @Override
                    protected EdgeRowKey generateRowKey( long shard ) {
                        return new EdgeRowKey( sourceId, type, targetId, shard );
                    }


                    @Override
                    protected Long getStartColumn( final Edge last ) {
                        return last.getTimestamp();
                    }


                    @Override
                    protected MarkedEdge createEdge( final Long column, final boolean marked ) {
                        return new SimpleMarkedEdge( sourceId, type, targetId, column.longValue(), marked );
                    }

                };

        return new ShardRowIterator<>( searcher, graphEdgeVersionsCf );
    }


    @Override
    public Iterator<MarkedEdge> getEdgesFromSource( final ApplicationScope scope, final SearchByEdgeType edgeType ) {

        ValidationUtils.validateApplicationScope( scope );
        EdgeUtils.validateSearchByEdgeType( edgeType );

        final Id sourceId = edgeType.getNode();
        final String type = edgeType.getType();
        final long maxTimestamp = edgeType.getMaxTimestamp();

        final EdgeSearcher<RowKey, DirectedEdge, MarkedEdge> searcher =
                new EdgeSearcher<RowKey, DirectedEdge, MarkedEdge>( scope, maxTimestamp, edgeType.last(),
                        edgeShardStrategy.getReadShards( scope, sourceId, maxTimestamp, type ) ) {


                    @Override
                    protected Serializer<DirectedEdge> getSerializer() {
                        return EDGE_SERIALIZER;
                    }


                    @Override
                    protected RowKey generateRowKey( long shard ) {
                        return new RowKey( sourceId, type, shard );
                    }


                    @Override
                    protected DirectedEdge getStartColumn( final Edge last ) {
                        return new DirectedEdge( last.getTargetNode(), last.getTimestamp() );
                    }


                    @Override
                    protected MarkedEdge createEdge( final DirectedEdge edge, final boolean marked ) {
                        return new SimpleMarkedEdge( sourceId, type, edge.id, edge.timestamp, marked );
                    }
                };

        return new ShardRowIterator<>( searcher, sourceNodeEdgesCf );
    }


    @Override
    public Iterator<MarkedEdge> getEdgesFromSourceByTargetType( final ApplicationScope scope,
                                                                final SearchByIdType edgeType ) {

        ValidationUtils.validateApplicationScope( scope );
        EdgeUtils.validateSearchByEdgeType( edgeType );

        final Id targetId = edgeType.getNode();
        final String type = edgeType.getType();
        final String targetType = edgeType.getIdType();
        final long maxTimestamp = edgeType.getMaxTimestamp();

        final EdgeSearcher<RowKeyType, DirectedEdge, MarkedEdge> searcher =
                new EdgeSearcher<RowKeyType, DirectedEdge, MarkedEdge>( scope, maxTimestamp, edgeType.last(),
                        edgeShardStrategy.getReadShards( scope, targetId, maxTimestamp, type, targetType ) ) {

                    @Override
                    protected Serializer<DirectedEdge> getSerializer() {
                        return EDGE_SERIALIZER;
                    }


                    @Override
                    protected RowKeyType generateRowKey( long shard ) {
                        return new RowKeyType( targetId, type, targetType, shard );
                    }


                    @Override
                    protected DirectedEdge getStartColumn( final Edge last ) {
                        return new DirectedEdge( last.getTargetNode(), last.getTimestamp() );
                    }


                    @Override
                    protected MarkedEdge createEdge( final DirectedEdge edge, final boolean marked ) {
                        return new SimpleMarkedEdge( targetId, type, edge.id, edge.timestamp, marked );
                    }
                };

        return new ShardRowIterator( searcher, sourceNodeTargetTypeCf );
    }


    @Override
    public Iterator<MarkedEdge> getEdgesToTarget( final ApplicationScope scope, final SearchByEdgeType edgeType ) {

        ValidationUtils.validateApplicationScope( scope );
        EdgeUtils.validateSearchByEdgeType( edgeType );

        final Id targetId = edgeType.getNode();
        final String type = edgeType.getType();
        final long maxTimestamp = edgeType.getMaxTimestamp();

        final EdgeSearcher<RowKey, DirectedEdge, MarkedEdge> searcher =
                new EdgeSearcher<RowKey, DirectedEdge, MarkedEdge>( scope, maxTimestamp, edgeType.last(),
                        edgeShardStrategy.getReadShards( scope, targetId, maxTimestamp, type ) ) {

                    @Override
                    protected Serializer<DirectedEdge> getSerializer() {
                        return EDGE_SERIALIZER;
                    }


                    @Override
                    protected RowKey generateRowKey( long shard ) {
                        return new RowKey( targetId, type, shard );
                    }


                    @Override
                    protected DirectedEdge getStartColumn( final Edge last ) {
                        return new DirectedEdge( last.getSourceNode(), last.getTimestamp() );
                    }


                    @Override
                    protected MarkedEdge createEdge( final DirectedEdge edge, final boolean marked ) {
                        return new SimpleMarkedEdge( edge.id, type, targetId, edge.timestamp, marked );
                    }
                };


        return new ShardRowIterator<>( searcher, targetNodeEdgesCf );
    }


    @Override
    public Iterator<MarkedEdge> getEdgesToTargetBySourceType( final ApplicationScope scope,
                                                              final SearchByIdType edgeType ) {

        ValidationUtils.validateApplicationScope( scope );
        EdgeUtils.validateSearchByEdgeType( edgeType );

        final Id targetId = edgeType.getNode();
        final String sourceType = edgeType.getIdType();
        final String type = edgeType.getType();
        final long maxTimestamp = edgeType.getMaxTimestamp();


        final EdgeSearcher<RowKeyType, DirectedEdge, MarkedEdge> searcher =
                new EdgeSearcher<RowKeyType, DirectedEdge, MarkedEdge>( scope, maxTimestamp, edgeType.last(),
                        edgeShardStrategy.getReadShards( scope, targetId, maxTimestamp, type, sourceType ) ) {
                    @Override
                    protected Serializer<DirectedEdge> getSerializer() {
                        return EDGE_SERIALIZER;
                    }


                    @Override
                    protected RowKeyType generateRowKey( final long shard ) {
                        return new RowKeyType( targetId, type, sourceType, shard );
                    }


                    @Override
                    protected DirectedEdge getStartColumn( final Edge last ) {
                        return new DirectedEdge( last.getTargetNode(), last.getTimestamp() );
                    }


                    @Override
                    protected MarkedEdge createEdge( final DirectedEdge edge, final boolean marked ) {
                        return new SimpleMarkedEdge( edge.id, type, targetId, edge.timestamp, marked );
                    }
                };

        return new ShardRowIterator<>( searcher, targetNodeSourceTypeCf );
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Arrays.asList( graphCf( sourceNodeEdgesCf ), graphCf( targetNodeEdgesCf ),
                graphCf( sourceNodeTargetTypeCf ), graphCf( targetNodeSourceTypeCf ),
                new MultiTennantColumnFamilyDefinition( graphEdgeVersionsCf, BytesType.class.getSimpleName(),
                        ColumnTypes.LONG_TYPE_REVERSED, BytesType.class.getSimpleName(),
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

        public final long timestamp;
        public final Id id;


        private DirectedEdge( final Id id, final long timestamp) {
            this.timestamp = timestamp;
            this.id = id;
        }
    }


    /**
     * Serializes to a source->target edge Note that we cannot set the edge type on de-serialization.  Only the target
     * Id and version.
     */
    private static class EdgeSerializer extends AbstractSerializer<DirectedEdge> {

        private static final IdColDynamicCompositeSerializer ID_COL_SERIALIZER = IdColDynamicCompositeSerializer.get();

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
            final long timestamp= composite.get( 0, LONG_SERIALIZER );


            //parse our id
            final Id id = ID_COL_SERIALIZER.fromComposite( composite, 1 );


            return new DirectedEdge( id, timestamp );
        }


        /**
         * Create the dynamic composite for this directed edge
         */
        private DynamicComposite createComposite( DirectedEdge edge, AbstractComposite.ComponentEquality equality ) {
            DynamicComposite composite = new DynamicComposite();

            //add our edge
            composite.addComponent( edge.timestamp, LONG_SERIALIZER, ColumnTypes.LONG_TYPE_REVERSED, equality );


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
        public final long shard;


        /**
         * Create a row key with the node and the edgeType
         */
        public RowKey( Id nodeId, String edgeType, final long shard ) {
            this( nodeId, EdgeHasher.createEdgeHash( edgeType ), shard );
        }


        /**
         * Create a new row key with the hash, should only be used in deserialization or internal callers.
         */
        protected RowKey( Id nodeId, long[] hash, final long shard ) {
            this.nodeId = nodeId;
            this.hash = hash;
            this.shard = shard;
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
        public RowKeyType( final Id nodeId, final String edgeType, final Id typeId, final long shard ) {
            this( nodeId, edgeType, typeId.getType(), shard );
        }


        /**
         * Create a row key with the node id in the row key, the edge type, adn the target type from the id
         */
        public RowKeyType( final Id nodeId, final String edgeType, final String targetType, final long shard ) {
            super( nodeId, EdgeHasher.createEdgeHash( edgeType, targetType ), shard );
        }


        /**
         * Internal use in de-serializing.  Should only be used in this case or by internal callers
         */
        private RowKeyType( final Id nodeId, final long[] hash, final long shard ) {
            super( nodeId, hash, shard );
        }
    }


    /**
     * Used to store row keys by sourceId, targetId and edgeType
     */
    private static class EdgeRowKey {
        public final Id sourceId;
        public final Id targetId;
        public final String edgeType;
        public final long shardId;


        private EdgeRowKey( final Id sourceId, final String edgeType, final Id targetId, final long shardId ) {
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.edgeType = edgeType;
            this.shardId = shardId;
        }
    }


    /**
     * Searcher to be used when performing the search.  Performs I/O transformation as well as parsing for the iterator.
     * If there are more row keys available to seek, the iterator will return true
     *
     * @param <R> The row type
     * @param <C> The column type
     * @param <T> The parsed return type
     */
    private static abstract class EdgeSearcher<R, C, T>
            implements ColumnParser<C, T>, Iterator<ScopedRowKey<ApplicationScope, R>> {

        protected final Optional<Edge> last;
        protected final long maxTimestamp;
        protected final ApplicationScope scope;
        protected final Iterator<Long> shards;


        protected EdgeSearcher( final ApplicationScope scope, final long maxTimestamp, final Optional<Edge> last,
                                final Iterator<Long> shards ) {
            this.scope = scope;
            this.maxTimestamp = maxTimestamp;
            this.last = last;
            this.shards = shards;
        }


        @Override
        public boolean hasNext() {
            return shards.hasNext();
        }


        @Override
        public ScopedRowKey<ApplicationScope, R> next() {
            return ScopedRowKey.fromKey( scope, generateRowKey( shards.next() ) );
        }


        @Override
        public void remove() {
            throw new UnsupportedOperationException( "Remove is unsupported" );
        }


        /**
         * Set the range on a search
         */
        public void setRange( final RangeBuilder builder ) {

            //set our start range since it was supplied to us
            if ( last.isPresent() ) {
                C sourceEdge = getStartColumn( last.get() );


                builder.setStart( sourceEdge, getSerializer() );
            }
            else {


            }
        }


        public boolean hasPage() {
            return last.isPresent();
        }


        @Override
        public T parseColumn( final Column<C> column ) {
            final C edge = column.getName();

            return createEdge( edge, column.getBooleanValue() );
        }


        /**
         * Get the column's serializer
         */
        protected abstract Serializer<C> getSerializer();


        /**
         * Create a row key for this search to use
         *
         * @param shard The shard to use in the row key
         */
        protected abstract R generateRowKey( final long shard );


        /**
         * Set the start column to begin searching from.  The last is provided
         */
        protected abstract C getStartColumn( final Edge last );


        /**
         * Create an edge to return to the user based on the directed edge provided
         *
         * @param column The column name
         * @param marked The marked flag in the column value
         */
        protected abstract T createEdge( final C column, final boolean marked );
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
            builder.addLong( key.shard );
        }


        @Override
        public RowKey fromComposite( final CompositeParser composite ) {

            final Id id = ID_SER.fromComposite( composite );
            final long[] hash = new long[] { composite.readLong(), composite.readLong() };
            final long shard = composite.readLong();


            return new RowKey( id, hash, shard );
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
            builder.addLong( keyType.shard );
        }


        @Override
        public RowKeyType fromComposite( final CompositeParser composite ) {

            final Id id = ID_SER.fromComposite( composite );
            final long[] hash = new long[] { composite.readLong(), composite.readLong() };
            final long shard = composite.readLong();

            return new RowKeyType( id, hash, shard );
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


    /**
     * Simple callback to perform puts and deletes with a common row setup code
     *
     * @param <R> The row key type
     */
    private static interface RowOp<R> {

        /**
         * Write the edge with the given data
         */
        void writeEdge( final MultiTennantColumnFamily<ApplicationScope, R, DirectedEdge> columnFamily, R rowKey,
                        DirectedEdge edge );

        /**
         * Perform the count on the edge
         */
        void countEdge( final Id rowId, long shardId, String... types );

        /**
         * Write the edge into the version cf
         */
        void writeVersion( final MultiTennantColumnFamily<ApplicationScope, EdgeRowKey, Long> columnFamily,
                           EdgeRowKey rowKey, long timestamp);
    }


    /**
     * Internal iterator to iterate over multiple row keys
     *
     * @param <R> The row type
     * @param <C> The column type
     * @param <T> The parsed return type
     */
    private class ShardRowIterator<R, C, T> implements Iterator<T> {

        private final EdgeSearcher<R, C, T> searcher;

        private final MultiTennantColumnFamily<ApplicationScope, R, C> cf;

        private Iterator<T> currentColumnIterator;


        private ShardRowIterator( final EdgeSearcher<R, C, T> searcher,
                                  final MultiTennantColumnFamily<ApplicationScope, R, C> cf ) {
            this.searcher = searcher;
            this.cf = cf;
        }


        @Override
        public boolean hasNext() {
            //we have more columns to return
            if ( currentColumnIterator != null && currentColumnIterator.hasNext() ) {
                return true;
            }

            /**
             * We have another row key, advance to it and re-check
             */
            if ( searcher.hasNext() ) {
                advanceRow();
                return hasNext();
            }

            //we have no more columns, and no more row keys, we're done
            return false;
        }


        @Override
        public T next() {
            if ( !hasNext() ) {
                throw new NoSuchElementException( "There are no more rows or columns left to advance" );
            }

            return currentColumnIterator.next();
        }


        @Override
        public void remove() {
            throw new UnsupportedOperationException( "Remove is unsupported" );
        }


        /**
         * Advance our iterator to the next row (assumes the check for row keys is elsewhere)
         */
        private void advanceRow() {

            /**
             * If the edge is present, we need to being seeking from this
             */

            final RangeBuilder rangeBuilder = new RangeBuilder().setLimit( graphFig.getScanPageSize() );


            //set the range into the search
            searcher.setRange( rangeBuilder );

            final ScopedRowKey<ApplicationScope, R> rowKey = searcher.next();


            RowQuery<ScopedRowKey<ApplicationScope, R>, C> query =
                    keyspace.prepareQuery( cf ).setConsistencyLevel( cassandraConfig.getReadCL() ).getKey( rowKey )
                            .autoPaginate( true ).withColumnRange( rangeBuilder.build() );


            currentColumnIterator =
                    new ColumnNameIterator<C, T>( query, searcher, searcher.hasPage() );
        }
    }
}
