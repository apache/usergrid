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
package org.apache.usergrid.persistence.graph.serialization.impl.shard.count;


import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.CounterColumnType;

import org.apache.usergrid.persistence.core.astyanax.CassandraConfig;
import org.apache.usergrid.persistence.core.astyanax.ColumnTypes;
import org.apache.usergrid.persistence.core.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.serialize.EdgeShardRowKeySerializer;
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
import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.serializers.BooleanSerializer;


@Singleton
public class NodeShardCounterSerializationImpl implements NodeShardCounterSerialization {


    private static final ShardKeySerializer SHARD_KEY_SERIALIZER = new ShardKeySerializer();

    /**
     * Edge shards
     */
    private static final MultiTennantColumnFamily<ScopedRowKey<ShardKey>, Boolean> EDGE_SHARD_COUNTS =
            new MultiTennantColumnFamily<>( "Edge_Shard_Counts",
                    new ScopedRowKeySerializer<>( SHARD_KEY_SERIALIZER ), BooleanSerializer.get() );


    protected final Keyspace keyspace;
    protected final CassandraConfig cassandraConfig;
    protected final GraphFig graphFig;


    @Inject
    public NodeShardCounterSerializationImpl( final Keyspace keyspace, final CassandraConfig cassandraConfig,
                                            final GraphFig graphFig ) {
        this.keyspace = keyspace;
        this.cassandraConfig = cassandraConfig;
        this.graphFig = graphFig;
    }


    @Override
    public MutationBatch flush( final Counter counter ) {


        Preconditions.checkNotNull( counter, "counter must be specified" );


        final MutationBatch batch = keyspace.prepareMutationBatch();

        for ( Map.Entry<ShardKey, AtomicLong> entry : counter.getEntries() ) {

            final ShardKey key = entry.getKey();
            final long value = entry.getValue().get();


            final ScopedRowKey rowKey = ScopedRowKey.fromKey( key.scope.getApplication(), key );


            batch.withRow( EDGE_SHARD_COUNTS, rowKey ).incrementCounterColumn(true , value );
        }


        return batch;
    }


    @Override
    public long getCount( final ShardKey key ) {

        final ScopedRowKey rowKey = ScopedRowKey.fromKey( key.scope.getApplication(), key );


        OperationResult<Column<Boolean>> column = null;
        try {
            column = keyspace.prepareQuery( EDGE_SHARD_COUNTS ).getKey( rowKey ).getColumn( true ).execute();
        }
        //column not found, return 0
        catch ( NotFoundException nfe ) {
            return 0;
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to read from cassandra", e );
        }

        return column.getResult().getLongValue();
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Collections.singleton(
                new MultiTennantColumnFamilyDefinition( EDGE_SHARD_COUNTS, BytesType.class.getSimpleName(),
                        ColumnTypes.BOOLEAN, CounterColumnType.class.getSimpleName(),
                        MultiTennantColumnFamilyDefinition.CacheOption.ALL ) );
    }



    private static class ShardKeySerializer implements CompositeFieldSerializer<ShardKey> {


        private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();

        private static final EdgeShardRowKeySerializer EDGE_SHARD_ROW_KEY_SERIALIZER = EdgeShardRowKeySerializer.INSTANCE;


        @Override
        public void toComposite( final CompositeBuilder builder, final ShardKey key ) {

            ID_SER.toComposite( builder, key.scope.getApplication() );

            EDGE_SHARD_ROW_KEY_SERIALIZER.toComposite( builder, key.directedEdgeMeta );

            builder.addLong( key.shard.getShardIndex() );

            builder.addLong( key.shard.getCreatedTime() );
        }


        @Override
        public ShardKey fromComposite( final CompositeParser composite ) {

            final Id applicationId = ID_SER.fromComposite( composite );

            final ApplicationScope scope = new ApplicationScopeImpl( applicationId );

            final DirectedEdgeMeta directedEdgeMeta = EDGE_SHARD_ROW_KEY_SERIALIZER.fromComposite( composite );

            final long shardIndex = composite.readLong();

            final long shardCreatedTime = composite.readLong();

            return new ShardKey( scope, new Shard( shardIndex, shardCreatedTime, false ), directedEdgeMeta );
        }


    }

}
