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

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.collection.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.collection.cassandra.ColumnTypes;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.CassandraConfig;
import org.apache.usergrid.persistence.graph.serialization.impl.OrganizationScopedRowKeySerializer;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ColumnNameIterator;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ColumnParser;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardSerialization;
import org.apache.usergrid.persistence.model.entity.Id;

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
    private static final MultiTennantColumnFamily<OrganizationScope, EdgeRowKey, Long> EDGE_SHARDS =
            new MultiTennantColumnFamily<>( "Edge_Shards",
                    new OrganizationScopedRowKeySerializer<>( new EdgeRowKeySerializer() ), LongSerializer.get() );


    private static final byte HOLDER = 0x00;

    private static final LongColumnParser COLUMN_PARSER = new LongColumnParser();


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
    public MutationBatch writeEdgeMeta( final OrganizationScope scope, final Id nodeId, final long shard,
                                        final String... types ) {


        ValidationUtils.validateOrganizationScope( scope );
        ValidationUtils.verifyIdentity(nodeId);
        Preconditions.checkArgument( shard > -1, "shardId must be greater than -1" );
        Preconditions.checkNotNull( types );

        final EdgeRowKey key = new EdgeRowKey( nodeId, types );

        final ScopedRowKey rowKey = ScopedRowKey.fromKey( scope, key );

        final MutationBatch batch = keyspace.prepareMutationBatch();

        batch.withRow( EDGE_SHARDS, rowKey ).putColumn( shard, HOLDER );

        return batch;
    }


    @Override
    public Iterator<Long> getEdgeMetaData( final OrganizationScope scope, final Id nodeId, final Optional<Long> start,
                                           final String... types ) {
        /**
         * If the edge is present, we need to being seeking from this
         */

        final RangeBuilder rangeBuilder = new RangeBuilder().setLimit( cassandraConfig.getScanPageSize() );

        if ( start.isPresent() ) {
            rangeBuilder.setStart( start.get() );
        }

        final EdgeRowKey key = new EdgeRowKey( nodeId, types );

        final ScopedRowKey rowKey = ScopedRowKey.fromKey( scope, key );


        final RowQuery<ScopedRowKey<OrganizationScope, EdgeRowKey>, Long> query =
                keyspace.prepareQuery( EDGE_SHARDS ).setConsistencyLevel( cassandraConfig.getReadCL() ).getKey( rowKey )
                        .autoPaginate( true ).withColumnRange( rangeBuilder.build() );


        return new ColumnNameIterator<>( query, COLUMN_PARSER, false, graphFig.getReadTimeout() );
    }


    @Override
    public MutationBatch removeEdgeMeta( final OrganizationScope scope, final Id nodeId, final long shard,
                                         final String... types ) {

        ValidationUtils.validateOrganizationScope( scope );
              ValidationUtils.verifyIdentity(nodeId);
              Preconditions.checkArgument( shard > -1, "shard must be greater than -1" );
              Preconditions.checkNotNull( types );

        final EdgeRowKey key = new EdgeRowKey( nodeId, types );

        final ScopedRowKey rowKey = ScopedRowKey.fromKey( scope, key );

        final MutationBatch batch = keyspace.prepareMutationBatch();

        batch.withRow( EDGE_SHARDS, rowKey ).deleteColumn( shard );

        return batch;
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {


        return Collections.singleton(
                new MultiTennantColumnFamilyDefinition( EDGE_SHARDS, BytesType.class.getSimpleName(),
                        ColumnTypes.LONG_TYPE_REVERSED, BytesType.class.getSimpleName(),
                        MultiTennantColumnFamilyDefinition.CacheOption.KEYS ) );
    }


    private static class LongColumnParser implements ColumnParser<Long, Long> {

        @Override
        public Long parseColumn( final Column<Long> column ) {
            return column.getName();
        }
    }
}
