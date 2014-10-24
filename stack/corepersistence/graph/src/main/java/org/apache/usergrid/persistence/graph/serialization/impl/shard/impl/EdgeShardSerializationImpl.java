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

package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.cassandra.db.marshal.BytesType;

import org.apache.usergrid.persistence.core.astyanax.CassandraConfig;
import org.apache.usergrid.persistence.core.astyanax.ColumnNameIterator;
import org.apache.usergrid.persistence.core.astyanax.ColumnParser;
import org.apache.usergrid.persistence.core.astyanax.ColumnTypes;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.serialize.EdgeShardRowKeySerializer;
import org.apache.usergrid.persistence.graph.serialization.util.GraphValidation;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.query.RowQuery;
import com.netflix.astyanax.serializers.LongSerializer;
import com.netflix.astyanax.util.RangeBuilder;


@Singleton
public class EdgeShardSerializationImpl implements EdgeShardSerialization {

    /**
     * Edge shards
     */
    private static final MultiTennantColumnFamily<ScopedRowKey<DirectedEdgeMeta>, Long> EDGE_SHARDS =
            new MultiTennantColumnFamily<>( "Edge_Shards",
                    new ScopedRowKeySerializer<>( EdgeShardRowKeySerializer.INSTANCE ), LongSerializer.get() );


    private static final ShardColumnParser COLUMN_PARSER = new ShardColumnParser();


    protected final Keyspace keyspace;
    protected final CassandraConfig cassandraConfig;
    protected final GraphFig graphFig;


    @Inject
    public EdgeShardSerializationImpl( final Keyspace keyspace, final CassandraConfig cassandraConfig,
                                       final GraphFig graphFig ) {
        this.keyspace = keyspace;
        this.cassandraConfig = cassandraConfig;
        this.graphFig = graphFig;
    }


    @Override
    public MutationBatch writeShardMeta( final ApplicationScope scope,
                                         final Shard shard,   final DirectedEdgeMeta metaData) {

        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateDirectedEdgeMeta( metaData );

        Preconditions.checkNotNull( metaData, "metadata must be present" );

        Preconditions.checkNotNull( shard );
        Preconditions.checkArgument( shard.getShardIndex() > -1, "shardid must be greater than -1" );
        Preconditions.checkArgument( shard.getCreatedTime() > -1, "createdTime must be greater than -1" );

        final ScopedRowKey rowKey = ScopedRowKey.fromKey( scope.getApplication(), metaData );

        final MutationBatch batch = keyspace.prepareMutationBatch();

        batch.withTimestamp( shard.getCreatedTime() ).withRow( EDGE_SHARDS, rowKey )
             .putColumn( shard.getShardIndex(), shard.isCompacted() );

        return batch;
    }


    @Override
    public Iterator<Shard> getShardMetaData( final ApplicationScope scope,
                                             final Optional<Shard> start,   final DirectedEdgeMeta metaData  ) {

        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateDirectedEdgeMeta( metaData );


        Preconditions.checkNotNull( metaData, "metadata must be present" );

        /**
         * If the edge is present, we need to being seeking from this
         */

        final RangeBuilder rangeBuilder = new RangeBuilder().setLimit( graphFig.getScanPageSize() );

        if ( start.isPresent() ) {
            final Shard shard = start.get();
            GraphValidation.valiateShard( shard );
            rangeBuilder.setStart( shard.getShardIndex() );
        }


        final ScopedRowKey rowKey = ScopedRowKey.fromKey( scope.getApplication(), metaData );


        final RowQuery<ScopedRowKey<DirectedEdgeMeta>, Long> query =
                keyspace.prepareQuery( EDGE_SHARDS ).setConsistencyLevel( cassandraConfig.getReadCL() ).getKey( rowKey )
                        .autoPaginate( true ).withColumnRange( rangeBuilder.build() );


        return new ColumnNameIterator<>( query, COLUMN_PARSER, false );
    }


    @Override
    public MutationBatch removeShardMeta( final ApplicationScope scope,
                                          final Shard shard,   final DirectedEdgeMeta metaData) {

        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.valiateShard( shard );
        GraphValidation.validateDirectedEdgeMeta( metaData );



        final ScopedRowKey rowKey = ScopedRowKey.fromKey( scope.getApplication(), metaData );

        final MutationBatch batch = keyspace.prepareMutationBatch();

        batch.withRow( EDGE_SHARDS, rowKey ).deleteColumn( shard.getShardIndex() );

        return batch;
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {


        return Collections.singleton(
                new MultiTennantColumnFamilyDefinition( EDGE_SHARDS, BytesType.class.getSimpleName(),
                        ColumnTypes.LONG_TYPE_REVERSED, BytesType.class.getSimpleName(),
                        MultiTennantColumnFamilyDefinition.CacheOption.KEYS ) );
    }






    private static class ShardColumnParser implements ColumnParser<Long, Shard> {

        @Override
        public Shard parseColumn( final Column<Long> column ) {
            return new Shard( column.getName(), column.getTimestamp(), column.getBooleanValue() );
        }
    }
}
