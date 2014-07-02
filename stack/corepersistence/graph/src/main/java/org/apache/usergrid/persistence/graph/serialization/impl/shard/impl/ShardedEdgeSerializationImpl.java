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


import java.util.Iterator;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.usergrid.persistence.core.astyanax.CassandraConfig;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.impl.SimpleMarkedEdge;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdge;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeColumnFamilies;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeRowKey;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardStrategy;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.RowKey;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.RowKeyType;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardedEdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.util.GraphValidation;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.util.RangeBuilder;

import static com.google.common.base.Preconditions.checkNotNull;


@Singleton
public class ShardedEdgeSerializationImpl implements ShardedEdgeSerialization {

    protected final Keyspace keyspace;
    protected final CassandraConfig cassandraConfig;
    protected final GraphFig graphFig;
    protected final EdgeShardStrategy writeEdgeShardStrategy;
    protected final TimeService timeService;


    @Inject
    public ShardedEdgeSerializationImpl( final Keyspace keyspace, final CassandraConfig cassandraConfig,
                                         final GraphFig graphFig, final EdgeShardStrategy writeEdgeShardStrategy,
                                         final TimeService timeService ) {


        checkNotNull( "keyspace required", keyspace );
        checkNotNull( "cassandraConfig required", cassandraConfig );
        checkNotNull( "consistencyFig required", graphFig );
        checkNotNull( "writeEdgeShardStrategy required", writeEdgeShardStrategy );
        checkNotNull( "timeService required", timeService );


        this.keyspace = keyspace;
        this.cassandraConfig = cassandraConfig;
        this.graphFig = graphFig;
        this.writeEdgeShardStrategy = writeEdgeShardStrategy;
        this.timeService = timeService;
    }


    @Override
    public MutationBatch writeEdge( final EdgeColumnFamilies columnFamilies, final ApplicationScope scope,
                                    final MarkedEdge markedEdge, final UUID timestamp  ) {
        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateEdge( markedEdge );
        ValidationUtils.verifyTimeUuid( timestamp, "timestamp" );


        final MutationBatch batch = keyspace.prepareMutationBatch().withConsistencyLevel( cassandraConfig.getWriteCL() )
                                            .withTimestamp( timestamp.timestamp() );

        final boolean isDeleted = markedEdge.isDeleted();


        doWrite( columnFamilies, scope, markedEdge, new RowOp<RowKey>() {
            @Override
            public void writeEdge( final MultiTennantColumnFamily<ApplicationScope, RowKey, DirectedEdge> columnFamily,
                                   final RowKey rowKey, final DirectedEdge edge ) {
                batch.withRow( columnFamily, ScopedRowKey.fromKey( scope, rowKey ) ).putColumn( edge, isDeleted );
            }


            @Override
            public void countEdge( final Shard shard, final DirectedEdgeMeta directedEdgeMeta ) {
               if(!isDeleted) {
                   writeEdgeShardStrategy.increment( scope, shard, 1,  directedEdgeMeta );
               }
            }



            @Override
            public void writeVersion( final MultiTennantColumnFamily<ApplicationScope, EdgeRowKey, Long> columnFamily,
                                      final EdgeRowKey rowKey, final long timestamp ) {
                batch.withRow( columnFamily, ScopedRowKey.fromKey( scope, rowKey ) ).putColumn( timestamp, isDeleted );
            }
        } );


        return batch;
    }


    @Override
    public MutationBatch deleteEdge( final EdgeColumnFamilies columnFamilies, final ApplicationScope scope,
                                     final MarkedEdge markedEdge, final UUID timestamp ) {
        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateEdge( markedEdge );
        ValidationUtils.verifyTimeUuid( timestamp, "timestamp" );


        final MutationBatch batch = keyspace.prepareMutationBatch().withConsistencyLevel( cassandraConfig.getWriteCL() )
                                            .withTimestamp( timestamp.timestamp() );


        doWrite( columnFamilies, scope, markedEdge, new RowOp<RowKey>() {
            @Override
            public void writeEdge( final MultiTennantColumnFamily<ApplicationScope, RowKey, DirectedEdge> columnFamily,
                                   final RowKey rowKey, final DirectedEdge edge ) {
                batch.withRow( columnFamily, ScopedRowKey.fromKey( scope, rowKey ) ).deleteColumn( edge );
            }


            @Override
            public void countEdge(  final Shard shard, final DirectedEdgeMeta directedEdgeMeta ) {
                writeEdgeShardStrategy.increment( scope,  shard, -1, directedEdgeMeta );
            }


            @Override
            public void writeVersion( final MultiTennantColumnFamily<ApplicationScope, EdgeRowKey, Long> columnFamily,
                                      final EdgeRowKey rowKey, final long timestamp ) {
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
    private void doWrite( final EdgeColumnFamilies columnFamilies, final ApplicationScope scope, final MarkedEdge edge,
                          final RowOp op ) {
        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateEdge( edge );

        final Id sourceNodeId = edge.getSourceNode();
        final String sourceNodeType = sourceNodeId.getType();
        final Id targetNodeId = edge.getTargetNode();
        final String targetNodeType = targetNodeId.getType();
        final long timestamp = edge.getTimestamp();
        final String type = edge.getType();


        /**
         * Key in the serializers based on the edge
         */


        /**
         * write edges from source->target
         */


        final long time = timeService.getCurrentTime();

        final DirectedEdge sourceEdge = new DirectedEdge( targetNodeId, timestamp );

        final DirectedEdgeMeta sourceEdgeMeta =  DirectedEdgeMeta.fromSourceNode( sourceNodeId, type );



        final ShardEntryGroup sourceRowKeyShard =
                writeEdgeShardStrategy.getWriteShards( scope, timestamp, sourceEdgeMeta );

        final MultiTennantColumnFamily<ApplicationScope, RowKey, DirectedEdge> sourceCf =
                columnFamilies.getSourceNodeCfName();


        for ( Shard shard : sourceRowKeyShard.getWriteShards(time) ) {

            final long shardId = shard.getShardIndex();
            final RowKey sourceRowKey = new RowKey( sourceNodeId, type, shardId );
            op.writeEdge( sourceCf, sourceRowKey, sourceEdge );
            op.countEdge( shard, sourceEdgeMeta );
        }



        final DirectedEdgeMeta sourceEdgeTargetTypeMeta =  DirectedEdgeMeta.fromSourceNodeTargetType( sourceNodeId,
                type, targetNodeType );


        final ShardEntryGroup sourceWithTypeRowKeyShard = writeEdgeShardStrategy
                .getWriteShards( scope, timestamp, sourceEdgeTargetTypeMeta );

        final MultiTennantColumnFamily<ApplicationScope, RowKeyType, DirectedEdge> targetCf =
                columnFamilies.getSourceNodeTargetTypeCfName();

        for ( Shard shard : sourceWithTypeRowKeyShard.getWriteShards(time) ) {

            final long shardId = shard.getShardIndex();
            final RowKeyType sourceRowKeyType = new RowKeyType( sourceNodeId, type, targetNodeId, shardId );

            op.writeEdge( targetCf, sourceRowKeyType, sourceEdge );
            op.countEdge( shard, sourceEdgeTargetTypeMeta );
        }


        /**
         * write edges from target<-source
         */

        final DirectedEdge targetEdge = new DirectedEdge( sourceNodeId, timestamp );

        final DirectedEdgeMeta targetEdgeMeta =  DirectedEdgeMeta.fromTargetNode( targetNodeId, type );



        final ShardEntryGroup targetRowKeyShard =
                writeEdgeShardStrategy.getWriteShards( scope, timestamp, targetEdgeMeta );

        final MultiTennantColumnFamily<ApplicationScope, RowKey, DirectedEdge> sourceByTargetCf =
                columnFamilies.getTargetNodeCfName();

        for ( Shard shard : targetRowKeyShard.getWriteShards(time) ) {
            final long shardId = shard.getShardIndex();
            final RowKey targetRowKey = new RowKey( targetNodeId, type, shardId );

            op.writeEdge( sourceByTargetCf, targetRowKey, targetEdge );
            op.countEdge( shard, targetEdgeMeta );
        }



        final DirectedEdgeMeta targetEdgeSourceTypeMeta =  DirectedEdgeMeta.fromTargetNodeSourceType( targetNodeId, type, sourceNodeType );


        final ShardEntryGroup targetWithTypeRowKeyShard = writeEdgeShardStrategy
                .getWriteShards( scope, timestamp, targetEdgeSourceTypeMeta );

        final MultiTennantColumnFamily<ApplicationScope, RowKeyType, DirectedEdge> targetBySourceCf =
                columnFamilies.getTargetNodeSourceTypeCfName();


        for ( Shard shard : targetWithTypeRowKeyShard.getWriteShards(time) ) {

            final long shardId = shard.getShardIndex();

            final RowKeyType targetRowKeyType = new RowKeyType( targetNodeId, type, sourceNodeId, shardId );


            op.writeEdge( targetBySourceCf, targetRowKeyType, targetEdge );
            op.countEdge( shard, targetEdgeSourceTypeMeta );
        }

        /**
         * Always a 0l shard, we're hard limiting 2b timestamps for the same edge
         */
        final EdgeRowKey edgeRowKey = new EdgeRowKey( sourceNodeId, type, targetNodeId, 0l );


        /**
         * Write this in the timestamp log for this edge of source->target
         */
        op.writeVersion( columnFamilies.getGraphEdgeVersions(), edgeRowKey, timestamp );
    }


    @Override
    public Iterator<MarkedEdge> getEdgeVersions( final EdgeColumnFamilies columnFamilies, final ApplicationScope scope,
                                                 final SearchByEdge search, final Iterator<ShardEntryGroup> shards ) {
        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateSearchByEdge( search );

        final Id targetId = search.targetNode();
        final Id sourceId = search.sourceNode();
        final String type = search.getType();
        final long maxTimestamp = search.getMaxTimestamp();
        final MultiTennantColumnFamily<ApplicationScope, EdgeRowKey, Long> columnFamily =
                columnFamilies.getGraphEdgeVersions();
        final Serializer<Long> serializer = columnFamily.getColumnSerializer();

        final EdgeSearcher<EdgeRowKey, Long, MarkedEdge> searcher =
                new EdgeSearcher<EdgeRowKey, Long, MarkedEdge>( scope, maxTimestamp, search.last(), shards ) {

                    @Override
                    protected Serializer<Long> getSerializer() {
                        return serializer;
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


                    @Override
                    public int compare( final MarkedEdge o1, final MarkedEdge o2 ) {
                        return Long.compare( o1.getTimestamp(), o2.getTimestamp() );
                    }
                };

        return new ShardRowIterator<>( searcher, columnFamily, keyspace, cassandraConfig.getReadCL(),
                graphFig.getScanPageSize() );
    }


    @Override
    public Iterator<MarkedEdge> getEdgesFromSource( final EdgeColumnFamilies columnFamilies,
                                                    final ApplicationScope scope, final SearchByEdgeType edgeType,
                                                    final Iterator<ShardEntryGroup> shards ) {

        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateSearchByEdgeType( edgeType );

        final Id sourceId = edgeType.getNode();
        final String type = edgeType.getType();
        final long maxTimestamp = edgeType.getMaxTimestamp();
        final MultiTennantColumnFamily<ApplicationScope, RowKey, DirectedEdge> columnFamily =
                columnFamilies.getSourceNodeCfName();
        final Serializer<DirectedEdge> serializer = columnFamily.getColumnSerializer();


        final SourceEdgeSearcher<RowKey, DirectedEdge, MarkedEdge> searcher =
                new SourceEdgeSearcher<RowKey, DirectedEdge, MarkedEdge>( scope, maxTimestamp, edgeType.last(), shards ) {


                    @Override
                    protected Serializer<DirectedEdge> getSerializer() {
                        return serializer;
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


        return new ShardRowIterator<>( searcher, columnFamily, keyspace, cassandraConfig.getReadCL(),
                graphFig.getScanPageSize() );
    }


    @Override
    public Iterator<MarkedEdge> getEdgesFromSourceByTargetType( final EdgeColumnFamilies columnFamilies,
                                                                final ApplicationScope scope,
                                                                final SearchByIdType edgeType,
                                                                final Iterator<ShardEntryGroup> shards ) {

        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateSearchByEdgeType( edgeType );

        final Id targetId = edgeType.getNode();
        final String type = edgeType.getType();
        final String targetType = edgeType.getIdType();
        final long maxTimestamp = edgeType.getMaxTimestamp();
        final MultiTennantColumnFamily<ApplicationScope, RowKeyType, DirectedEdge> columnFamily =
                columnFamilies.getSourceNodeTargetTypeCfName();
        final Serializer<DirectedEdge> serializer = columnFamily.getColumnSerializer();


        final SourceEdgeSearcher<RowKeyType, DirectedEdge, MarkedEdge> searcher =
                new SourceEdgeSearcher<RowKeyType, DirectedEdge, MarkedEdge>( scope, maxTimestamp, edgeType.last(), shards ) {

                    @Override
                    protected Serializer<DirectedEdge> getSerializer() {
                        return serializer;
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

        return new ShardRowIterator( searcher, columnFamily, keyspace, cassandraConfig.getReadCL(),
                graphFig.getScanPageSize() );
    }




    @Override
    public Iterator<MarkedEdge> getEdgesToTarget( final EdgeColumnFamilies columnFamilies, final ApplicationScope scope,
                                                  final SearchByEdgeType edgeType,
                                                  final Iterator<ShardEntryGroup> shards ) {
        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateSearchByEdgeType( edgeType );

        final Id targetId = edgeType.getNode();
        final String type = edgeType.getType();
        final long maxTimestamp = edgeType.getMaxTimestamp();
        final MultiTennantColumnFamily<ApplicationScope, RowKey, DirectedEdge> columnFamily =
                columnFamilies.getTargetNodeCfName();
        final Serializer<DirectedEdge> serializer = columnFamily.getColumnSerializer();

        final TargetEdgeSearcher<RowKey, DirectedEdge, MarkedEdge> searcher =
                new TargetEdgeSearcher<RowKey, DirectedEdge, MarkedEdge>( scope, maxTimestamp, edgeType.last(), shards ) {

                    @Override
                    protected Serializer<DirectedEdge> getSerializer() {
                        return serializer;
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


        return new ShardRowIterator<>( searcher, columnFamily, keyspace, cassandraConfig.getReadCL(),
                graphFig.getScanPageSize() );
    }


    @Override
    public Iterator<MarkedEdge> getEdgesToTargetBySourceType( final EdgeColumnFamilies columnFamilies,
                                                              final ApplicationScope scope,
                                                              final SearchByIdType edgeType,
                                                              final Iterator<ShardEntryGroup> shards ) {

        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateSearchByEdgeType( edgeType );

        final Id targetId = edgeType.getNode();
        final String sourceType = edgeType.getIdType();
        final String type = edgeType.getType();
        final long maxTimestamp = edgeType.getMaxTimestamp();
        final MultiTennantColumnFamily<ApplicationScope, RowKeyType, DirectedEdge> columnFamily =
                columnFamilies.getTargetNodeSourceTypeCfName();
        final Serializer<DirectedEdge> serializer = columnFamily.getColumnSerializer();


        final TargetEdgeSearcher<RowKeyType, DirectedEdge, MarkedEdge> searcher =
                new TargetEdgeSearcher<RowKeyType, DirectedEdge, MarkedEdge>( scope, maxTimestamp, edgeType.last(), shards ) {
                    @Override
                    protected Serializer<DirectedEdge> getSerializer() {
                        return serializer;
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

        return new ShardRowIterator<>( searcher, columnFamily, keyspace, cassandraConfig.getReadCL(),
                graphFig.getScanPageSize() );
    }


    /**
     * Class for performing searched on rows based on source id
     */
    private static abstract class SourceEdgeSearcher<R, C, T extends Edge> extends EdgeSearcher<R, C, T>{

        protected SourceEdgeSearcher( final ApplicationScope scope, final long maxTimestamp, final Optional<Edge> last,
                                      final Iterator<ShardEntryGroup> shards ) {
            super( scope, maxTimestamp, last, shards );
        }


        public int compare( final T o1, final T o2 ) {
            int compare = Long.compare(o1.getTimestamp(), o2.getTimestamp());

            if(compare == 0){
                compare = o1.getTargetNode().compareTo( o2.getTargetNode());
            }

            return compare;
        }


    }


    /**
     * Class for performing searched on rows based on target id
     */
    private static abstract class TargetEdgeSearcher<R, C, T extends Edge> extends EdgeSearcher<R, C, T>{

        protected TargetEdgeSearcher( final ApplicationScope scope, final long maxTimestamp, final Optional<Edge> last,
                                      final Iterator<ShardEntryGroup> shards ) {
            super( scope, maxTimestamp, last, shards );
        }


        public int compare( final T o1, final T o2 ) {
            int compare = Long.compare(o1.getTimestamp(), o2.getTimestamp());

            if(compare == 0){
                compare = o1.getTargetNode().compareTo( o2.getTargetNode());
            }

            return compare;
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
        void writeEdge( final MultiTennantColumnFamily<ApplicationScope, R, DirectedEdge> columnFamily, final  R rowKey,
                        final DirectedEdge edge );

        /**
         * Perform the count on the edge
         */
        void countEdge( final Shard shard, final DirectedEdgeMeta directedEdgeMeta);

        /**
         * Write the edge into the version cf
         */
        void writeVersion( final MultiTennantColumnFamily<ApplicationScope, EdgeRowKey, Long> columnFamily,
                           final EdgeRowKey rowKey, long timestamp );
    }
}
