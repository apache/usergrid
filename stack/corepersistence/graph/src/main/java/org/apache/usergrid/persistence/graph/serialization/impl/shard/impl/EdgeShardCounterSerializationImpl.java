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

import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.CounterColumnType;
import org.apache.cassandra.db.marshal.LongType;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.collection.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.collection.cassandra.ColumnTypes;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.CassandraConfig;
import org.apache.usergrid.persistence.graph.serialization.impl.OrganizationScopedRowKeySerializer;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardCounterSerialization;
import org.apache.usergrid.persistence.graph.serialization.util.EdgeUtils;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.serializers.LongSerializer;


@Singleton
public class EdgeShardCounterSerializationImpl implements EdgeShardCounterSerialization {


    /**
     * Edge shards
     */
    private static final MultiTennantColumnFamily<OrganizationScope, EdgeRowKey, Long> EDGE_SHARD_COUNTS =
            new MultiTennantColumnFamily<>( "Edge_Shard_Counts",
                    new OrganizationScopedRowKeySerializer<>( new EdgeRowKeySerializer() ), LongSerializer.get() );


    protected final Keyspace keyspace;
    protected final CassandraConfig cassandraConfig;
    protected final GraphFig graphFig;


    @Inject
    public EdgeShardCounterSerializationImpl( final Keyspace keyspace, final CassandraConfig cassandraConfig,
                                              final GraphFig graphFig ) {
        this.keyspace = keyspace;
        this.cassandraConfig = cassandraConfig;
        this.graphFig = graphFig;
    }


    @Override
    public MutationBatch writeMetaDataLog( final OrganizationScope scope, final Id nodeId, final long shardId,
                                           final long count, final String... types ) {

        ValidationUtils.validateOrganizationScope( scope );
        ValidationUtils.verifyIdentity(nodeId);
        Preconditions.checkArgument( shardId > -1, "shardId must be greater than -1" );
        Preconditions.checkNotNull( types );

        final EdgeRowKey key = new EdgeRowKey( nodeId, types );

        final ScopedRowKey rowKey = ScopedRowKey.fromKey( scope, key );

        final MutationBatch batch = keyspace.prepareMutationBatch();

        batch.withRow( EDGE_SHARD_COUNTS, rowKey ).incrementCounterColumn( shardId, count );

        return batch;
    }


    @Override
    public long getCount( final OrganizationScope scope, final Id nodeId, final long shardId, final String... types ) {


        ValidationUtils.validateOrganizationScope( scope );
        ValidationUtils.verifyIdentity(nodeId);
        Preconditions.checkArgument( shardId > -1, "shardId must be greater than -1" );
        Preconditions.checkNotNull( types );


        final EdgeRowKey key = new EdgeRowKey( nodeId, types );

        final ScopedRowKey rowKey = ScopedRowKey.fromKey( scope, key );


        try {
            OperationResult<Column<Long>> column =
                    keyspace.prepareQuery( EDGE_SHARD_COUNTS ).getKey( rowKey ).getColumn( shardId ).execute();

            return column.getResult().getLongValue();
        }
        //column not found, return 0
        catch ( NotFoundException nfe ) {
            return 0;
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "An error occurred connecting to cassandra", e );
        }

    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Collections.singleton(
                new MultiTennantColumnFamilyDefinition( EDGE_SHARD_COUNTS, BytesType.class.getSimpleName(),
                        ColumnTypes.LONG_TYPE_REVERSED,  CounterColumnType.class.getSimpleName(),
                        MultiTennantColumnFamilyDefinition.CacheOption.KEYS ) );
    }


}
