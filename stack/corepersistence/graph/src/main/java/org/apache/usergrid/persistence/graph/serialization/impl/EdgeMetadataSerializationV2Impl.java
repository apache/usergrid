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

package org.apache.usergrid.persistence.graph.serialization.impl;


import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.marshal.UTF8Type;

import org.apache.usergrid.persistence.core.astyanax.BucketScopedRowKey;
import org.apache.usergrid.persistence.core.astyanax.BucketScopedRowKeySerializer;
import org.apache.usergrid.persistence.core.astyanax.CassandraConfig;
import org.apache.usergrid.persistence.core.astyanax.ColumnSearch;
import org.apache.usergrid.persistence.core.astyanax.CompositeFieldSerializer;
import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.core.astyanax.MultiRowColumnIterator;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.astyanax.StringColumnParser;
import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.shard.ExpandingShardLocator;
import org.apache.usergrid.persistence.core.shard.StringHashUtils;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.SearchIdType;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.util.GraphValidation;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.util.RangeBuilder;


/**
 * Class to perform all edge metadata I/O
 */
@Singleton
public class EdgeMetadataSerializationV2Impl implements EdgeMetadataSerialization, Migration {

    private static final byte[] HOLDER = new byte[] { 0 };


    //row key serializers
    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();

    private static final BucketScopedRowKeySerializer<Id> ROW_KEY_SER = new BucketScopedRowKeySerializer<>( ID_SER );

    private static final StringSerializer STRING_SERIALIZER = StringSerializer.get();

    private static final EdgeTypeRowCompositeSerializer EDGE_SER = new EdgeTypeRowCompositeSerializer();

    private static final BucketScopedRowKeySerializer<EdgeIdTypeKey> EDGE_TYPE_ROW_KEY =
            new BucketScopedRowKeySerializer<>( EDGE_SER );

    private static final StringColumnParser PARSER = StringColumnParser.get();


    /**
     * V1 CF's.  We can't delete these until a full migration has been run
     */
    /**
     * CFs where the row key contains the source node id
     */
    private static final MultiTennantColumnFamily<BucketScopedRowKey<Id>, String> CF_SOURCE_EDGE_TYPES =
            new MultiTennantColumnFamily<>( "Graph_Source_Edge_Types_V2", ROW_KEY_SER, STRING_SERIALIZER );

    //all target id types for source edge type
    private static final MultiTennantColumnFamily<BucketScopedRowKey<EdgeIdTypeKey>, String> CF_SOURCE_EDGE_ID_TYPES =
            new MultiTennantColumnFamily<>( "Graph_Source_Edge_Id_Types_V2", EDGE_TYPE_ROW_KEY, STRING_SERIALIZER );

    /**
     * CFs where the row key is the target node id
     */
    private static final MultiTennantColumnFamily<BucketScopedRowKey<Id>, String> CF_TARGET_EDGE_TYPES =
            new MultiTennantColumnFamily<>( "Graph_Target_Edge_Types_V2", ROW_KEY_SER, STRING_SERIALIZER );


    //all source id types for target edge type
    private static final MultiTennantColumnFamily<BucketScopedRowKey<EdgeIdTypeKey>, String> CF_TARGET_EDGE_ID_TYPES =
            new MultiTennantColumnFamily<>( "Graph_Target_Edge_Id_Types_V2", EDGE_TYPE_ROW_KEY, STRING_SERIALIZER );


    private static final Comparator<String> STRING_COMPARATOR = new Comparator<String>() {

        @Override
        public int compare( final String o1, final String o2 ) {
            return o1.compareTo( o2 );
        }
    };


    /**
     * Funnel for hashing IDS
     */
    private static final Funnel<Id> ID_FUNNEL = new Funnel<Id>() {

        @Override
        public void funnel( final Id from, final PrimitiveSink into ) {
            final UUID id = from.getUuid();
            final String type = from.getType();

            into.putLong( id.getMostSignificantBits() );
            into.putLong( id.getLeastSignificantBits() );
            into.putString( type, StringHashUtils.UTF8 );
        }
    };

    /**
     * Funnel for hashing IDS
     */
    private static final Funnel<EdgeIdTypeKey> EDGE_TYPE_FUNNEL = new Funnel<EdgeIdTypeKey>() {

        @Override
        public void funnel( final EdgeIdTypeKey from, final PrimitiveSink into ) {

            final UUID id = from.node.getUuid();
            final String type = from.node.getType();

            into.putLong( id.getMostSignificantBits() );
            into.putLong( id.getLeastSignificantBits() );
            into.putString( type, StringHashUtils.UTF8 );
            into.putString( from.edgeType, StringHashUtils.UTF8 );
        }
    };


    protected final Keyspace keyspace;
    private final CassandraConfig cassandraConfig;
    private final GraphFig graphFig;

    /**
     * Locator for all id buckets
     */
    private ExpandingShardLocator<Id> idExpandingShardLocator;

    /**
     * Locator for all edge types
     */
    private ExpandingShardLocator<EdgeIdTypeKey> edgeTypeExpandingShardLocator;


    @Inject
    public EdgeMetadataSerializationV2Impl( final Keyspace keyspace, final CassandraConfig cassandraConfig,
                                            final GraphFig graphFig ) {

        Preconditions.checkNotNull( "cassandraConfig is required", cassandraConfig );
        Preconditions.checkNotNull( "consistencyFig is required", graphFig );
        Preconditions.checkNotNull( "keyspace is required", keyspace );

        this.keyspace = keyspace;
        this.cassandraConfig = cassandraConfig;
        this.graphFig = graphFig;

        //set up the shard locator instances
        idExpandingShardLocator = new ExpandingShardLocator<>( ID_FUNNEL, cassandraConfig.getShardSettings() );

        edgeTypeExpandingShardLocator =
                new ExpandingShardLocator<>( EDGE_TYPE_FUNNEL, cassandraConfig.getShardSettings() );
    }


    @Override
    public MutationBatch writeEdge( final ApplicationScope scope, final Edge edge ) {

        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateEdge( edge );

        final Id scopeId = scope.getApplication();
        final Id source = edge.getSourceNode();
        final Id target = edge.getTargetNode();
        final String edgeType = edge.getType();
        final long timestamp = edge.getTimestamp();

        final MutationBatch batch = keyspace.prepareMutationBatch().withConsistencyLevel( cassandraConfig.getWriteCL() )
                                            .withTimestamp( timestamp );


        //add source->target edge type to meta data
        final int sourceKeyBucket = idExpandingShardLocator.getCurrentBucket( source );


        final BucketScopedRowKey<Id> sourceKey = BucketScopedRowKey.fromKey( scopeId, source, sourceKeyBucket );

        batch.withRow( CF_SOURCE_EDGE_TYPES, sourceKey ).putColumn( edgeType, HOLDER );


        //write source->target edge type and id type to meta data
        final EdgeIdTypeKey sourceTargetTypeKey = new EdgeIdTypeKey( source, edgeType );

        final int sourceTargetTypeBucket = edgeTypeExpandingShardLocator.getCurrentBucket( sourceTargetTypeKey );

        final BucketScopedRowKey<EdgeIdTypeKey> sourceTypeKey =
                BucketScopedRowKey.fromKey( scopeId, sourceTargetTypeKey, sourceTargetTypeBucket );

        batch.withRow( CF_SOURCE_EDGE_ID_TYPES, sourceTypeKey ).putColumn( target.getType(), HOLDER );


        final int targetKeyBucket = idExpandingShardLocator.getCurrentBucket( target );

        final BucketScopedRowKey<Id> targetKey = BucketScopedRowKey.fromKey( scopeId, target, targetKeyBucket );

        batch.withRow( CF_TARGET_EDGE_TYPES, targetKey ).putColumn( edgeType, HOLDER );


        //write target<--source edge type and id type to meta data

        final EdgeIdTypeKey targetSourceTypeKey = new EdgeIdTypeKey( target, edgeType );

        final int targetSourceTypeKeyBucket = edgeTypeExpandingShardLocator.getCurrentBucket( targetSourceTypeKey );

        final BucketScopedRowKey<EdgeIdTypeKey> targetTypeKey =
                BucketScopedRowKey.fromKey( scopeId, targetSourceTypeKey, targetSourceTypeKeyBucket );

        batch.withRow( CF_TARGET_EDGE_ID_TYPES, targetTypeKey ).putColumn( source.getType(), HOLDER );


        return batch;
    }


    @Override
    public MutationBatch removeEdgeTypeFromSource( final ApplicationScope scope, final Edge edge ) {
        return removeEdgeTypeFromSource( scope, edge.getSourceNode(), edge.getType(), edge.getTimestamp() );
    }


    @Override
    public MutationBatch removeEdgeTypeFromSource( final ApplicationScope scope, final Id sourceNode, final String type,
                                                   final long version ) {
        return removeEdgeType( scope, sourceNode, type, version, CF_SOURCE_EDGE_TYPES );
    }


    @Override
    public MutationBatch removeIdTypeFromSource( final ApplicationScope scope, final Edge edge ) {
        return removeIdTypeFromSource( scope, edge.getSourceNode(), edge.getType(), edge.getTargetNode().getType(),
                edge.getTimestamp() );
    }


    @Override
    public MutationBatch removeIdTypeFromSource( final ApplicationScope scope, final Id sourceNode, final String type,
                                                 final String idType, final long version ) {
        return removeIdType( scope, sourceNode, idType, type, version, CF_SOURCE_EDGE_ID_TYPES );
    }


    @Override
    public MutationBatch removeEdgeTypeToTarget( final ApplicationScope scope, final Edge edge ) {
        return removeEdgeTypeToTarget( scope, edge.getTargetNode(), edge.getType(), edge.getTimestamp() );
    }


    @Override
    public MutationBatch removeEdgeTypeToTarget( final ApplicationScope scope, final Id targetNode, final String type,
                                                 final long version ) {
        return removeEdgeType( scope, targetNode, type, version, CF_TARGET_EDGE_TYPES );
    }


    @Override
    public MutationBatch removeIdTypeToTarget( final ApplicationScope scope, final Edge edge ) {
        return removeIdTypeToTarget( scope, edge.getTargetNode(), edge.getType(), edge.getSourceNode().getType(),
                edge.getTimestamp() );
    }


    @Override
    public MutationBatch removeIdTypeToTarget( final ApplicationScope scope, final Id targetNode, final String type,
                                               final String idType, final long version ) {
        return removeIdType( scope, targetNode, idType, type, version, CF_TARGET_EDGE_ID_TYPES );
    }


    /**
     * Remove the edge
     *
     * @param scope The scope
     * @param rowKeyId The id to use in the row key
     * @param edgeType The edge type
     * @param version The version of the edge
     * @param cf The column family
     */
    private MutationBatch removeEdgeType( final ApplicationScope scope, final Id rowKeyId, final String edgeType,
                                          final long version,
                                          final MultiTennantColumnFamily<BucketScopedRowKey<Id>, String> cf ) {


        //write target<--source edge type meta data
        final int currentShard = idExpandingShardLocator.getCurrentBucket( rowKeyId );

        final BucketScopedRowKey<Id> rowKey =
                BucketScopedRowKey.fromKey( scope.getApplication(), rowKeyId, currentShard );

        final MutationBatch batch = keyspace.prepareMutationBatch().withTimestamp( version );

        batch.withRow( cf, rowKey ).deleteColumn( edgeType );

        return batch;
    }


    /**
     * Remove the id type
     *
     * @param scope The scope to use
     * @param rowId The id to use in the row key
     * @param idType The id type to use in the column
     * @param edgeType The edge type to use in the column
     * @param version The version to use on the column
     * @param cf The column family to use
     *
     * @return A populated mutation with the remove operations
     */
    private MutationBatch removeIdType( final ApplicationScope scope, final Id rowId, final String idType,
                                        final String edgeType, final long version,
                                        final MultiTennantColumnFamily<BucketScopedRowKey<EdgeIdTypeKey>, String> cf ) {


        final EdgeIdTypeKey edgeIdTypeKey = new EdgeIdTypeKey( rowId, edgeType );

        final int currentShard = edgeTypeExpandingShardLocator.getCurrentBucket( edgeIdTypeKey );


        final MutationBatch batch = keyspace.prepareMutationBatch().withTimestamp( version );


        //write target<--source edge type and id type to meta data
        final BucketScopedRowKey<EdgeIdTypeKey> rowKey =
                BucketScopedRowKey.fromKey( scope.getApplication(), edgeIdTypeKey, currentShard );


        batch.withRow( cf, rowKey ).deleteColumn( idType );

        return batch;
    }


    @Override
    public Iterator<String> getEdgeTypesFromSource( final ApplicationScope scope, final SearchEdgeType search ) {
        return getEdgeTypes( scope, search, CF_SOURCE_EDGE_TYPES );
    }


    @Override
    public Iterator<String> getIdTypesFromSource( final ApplicationScope scope, final SearchIdType search ) {
        return getIdTypes( scope, search, CF_SOURCE_EDGE_ID_TYPES );
    }


    @Override
    public Iterator<String> getEdgeTypesToTarget( final ApplicationScope scope, final SearchEdgeType search ) {
        return getEdgeTypes( scope, search, CF_TARGET_EDGE_TYPES );
    }


    /**
     * Get the edge types from the search criteria.
     *
     * @param scope The org scope
     * @param search The edge type search info
     * @param cf The column family to execute on
     */
    private Iterator<String> getEdgeTypes( final ApplicationScope scope, final SearchEdgeType search,
                                           final MultiTennantColumnFamily<BucketScopedRowKey<Id>, String> cf ) {
        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateSearchEdgeType( search );


        final Id applicationId = scope.getApplication();
        final Id searchNode = search.getNode();

        final int[] bucketIds = idExpandingShardLocator.getAllBuckets( searchNode );


        //no generics is intentional here
        final List<BucketScopedRowKey<Id>> buckets =
                BucketScopedRowKey.fromRange( applicationId, searchNode, bucketIds );


        final ColumnSearch<String> columnSearch = createSearch( search );


        return new MultiRowColumnIterator( keyspace, cf, cassandraConfig.getReadCL(), PARSER, columnSearch,
                STRING_COMPARATOR, buckets, graphFig.getScanPageSize() );
    }


    @Override
    public Iterator<String> getIdTypesToTarget( final ApplicationScope scope, final SearchIdType search ) {
        return getIdTypes( scope, search, CF_TARGET_EDGE_ID_TYPES );
    }


    /**
     * Get the id types from the specified column family
     *
     * @param scope The organization scope to use
     * @param search The search criteria
     * @param cf The column family to search
     */
    public Iterator<String> getIdTypes( final ApplicationScope scope, final SearchIdType search,
                                        final MultiTennantColumnFamily<BucketScopedRowKey<EdgeIdTypeKey>, String> cf ) {
        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateSearchEdgeIdType( search );


        final Id applicationId = scope.getApplication();

        final Id searchNode = search.getNode();

        final EdgeIdTypeKey edgeIdTypeKey = new EdgeIdTypeKey( searchNode, search.getEdgeType() );

        final int[] bucketIds = edgeTypeExpandingShardLocator.getAllBuckets( edgeIdTypeKey );

        //no generics is intentional here
        final List<BucketScopedRowKey<EdgeIdTypeKey>> buckets =
                BucketScopedRowKey.fromRange( applicationId, edgeIdTypeKey, bucketIds );


        final ColumnSearch<String> columnSearch = createSearch( search );


        return new MultiRowColumnIterator( keyspace, cf, cassandraConfig.getReadCL(), PARSER, columnSearch,
                STRING_COMPARATOR, buckets, graphFig.getScanPageSize() );
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Arrays.asList( graphCf( CF_SOURCE_EDGE_TYPES ), graphCf( CF_TARGET_EDGE_TYPES ),
                graphCf( CF_SOURCE_EDGE_ID_TYPES ), graphCf( CF_TARGET_EDGE_ID_TYPES ) );
    }


    /**
     * Helper to generate an edge definition by the type
     */
    private MultiTennantColumnFamilyDefinition graphCf( MultiTennantColumnFamily cf ) {
        return new MultiTennantColumnFamilyDefinition( cf, BytesType.class.getSimpleName(),
                UTF8Type.class.getSimpleName(), BytesType.class.getSimpleName(),
                MultiTennantColumnFamilyDefinition.CacheOption.KEYS );
    }


    /**
     * Create a new instance of our search
     */
    private ColumnSearch<String> createSearch( final SearchEdgeType search ) {

        //resume from the last if specified.  Also set the range
        return new ColumnSearch<String>() {
            @Override
            public void buildRange( final RangeBuilder rangeBuilder, final String value ) {
                rangeBuilder.setLimit( graphFig.getScanPageSize() );


                if ( value != null ) {
                    rangeBuilder.setStart( value );
                }

                //we have a last, it's where we need to start seeking from
                else if ( search.getLast().isPresent() ) {
                    rangeBuilder.setStart( search.getLast().get() );
                }

                //no last was set, but we have a prefix, set it
                else if ( search.prefix().isPresent() ) {
                    rangeBuilder.setStart( search.prefix().get() );
                }


                //we have a prefix, so make sure we only seek to prefix + max UTF value
                if ( search.prefix().isPresent() ) {
                    rangeBuilder.setEnd( search.prefix().get() + "\uffff" );
                }
            }


            @Override
            public void buildRange( final RangeBuilder rangeBuilder ) {
                buildRange( rangeBuilder, null );
            }


            @Override
            public boolean skipFirst( final String first ) {

                final Optional<String> last = search.getLast();

                if(!last.isPresent()){
                    return false;
                }

                return last.get().equals( first );
            }
        };
    }


    @Override
    public int getImplementationVersion() {
        return GraphDataVersions.META_SHARDING.getVersion();
    }


    /**
     * Inner class to serialize and edgeIdTypeKey
     */
    private static class EdgeTypeRowCompositeSerializer implements CompositeFieldSerializer<EdgeIdTypeKey> {


        private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();


        @Override
        public void toComposite( final CompositeBuilder builder, final EdgeIdTypeKey value ) {
            ID_SER.toComposite( builder, value.node );

            builder.addString( value.edgeType );
        }


        @Override
        public EdgeIdTypeKey fromComposite( final CompositeParser composite ) {
            final Id id = ID_SER.fromComposite( composite );

            final String edgeType = composite.readString();

            return new EdgeIdTypeKey( id, edgeType );
        }

    }

    //
    //    private RangeBuilder createRange( final SearchEdgeType search ) {
    //        final RangeBuilder builder = new RangeBuilder().setLimit( graphFig.getScanPageSize() );
    //
    //
    //        //we have a last, it's where we need to start seeking from
    //        if ( search.getLast().isPresent() ) {
    //            builder.setStart( search.getLast().get() );
    //        }
    //
    //        //no last was set, but we have a prefix, set it
    //        else if ( search.prefix().isPresent() ) {
    //            builder.setStart( search.prefix().get() );
    //        }
    //
    //
    //        //we have a prefix, so make sure we only seek to prefix + max UTF value
    //        if ( search.prefix().isPresent() ) {
    //            builder.setEnd( search.prefix().get() + "\uffff" );
    //        }
    //
    //
    //        return builder;
    //    }


    //    private void setStart( final SearchEdgeType search, final RangeBuilder builder ) {
    //        //prefix is set, set our end marker
    //        if ( search.getLast().isPresent() ) {
    //            builder.setEnd( search.getLast().get() );
    //        }
    //
    //        else if ( search.prefix().isPresent() ) {
    //            builder.setStart( search.prefix().get() );
    //        }
    //    }
    //
    //
    //    private void setEnd( final SearchEdgeType search, final RangeBuilder builder ) {
    //        //if our last is set, it takes precendence
    //
    //        if ( search.prefix().isPresent() ) {
    //            builder.setEnd( search.prefix().get() + "\uffff" );
    //        }
    //    }


    /**
     * Simple key object for I/O
     */
    private static class EdgeIdTypeKey {
        private final Id node;
        private final String edgeType;


        private EdgeIdTypeKey( final Id node, final String edgeType ) {
            this.node = node;
            this.edgeType = edgeType;
        }
    }
}
