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
import org.apache.cassandra.db.marshal.DynamicCompositeType;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.collection.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ColumnNameIterator;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ColumnParser;
import org.apache.usergrid.persistence.graph.serialization.util.EdgeHasher;
import org.apache.usergrid.persistence.graph.serialization.util.EdgeUtils;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ByteBufferRange;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.DynamicComposite;
import com.netflix.astyanax.query.RowQuery;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;
import com.netflix.astyanax.util.RangeBuilder;
import org.apache.usergrid.persistence.astyanax.IdColDynamicCompositeSerializer;


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

    //row key serializers
    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();
    private static final OrganizationScopedRowKeySerializer<Id> ORG_ROW_KEY_SER =
            new OrganizationScopedRowKeySerializer<Id>( ID_SER );

    //Edge serializers
    private static final EdgeTypeSerializer EDGE_TYPE_SERIALIZER = new EdgeTypeSerializer();

    private static final EdgeTypeIdSerializer EDGE_TYPE_ID_SERIALIZER = new EdgeTypeIdSerializer();


    // column families
    private static final MultiTennantColumnFamily<OrganizationScope, Id, DirectedEdge> CF_SOURCE_EDGES =
            new MultiTennantColumnFamily<OrganizationScope, Id, DirectedEdge>( "Graph_Source_Edges", ORG_ROW_KEY_SER,
                    EDGE_TYPE_SERIALIZER );


    private static final MultiTennantColumnFamily<OrganizationScope, Id, DirectedEdge> CF_TARGET_EDGES =
            new MultiTennantColumnFamily<OrganizationScope, Id, DirectedEdge>( "Graph_Target_Edges", ORG_ROW_KEY_SER,
                    EDGE_TYPE_SERIALIZER );


    private static final MultiTennantColumnFamily<OrganizationScope, Id, DirectedEdge> CF_SOURCE_TYPE_EDGES =
            new MultiTennantColumnFamily<OrganizationScope, Id, DirectedEdge>( "Graph_SourceType_Edges",
                    ORG_ROW_KEY_SER, EDGE_TYPE_ID_SERIALIZER );


    private static final MultiTennantColumnFamily<OrganizationScope, Id, DirectedEdge> CF_TARGET_TYPE_EDGES =
            new MultiTennantColumnFamily<OrganizationScope, Id, DirectedEdge>( "Graph_TargetType_Edges",
                    ORG_ROW_KEY_SER, EDGE_TYPE_ID_SERIALIZER );


    //edge creators for seeking in either direction
    private static final SourceDirectedEdgeCreator SOURCE_DIRECTED_EDGE_CREATOR = new SourceDirectedEdgeCreator();

    private static final TargetDirectedEdgeCreator TARGET_DIRECTED_EDGE_CREATOR = new TargetDirectedEdgeCreator();

    protected final Keyspace keyspace;


    @Inject
    public EdgeSerializationImpl( final Keyspace keyspace ) {
        this.keyspace = keyspace;
    }


    @Override
    public MutationBatch writeEdge( final OrganizationScope scope, final Edge edge ) {
        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateEdge( edge );

        final MutationBatch batch = keyspace.prepareMutationBatch();

        final String edgeTypeName = edge.getType();
        final Id sourceNodeId = edge.getSourceNode();
        final Id targetNodeId = edge.getTargetNode();
        final UUID version = edge.getVersion();


        /**
         * Edge from source->target
         */
        final ScopedRowKey<OrganizationScope, Id> sourceKey =
                new ScopedRowKey<OrganizationScope, Id>( scope, sourceNodeId );


        final DirectedEdge sourceEdge = new DirectedEdge( edgeTypeName, targetNodeId, version );


        /**
         * Edge of target<-source
         */
        final ScopedRowKey<OrganizationScope, Id> targetKey =
                new ScopedRowKey<OrganizationScope, Id>( scope, targetNodeId );

        final DirectedEdge targetEdge = new DirectedEdge( edgeTypeName, sourceNodeId, version );


        /**
         * Write edges from source->target
         */
        batch.withRow( CF_SOURCE_EDGES, sourceKey ).putColumn( sourceEdge, HOLDER );

        batch.withRow( CF_SOURCE_TYPE_EDGES, sourceKey ).putColumn( sourceEdge, HOLDER );

        /**
         * Write edges target<- source
         */

        batch.withRow( CF_TARGET_EDGES, targetKey ).putColumn( targetEdge, HOLDER );

        batch.withRow( CF_TARGET_TYPE_EDGES, targetKey ).putColumn( targetEdge, HOLDER );

        return batch;
    }


    @Override
    public MutationBatch deleteEdge( final OrganizationScope scope, final Edge edge ) {

        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateEdge( edge );

        final MutationBatch batch = keyspace.prepareMutationBatch();

        final String edgeTypeName = edge.getType();
        final Id sourceNodeId = edge.getSourceNode();
        final Id targetNodeId = edge.getTargetNode();
        final UUID version = edge.getVersion();


        /**
         * Edge from source->target
         */
        final ScopedRowKey<OrganizationScope, Id> sourceKey =
                new ScopedRowKey<OrganizationScope, Id>( scope, sourceNodeId );


        final DirectedEdge sourceEdge = new DirectedEdge( edgeTypeName, targetNodeId, version );


        /**
         * Edge of target<-source
         */
        final ScopedRowKey<OrganizationScope, Id> targetKey =
                new ScopedRowKey<OrganizationScope, Id>( scope, targetNodeId );

        final DirectedEdge targetEdge = new DirectedEdge( edgeTypeName, sourceNodeId, version );


        /**
         * Write edges from source->target
         */
        batch.withRow( CF_SOURCE_EDGES, sourceKey ).deleteColumn( sourceEdge );

        batch.withRow( CF_SOURCE_TYPE_EDGES, sourceKey ).deleteColumn( sourceEdge );

        /**
         * Write edges target<- source
         */

        batch.withRow( CF_TARGET_EDGES, targetKey ).deleteColumn( targetEdge );

        batch.withRow( CF_TARGET_TYPE_EDGES, targetKey ).deleteColumn( targetEdge );

        return batch;
    }


    @Override
    public Iterator<Edge> getTargetEdges( final OrganizationScope scope, final SearchByEdgeType edgeType ) {
        return getEdges( scope, edgeType, CF_SOURCE_EDGES, EDGE_TYPE_ID_SERIALIZER, SOURCE_DIRECTED_EDGE_CREATOR );
    }


    @Override
    public Iterator<Edge> getTargetIdEdges( final OrganizationScope scope, final SearchByIdType edgeType ) {
        return getEdges( scope, edgeType, CF_SOURCE_TYPE_EDGES, EDGE_TYPE_SERIALIZER, SOURCE_DIRECTED_EDGE_CREATOR );
    }



    @Override
    public Iterator<Edge> getSourceEdges( final OrganizationScope scope, final SearchByEdgeType edgeType ) {
        return getEdges( scope, edgeType, CF_TARGET_EDGES, EDGE_TYPE_SERIALIZER, TARGET_DIRECTED_EDGE_CREATOR );
    }


    @Override
    public Iterator<Edge> getSourceIdEdges( final OrganizationScope scope, final SearchByIdType edgeType ) {
        return getEdges( scope, edgeType, CF_TARGET_TYPE_EDGES, EDGE_TYPE_ID_SERIALIZER, TARGET_DIRECTED_EDGE_CREATOR );
    }


    /**
     * Get the edges with the specified criteria
     * @param scope The organization scope
     * @param edgeType The search type
     * @param cf The column familiy to search
     * @param edgeSerializer The serializer to use when reading columns
     * @param directedEdgeCreator The creator to use when creating the edges for searching
     * @return An iterator of all Edge instance that match the criteria
     */
    private Iterator<Edge> getEdges( final OrganizationScope scope, final SearchByEdgeType edgeType,
                                     MultiTennantColumnFamily<OrganizationScope, Id, DirectedEdge> cf,
                                     final EdgeSerializer edgeSerializer,
                                     final DirectedEdgeCreator directedEdgeCreator ) {
        ValidationUtils.validateOrganizationScope( scope );
        EdgeUtils.validateSearchByEdgeType( edgeType );


        final String edgeTypeName = edgeType.getType();
        final Id nodeId = edgeType.getNode();
        final UUID version = edgeType.getMaxVersion();


        /**
         * Edge from source->target
         */
        final ScopedRowKey<OrganizationScope, Id> sourceKey = new ScopedRowKey<OrganizationScope, Id>( scope, nodeId );


        DirectedEdge sourceEdge;

        /**
         * If the edge is present, we need to being seeking from this
         */
        if ( edgeType.last().isPresent() ) {
            sourceEdge = directedEdgeCreator.getDirectedEdge( edgeType.last().get() );
        }
        else {
            sourceEdge = new DirectedEdge( edgeTypeName, nodeId, version );
        }


        //resume from the last if specified.  Also set the range
        final ByteBufferRange searchRange =
                new RangeBuilder().setLimit( PAGE_SIZE ).setStart( sourceEdge, edgeSerializer ).build();

        RowQuery<ScopedRowKey<OrganizationScope, Id>, DirectedEdge> query =
                keyspace.prepareQuery( cf ).getKey( sourceKey ).autoPaginate( true ).withColumnRange( searchRange );


        try {
            return new ColumnNameIterator<DirectedEdge, Edge>( query.execute().getResult().iterator(),
                    new SourceEdgeColumnParser( nodeId, edgeTypeName ) );
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to connect to cassandra", e );
        }
    }




    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return Arrays.asList( graphCf( CF_SOURCE_EDGES ), graphCf( CF_TARGET_EDGES ),
                graphCf( CF_SOURCE_TYPE_EDGES ), graphCf( CF_TARGET_TYPE_EDGES ) );
    }


    /**
     * Helper to generate an edge definition by the type
     */
    private MultiTennantColumnFamilyDefinition graphCf( MultiTennantColumnFamily cf ) {
        return new MultiTennantColumnFamilyDefinition( cf, DynamicCompositeType.class.getSimpleName(),
                BytesType.class.getSimpleName(), BytesType.class.getSimpleName() );
    }


    /**
     * Serializes to a source->target edge Note that we cannot set the edge type on de-serialization.  Only the target
     * Id and version.
     */
    private static abstract class EdgeSerializer extends AbstractSerializer<DirectedEdge> {

        private static final IdColDynamicCompositeSerializer ID_COL_SERIALIZER = IdColDynamicCompositeSerializer.get();

        private static final UUIDSerializer UUID_SERIALIZER = UUIDSerializer.get();


        @Override
        public ByteBuffer toByteBuffer( final DirectedEdge edge ) {
            final long[] targetEdgeTypes = getHash( edge );


            DynamicComposite composite = new DynamicComposite();

            for ( long hash : targetEdgeTypes ) {
                composite.add( hash );
            }

            ID_COL_SERIALIZER.toComposite( composite, edge.id );

            composite.addComponent( edge.version, UUID_SERIALIZER );


            return composite.serialize();
        }


        @Override
        public DirectedEdge fromByteBuffer( final ByteBuffer byteBuffer ) {
            DynamicComposite composite = DynamicComposite.fromByteBuffer( byteBuffer );


            Preconditions.checkArgument( composite.size() == 5, "Composite should have 5 elements" );

            final Id id = ID_COL_SERIALIZER.fromComposite( composite, 3 );

            final UUID version = composite.get( 5, UUID_SERIALIZER );

            return new DirectedEdge( null, id, version );
        }


        /**
         * Get the hashes for the edge
         */
        protected abstract long[] getHash( DirectedEdge edge );
    }


    /**
     * Edge type serializer
     */
    private static class EdgeTypeSerializer extends EdgeSerializer {

        @Override
        protected long[] getHash( final DirectedEdge edge ) {
            return EdgeHasher.createEdgeHash( edge.type );
        }
    }


    /**
     * Edge type serializer
     */
    private static class EdgeTypeIdSerializer extends EdgeSerializer {

        @Override
        protected long[] getHash( final DirectedEdge edge ) {
            return EdgeHasher.createEdgeHash( edge.type, edge.id );
        }
    }


    /**
     * Internal class to represent edge data for serialization
     */
    private static class DirectedEdge {

        public final String type;
        public final UUID version;
        public final Id id;


        private DirectedEdge( final String type, final Id id, final UUID version ) {
            this.type = type;
            this.version = version;
            this.id = id;
        }
    }


    /**
     * Simple parser to parse column names
     */
    private abstract static class EdgeColumnParser implements ColumnParser<DirectedEdge, Edge> {

        private final Id id;
        private final String edgeType;


        /**
         * Default constructor. Needs to take the id and edge used in the search
         */
        protected EdgeColumnParser( final Id id, final String edgeType ) {
            this.id = id;
            this.edgeType = edgeType;
        }


        @Override
        public Edge parseColumn( final Column<DirectedEdge> column ) {

            final DirectedEdge edge = column.getName();

            return buildEdge( id, edgeType, edge.id, edge.version );
        }


        protected abstract Edge buildEdge( final Id rowKeyId, final String edgeType, final Id colId,
                                           final UUID version );
    }


    /**
     * Creator from source->target
     */
    private static class SourceEdgeColumnParser extends EdgeColumnParser {

        /**
         * Default constructor. Needs to take the id and edge used in the search
         */
        protected SourceEdgeColumnParser( final Id id, final String edgeType ) {
            super( id, edgeType );
        }


        @Override
        protected Edge buildEdge( final Id rowKeyId, final String edgeType, final Id colId, final UUID version ) {
            return new SimpleEdge( rowKeyId, edgeType, colId, version );
        }
    }


    /**
     * Creator from target<-source
     */
    private static class TargetEdgeColumnParser extends EdgeColumnParser {

        /**
         * Default constructor. Needs to take the id and edge used in the search
         */
        protected TargetEdgeColumnParser( final Id id, final String edgeType ) {
            super( id, edgeType );
        }


        @Override
        protected Edge buildEdge( final Id rowKeyId, final String edgeType, final Id colId, final UUID version ) {
            return new SimpleEdge( colId, edgeType, rowKeyId, version );
        }
    }


    private static interface DirectedEdgeCreator {

        /**
         * @param last The last edge
         *
         * @return The edge to use when searching
         */
        DirectedEdge getDirectedEdge( Edge last );
    }


    /**
     * Source->Target directed edge
     */
    private static class SourceDirectedEdgeCreator implements DirectedEdgeCreator {

        @Override
        public DirectedEdge getDirectedEdge( final Edge last ) {
            return new DirectedEdge( last.getType(), last.getTargetNode(), last.getVersion() );
        }
    }


    /**
     * Source->Target directed edge
     */
    private static class TargetDirectedEdgeCreator implements DirectedEdgeCreator {

        @Override
        public DirectedEdge getDirectedEdge( final Edge last ) {
            return new DirectedEdge( last.getType(), last.getSourceNode(), last.getVersion() );
        }
    }
}
