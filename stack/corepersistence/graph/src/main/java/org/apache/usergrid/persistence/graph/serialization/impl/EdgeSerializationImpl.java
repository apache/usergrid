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


import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import javax.inject.Inject;

import com.google.common.base.Optional;
import org.apache.usergrid.persistence.core.astyanax.CassandraConfig;
import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.*;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByIdType;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeColumnFamilies;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardStrategy;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardGroupDeletion;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardedEdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.ShardGroupColumnIterator;
import org.apache.usergrid.persistence.graph.serialization.util.GraphValidation;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Serialization for edges.  Delegates partitioning to the sharding strategy.
 */
@Singleton
public class EdgeSerializationImpl implements EdgeSerialization {


    protected final Keyspace keyspace;
    protected final CassandraConfig cassandraConfig;
    protected final GraphFig graphFig;
    protected final EdgeShardStrategy edgeShardStrategy;
    protected final EdgeColumnFamilies edgeColumnFamilies;
    protected final ShardedEdgeSerialization shardedEdgeSerialization;
    protected final TimeService timeService;
    protected final ShardGroupDeletion shardGroupDeletion;


    @Inject
    public EdgeSerializationImpl( final Keyspace keyspace, final CassandraConfig cassandraConfig,
                                  final GraphFig graphFig, final EdgeShardStrategy edgeShardStrategy,
                                  final EdgeColumnFamilies edgeColumnFamilies,
                                  final ShardedEdgeSerialization shardedEdgeSerialization,
                                  final TimeService timeService, final ShardGroupDeletion shardGroupDeletion ) {



        checkNotNull( keyspace, "keyspace required" );
        checkNotNull( cassandraConfig, "cassandraConfig required" );
        checkNotNull( edgeShardStrategy, "edgeShardStrategy required" );
        checkNotNull( edgeColumnFamilies, "edgeColumnFamilies required" );
        checkNotNull( shardedEdgeSerialization, "shardedEdgeSerialization required" );
        checkNotNull( timeService, "timeService required" );
        checkNotNull( shardGroupDeletion, "shardGroupDeletion require");


        this.keyspace = keyspace;
        this.cassandraConfig = cassandraConfig;
        this.graphFig = graphFig;
        this.edgeShardStrategy = edgeShardStrategy;
        this.edgeColumnFamilies = edgeColumnFamilies;
        this.shardedEdgeSerialization = shardedEdgeSerialization;
        this.timeService = timeService;
        this.shardGroupDeletion = shardGroupDeletion;
    }


    @Override
    public MutationBatch writeEdge( final ApplicationScope scope, final MarkedEdge markedEdge, final UUID timestamp ) {

        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateEdge( markedEdge );
        ValidationUtils.verifyTimeUuid( timestamp, "timestamp" );

        final long now = timeService.getCurrentTime();
        final Id sourceNode = markedEdge.getSourceNode();
        final Id targetNode = markedEdge.getTargetNode();
        final String edgeType = markedEdge.getType();
        final long edgeTimestamp = markedEdge.getTimestamp();

        /**
         * Source write
         */
        final DirectedEdgeMeta sourceEdgeMeta = DirectedEdgeMeta.fromSourceNode( sourceNode, edgeType );

        final Collection<Shard> sourceWriteShards =
                edgeShardStrategy.getWriteShards( scope, edgeTimestamp, sourceEdgeMeta ).getWriteShards( now );

        final MutationBatch batch = shardedEdgeSerialization
                .writeEdgeFromSource( edgeColumnFamilies, scope, markedEdge, sourceWriteShards, sourceEdgeMeta,
                        timestamp );


        /**
         * Source with target  type write
         */
        final DirectedEdgeMeta sourceTargetTypeEdgeMeta =
                DirectedEdgeMeta.fromSourceNodeTargetType( sourceNode, edgeType, targetNode.getType() );

        final Collection<Shard> sourceTargetTypeWriteShards =
                edgeShardStrategy.getWriteShards( scope, edgeTimestamp, sourceTargetTypeEdgeMeta )
                                 .getWriteShards( now );

        batch.mergeShallow( shardedEdgeSerialization
                .writeEdgeFromSourceWithTargetType( edgeColumnFamilies, scope, markedEdge, sourceTargetTypeWriteShards,
                        sourceTargetTypeEdgeMeta, timestamp ) );


        /**
         * Target write
         *
         */

        final DirectedEdgeMeta targetEdgeMeta = DirectedEdgeMeta.fromTargetNode( targetNode, edgeType );

        final Collection<Shard> targetWriteShards =
                edgeShardStrategy.getWriteShards( scope, edgeTimestamp, targetEdgeMeta ).getWriteShards( now );

        batch.mergeShallow( shardedEdgeSerialization
                .writeEdgeToTarget( edgeColumnFamilies, scope, markedEdge, targetWriteShards, targetEdgeMeta,
                        timestamp ) );


        /**
         * Target with source type write
         */

        final DirectedEdgeMeta targetSourceTypeEdgeMeta =
                DirectedEdgeMeta.fromTargetNodeSourceType( targetNode, edgeType, sourceNode.getType() );

        final Collection<Shard> targetSourceTypeWriteShards =
                edgeShardStrategy.getWriteShards( scope, edgeTimestamp, targetSourceTypeEdgeMeta )
                                 .getWriteShards( now );

        batch.mergeShallow( shardedEdgeSerialization
                .writeEdgeToTargetWithSourceType( edgeColumnFamilies, scope, markedEdge, targetSourceTypeWriteShards,
                        targetSourceTypeEdgeMeta, timestamp ) );


        /**
         * Version write
         */

        final DirectedEdgeMeta edgeVersionsMeta = DirectedEdgeMeta.fromEdge( sourceNode, targetNode, edgeType );

        final Collection<Shard> edgeVersionsShards =
                edgeShardStrategy.getWriteShards( scope, edgeTimestamp, edgeVersionsMeta ).getWriteShards( now );

        batch.mergeShallow( shardedEdgeSerialization
                .writeEdgeVersions( edgeColumnFamilies, scope, markedEdge, edgeVersionsShards,
                        edgeVersionsMeta, timestamp ) );


        return batch;
    }


    @Override
    public MutationBatch deleteEdge( final ApplicationScope scope, final MarkedEdge markedEdge, final UUID timestamp ) {
        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateEdge( markedEdge );
        ValidationUtils.verifyTimeUuid( timestamp, "timestamp" );

        final long now = timeService.getCurrentTime();
        final Id sourceNode = markedEdge.getSourceNode();
        final Id targetNode = markedEdge.getTargetNode();
        final String edgeType = markedEdge.getType();
        final long edgeTimestamp = markedEdge.getTimestamp();

        /**
         * Source write
         */
        final DirectedEdgeMeta sourceEdgeMeta = DirectedEdgeMeta.fromSourceNode( sourceNode, edgeType );

        final Collection<Shard> sourceWriteShards =
                edgeShardStrategy.getWriteShards( scope, edgeTimestamp, sourceEdgeMeta ).getWriteShards( now );

        final MutationBatch batch = shardedEdgeSerialization
                .deleteEdgeFromSource( edgeColumnFamilies, scope, markedEdge, sourceWriteShards, sourceEdgeMeta,
                        timestamp );


        /**
         * Source with target  type write
         */
        final DirectedEdgeMeta sourceTargetTypeEdgeMeta =
                DirectedEdgeMeta.fromSourceNodeTargetType( sourceNode, edgeType, targetNode.getType() );

        final Collection<Shard> sourceTargetTypeWriteShards =
                edgeShardStrategy.getWriteShards( scope, edgeTimestamp, sourceTargetTypeEdgeMeta )
                                 .getWriteShards( now );

        batch.mergeShallow( shardedEdgeSerialization
                .deleteEdgeFromSourceWithTargetType( edgeColumnFamilies, scope, markedEdge, sourceTargetTypeWriteShards,
                        sourceTargetTypeEdgeMeta, timestamp ) );


        /**
         * Target write
         *
         */

        final DirectedEdgeMeta targetEdgeMeta = DirectedEdgeMeta.fromTargetNode( targetNode, edgeType );

        final Collection<Shard> targetWriteShards =
                edgeShardStrategy.getWriteShards( scope, edgeTimestamp, targetEdgeMeta ).getWriteShards( now );

        batch.mergeShallow( shardedEdgeSerialization
                .deleteEdgeToTarget( edgeColumnFamilies, scope, markedEdge, targetWriteShards, targetEdgeMeta,
                        timestamp ) );


        /**
         * Target with source type write
         */

        final DirectedEdgeMeta targetSourceTypeEdgeMeta =
                DirectedEdgeMeta.fromTargetNodeSourceType( targetNode, edgeType, sourceNode.getType() );

        final Collection<Shard> targetSourceTypeWriteShards =
                edgeShardStrategy.getWriteShards( scope, edgeTimestamp, targetSourceTypeEdgeMeta )
                                 .getWriteShards( now );

        batch.mergeShallow( shardedEdgeSerialization
                .deleteEdgeToTargetWithSourceType( edgeColumnFamilies, scope, markedEdge, targetSourceTypeWriteShards,
                        targetSourceTypeEdgeMeta, timestamp ) );


        /**
         * Version write
         */

        final DirectedEdgeMeta edgeVersionsMeta = DirectedEdgeMeta.fromEdge( sourceNode, targetNode, edgeType );

        final Collection<Shard> edgeVersionsShards =
                edgeShardStrategy.getWriteShards( scope, edgeTimestamp, edgeVersionsMeta ).getWriteShards( now );

        batch.mergeShallow( shardedEdgeSerialization
                .deleteEdgeVersions( edgeColumnFamilies, scope, markedEdge, edgeVersionsShards,
                        edgeVersionsMeta, timestamp ) );


        return batch;
    }


    @Override
    public Iterator<MarkedEdge> getEdgeVersions( final ApplicationScope scope, final SearchByEdge search ) {
        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateSearchByEdge( search );

        final Id targetId = search.targetNode();
        final Id sourceId = search.sourceNode();
        final String type = search.getType();
        final long maxTimestamp = search.getMaxTimestamp();

        final DirectedEdgeMeta versionMetaData = DirectedEdgeMeta.fromEdge( sourceId, targetId, type );


        final Iterator<ShardEntryGroup> readShards =
                edgeShardStrategy.getReadShards( scope, maxTimestamp, versionMetaData );


        //now create a result iterator with our iterator of read shards

        return new ShardGroupColumnIterator( scope, versionMetaData, shardGroupDeletion, readShards ) {
            @Override
            protected Iterator<MarkedEdge> getIterator( final Collection<Shard> readShards ) {
                return shardedEdgeSerialization.getEdgeVersions( edgeColumnFamilies, scope, search, readShards );
            }

            @Override
            protected Iterator<MarkedEdge> getIteratorFullRange( final Collection<Shard> readShards ) {

                final SearchByEdge searchFullRange = new SimpleSearchByEdge(
                    search.sourceNode(), search.getType(),search.targetNode(), Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING, Optional.absent());

                return shardedEdgeSerialization.getEdgeVersions( edgeColumnFamilies, scope, searchFullRange, readShards );
            }
        };
    }


    @Override
    public Iterator<MarkedEdge> getEdgesFromSource( final ApplicationScope scope, final SearchByEdgeType edgeType ) {


        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateSearchByEdgeType( edgeType );

        final Id sourceId = edgeType.getNode();
        final String type = edgeType.getType();
        final long maxTimestamp = edgeType.getMaxTimestamp();


        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromSourceNode( sourceId, type );


        final Iterator<ShardEntryGroup> readShards = edgeShardStrategy.getReadShards( scope, maxTimestamp, directedEdgeMeta );

        return new ShardGroupColumnIterator( scope, directedEdgeMeta, shardGroupDeletion, readShards ) {
            @Override
            protected Iterator<MarkedEdge> getIterator( final Collection<Shard> readShards ) {
                return shardedEdgeSerialization.getEdgesFromSource( edgeColumnFamilies, scope, edgeType, readShards );
            }

            @Override
            protected Iterator<MarkedEdge> getIteratorFullRange( final Collection<Shard> readShards ) {

                final SearchByEdgeType searchFullRange = new SimpleSearchByEdgeType(
                    edgeType.getNode(), edgeType.getType(), Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING, Optional.absent(), false );

                return shardedEdgeSerialization.getEdgesFromSource( edgeColumnFamilies, scope, searchFullRange, readShards );
            }
        };
    }


    @Override
    public Iterator<MarkedEdge> getEdgesFromSourceByTargetType( final ApplicationScope scope,
                                                                final SearchByIdType edgeType ) {

        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateSearchByIdType( edgeType );

        final Id sourceId = edgeType.getNode();
        final String type = edgeType.getType();
        final String targetType = edgeType.getIdType();
        final long maxTimestamp = edgeType.getMaxTimestamp();

        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromSourceNodeTargetType( sourceId, type, targetType );


        final Iterator<ShardEntryGroup> readShards = edgeShardStrategy.getReadShards( scope, maxTimestamp, directedEdgeMeta );


        return new ShardGroupColumnIterator( scope, directedEdgeMeta, shardGroupDeletion, readShards ) {
            @Override
            protected Iterator<MarkedEdge> getIterator( final Collection<Shard> readShards ) {
                return shardedEdgeSerialization
                        .getEdgesFromSourceByTargetType( edgeColumnFamilies, scope, edgeType, readShards );
            }

            @Override
            protected Iterator<MarkedEdge> getIteratorFullRange (final Collection<Shard> readShards) {

                final SearchByIdType edgeTypeFullRange = new SimpleSearchByIdType(
                    edgeType.getNode(), edgeType.getType(), Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                    edgeType.getIdType(), Optional.absent(), false );

                return shardedEdgeSerialization
                    .getEdgesFromSourceByTargetType( edgeColumnFamilies, scope, edgeTypeFullRange, readShards);
            }
        };
    }


    @Override
    public Iterator<MarkedEdge> getEdgesToTarget( final ApplicationScope scope, final SearchByEdgeType edgeType ) {

        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateSearchByEdgeType( edgeType );

        final Id targetId = edgeType.getNode();
        final String type = edgeType.getType();
        final long maxTimestamp = edgeType.getMaxTimestamp();


        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromTargetNode( targetId, type );


        final Iterator<ShardEntryGroup> readShards = edgeShardStrategy.getReadShards( scope, maxTimestamp, directedEdgeMeta );

        return new ShardGroupColumnIterator( scope, directedEdgeMeta, shardGroupDeletion, readShards ) {
            @Override
            protected Iterator<MarkedEdge> getIterator( final Collection<Shard> readShards ) {
                return shardedEdgeSerialization.getEdgesToTarget( edgeColumnFamilies, scope, edgeType, readShards );
            }

            @Override
            protected Iterator<MarkedEdge> getIteratorFullRange( final Collection<Shard> readShards ) {

                final SearchByEdgeType edgeTypeFullRange = new SimpleSearchByEdgeType(
                    edgeType.getNode(), edgeType.getType(), Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                    Optional.absent(), false );

                return shardedEdgeSerialization.getEdgesToTarget( edgeColumnFamilies, scope, edgeTypeFullRange, readShards );
            }

        };
    }


    @Override
    public Iterator<MarkedEdge> getEdgesToTargetBySourceType( final ApplicationScope scope,
                                                              final SearchByIdType edgeType ) {

        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateSearchByIdType( edgeType );

        final Id targetId = edgeType.getNode();
        final String sourceType = edgeType.getIdType();
        final String type = edgeType.getType();
        final long maxTimestamp = edgeType.getMaxTimestamp();


        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromTargetNodeSourceType( targetId, type, sourceType );


        final Iterator<ShardEntryGroup> readShards = edgeShardStrategy.getReadShards( scope, maxTimestamp, directedEdgeMeta );


        return new ShardGroupColumnIterator( scope, directedEdgeMeta, shardGroupDeletion, readShards ) {
            @Override
            protected Iterator<MarkedEdge> getIterator( final Collection<Shard> readShards ) {
                return shardedEdgeSerialization
                        .getEdgesToTargetBySourceType( edgeColumnFamilies, scope, edgeType, readShards );
            }

            @Override
            protected Iterator<MarkedEdge> getIteratorFullRange( final Collection<Shard> readShards ) {

                final SearchByIdType edgeTypeFullRange = new SimpleSearchByIdType(
                    edgeType.getNode(), edgeType.getType(), Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                    edgeType.getIdType(), Optional.absent(), false );

                return shardedEdgeSerialization.getEdgesToTargetBySourceType( edgeColumnFamilies, scope, edgeTypeFullRange, readShards);

            }
        };
    }
}
