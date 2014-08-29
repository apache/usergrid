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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cassandra.db.marshal.BooleanType;
import org.apache.cassandra.db.marshal.BytesType;

import org.apache.usergrid.persistence.core.astyanax.CassandraConfig;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.OrganizationScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.hystrix.HystrixCassandra;
import org.apache.usergrid.persistence.core.migration.Migration;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.exception.GraphRuntimeException;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.graph.serialization.util.GraphValidation;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.ColumnFamilyQuery;
import com.netflix.astyanax.serializers.BooleanSerializer;


/**
 *
 *
 */
@Singleton
public class NodeSerializationImpl implements NodeSerialization, Migration {


    //Row key by node id.
    private static final IdRowCompositeSerializer ROW_SERIALIZER = IdRowCompositeSerializer.get();

    private static final BooleanSerializer BOOLEAN_SERIALIZER = BooleanSerializer.get();

    /**
     * Column name is always just "true"
     */
    private static final boolean COLUMN_NAME = true;


    /**
     * Columns are always a byte, and the entire value is contained within a row key.  This is intentional This allows
     * us to make heavy use of Cassandra's bloom filters, as well as key caches. Since most nodes will only exist for a
     * short amount of time in this CF, we'll most likely have them in the key cache, and we'll also bounce from the
     * BloomFilter on read.  This means our performance will be no worse than checking a distributed cache in RAM for
     * the existence of a marked node.
     */
    private static final MultiTennantColumnFamily<ApplicationScope, Id, Boolean> GRAPH_DELETE =
            new MultiTennantColumnFamily<ApplicationScope, Id, Boolean>( "Graph_Marked_Nodes",
                    new OrganizationScopedRowKeySerializer<Id>( ROW_SERIALIZER ), BOOLEAN_SERIALIZER );


    protected final Keyspace keyspace;
    protected final CassandraConfig fig;


    @Inject
    public NodeSerializationImpl( final Keyspace keyspace, final CassandraConfig fig ) {
        this.keyspace = keyspace;
        this.fig = fig;
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Collections.singleton(
                new MultiTennantColumnFamilyDefinition( GRAPH_DELETE, BytesType.class.getSimpleName(),
                        BooleanType.class.getSimpleName(), BytesType.class.getSimpleName(),
                        MultiTennantColumnFamilyDefinition.CacheOption.ALL ) );
    }


    @Override
    public MutationBatch mark( final ApplicationScope scope, final Id node, final long timestamp ) {
        ValidationUtils.validateApplicationScope( scope );
        ValidationUtils.verifyIdentity( node );
        GraphValidation.validateTimestamp( timestamp, "timestamp" );

        MutationBatch batch = keyspace.prepareMutationBatch().withConsistencyLevel( fig.getWriteCL() );

        batch.withRow( GRAPH_DELETE, ScopedRowKey.fromKey( scope, node ) ).setTimestamp( timestamp )
             .putColumn( COLUMN_NAME, timestamp );

        return batch;
    }


    @Override
    public MutationBatch delete( final ApplicationScope scope, final Id node, final long timestamp ) {
        ValidationUtils.validateApplicationScope( scope );
        ValidationUtils.verifyIdentity( node );
        GraphValidation.validateTimestamp( timestamp, "timestamp" );

        MutationBatch batch = keyspace.prepareMutationBatch().withConsistencyLevel( fig.getWriteCL() );

        batch.withRow( GRAPH_DELETE, ScopedRowKey.fromKey( scope, node ) ).setTimestamp( timestamp )
             .deleteColumn( COLUMN_NAME );

        return batch;
    }


    @Override
    public Optional<Long> getMaxVersion( final ApplicationScope scope, final Id node ) {
        ValidationUtils.validateApplicationScope( scope );
        ValidationUtils.verifyIdentity( node );

        ColumnFamilyQuery<ScopedRowKey<ApplicationScope, Id>, Boolean> query =
                keyspace.prepareQuery( GRAPH_DELETE ).setConsistencyLevel( fig.getReadCL() );


        try {
            Column<Boolean> result = HystrixCassandra
                    .user( query.getKey( ScopedRowKey.fromKey( scope, node ) ).getColumn( COLUMN_NAME ) )
                    .getResult();

            return Optional.of( result.getLongValue() );
        }
        catch (RuntimeException re ) {
            if(re.getCause().getCause() instanceof   NotFoundException) {
                //swallow, there's just no column
                return Optional.absent();
            }

            throw re;
        }

    }


    @Override
    public Map<Id, Long> getMaxVersions( final ApplicationScope scope, final Collection<? extends Edge> edges ) {
        ValidationUtils.validateApplicationScope( scope );
        Preconditions.checkNotNull( edges, "edges cannot be null" );


        final ColumnFamilyQuery<ScopedRowKey<ApplicationScope, Id>, Boolean> query =
                keyspace.prepareQuery( GRAPH_DELETE ).setConsistencyLevel( fig.getReadCL() );


        final List<ScopedRowKey<ApplicationScope, Id>> keys =
                new ArrayList<ScopedRowKey<ApplicationScope, Id>>( edges.size() );

        //worst case all are marked
        final Map<Id, Long> versions = new HashMap<>( edges.size() );

        for ( final Edge edge : edges ) {
            keys.add( ScopedRowKey.fromKey( scope, edge.getSourceNode() ) );
            keys.add( ScopedRowKey.fromKey( scope, edge.getTargetNode() ) );
        }


        final Rows<ScopedRowKey<ApplicationScope, Id>, Boolean> results = HystrixCassandra
                .user( query.getRowSlice( keys ).withColumnSlice( Collections.singletonList( COLUMN_NAME ) ) )
                .getResult();

        for ( Row<ScopedRowKey<ApplicationScope, Id>, Boolean> row : results ) {
            Column<Boolean> column = row.getColumns().getColumnByName( COLUMN_NAME );

            if ( column != null ) {
                versions.put( row.getKey().getKey(), column.getLongValue() );
            }
        }


        return versions;
    }
}
