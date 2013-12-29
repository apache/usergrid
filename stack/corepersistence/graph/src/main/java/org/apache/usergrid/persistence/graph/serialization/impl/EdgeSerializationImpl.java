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
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.astynax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.collection.astynax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.collection.astynax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.collection.astynax.ScopedRowKey;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.astyanax.IdColDynamicCompositeSerializer;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.util.EdgeHasher;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.AbstractComposite;
import com.netflix.astyanax.model.DynamicComposite;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;


/**
 *
 *
 */
@Singleton
public class EdgeSerializationImpl implements EdgeSerialization, Migration {


    //row key serializers
    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();
    private static final OrganizationScopedRowKeySerializer<Id> ID_ROW_KEY_SER =
            new OrganizationScopedRowKeySerializer<Id>( ID_SER );

    private static final StringSerializer STRING_SERIALIZER = StringSerializer.get();


    private static final MultiTennantColumnFamily<OrganizationScope, Id, String> CF_SOURCE_EDGES =
            new MultiTennantColumnFamily<OrganizationScope, Id, String>( "Graph_Source_Edges", ID_ROW_KEY_SER,
                    STRING_SERIALIZER );


    private static final MultiTennantColumnFamily<OrganizationScope, Id, String> CF_TARGET_EDGES =
            new MultiTennantColumnFamily<OrganizationScope, Id, String>( "Graph_Target_Edges", ID_ROW_KEY_SER,
                    STRING_SERIALIZER );


    private static final MultiTennantColumnFamily<OrganizationScope, Id, String> CF_SOURCE_TYPE_EDGES =
            new MultiTennantColumnFamily<OrganizationScope, Id, String>( "Graph_SourceType_Edges", ID_ROW_KEY_SER,
                    STRING_SERIALIZER );


    private static final MultiTennantColumnFamily<OrganizationScope, Id, String> CF_TARGET_TYPE_EDGES =
            new MultiTennantColumnFamily<OrganizationScope, Id, String>( "Graph_TargetType_Edges", ID_ROW_KEY_SER,
                    STRING_SERIALIZER );


    protected final Keyspace keyspace;


    @Inject
    public EdgeSerializationImpl( final Keyspace keyspace ) {
        this.keyspace = keyspace;
    }


    @Override
    public MutationBatch writeEdge( final OrganizationScope scope, final Edge edge ) {
        final MutationBatch batch = keyspace.prepareMutationBatch();

        final String edgeTypeName = edge.getType();
        final Id sourceNodeId = edge.getSourceNode();
        final Id targetNodeId = edge.getTargetNode();

        final long[] edgeType = EdgeHasher.createEdgeHash( edgeTypeName );
        final long[] targetEdgeType = EdgeHasher.createEdgeHash( edgeTypeName, targetNodeId );
        final long[] sourceEdgeType = EdgeHasher.createEdgeHash( edgeTypeName, sourceNodeId );

        final ScopedRowKey<OrganizationScope, Id> sourceKey =
                new ScopedRowKey<OrganizationScope, Id>( scope, sourceNodeId );
        final ScopedRowKey<OrganizationScope, Id> targetKey =
                new ScopedRowKey<OrganizationScope, Id>( scope, targetNodeId );


        batch.withRow( CF_TARGET_EDGES, sourceKey ).putColumn();

        return batch;
    }


    @Override
    public MutationBatch deleteEdge( final OrganizationScope scope, final Edge edge ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public Iterator<Edge> getTargetEdges( final OrganizationScope scope, final SearchByEdgeType edgeType ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public Iterator<Edge> getTargetIdEdges( final OrganizationScope scope, final SearchByIdType edgeType ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public Iterator<Edge> getSourceEdges( final OrganizationScope scope, final SearchByEdgeType edgeType ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public Iterator<Edge> getSourceIdEdges( final OrganizationScope scope, final SearchByIdType edgeType ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    /**
     * Serializes to a source->target edge
     */
    private static class TargetEdgeSerializer extends AbstractSerializer<Edge> {

        private static final IdColDynamicCompositeSerializer ID_COL_SERIALIZER = IdColDynamicCompositeSerializer.get();

        private static final UUIDSerializer UUID_SERIALIZER = UUIDSerializer.get();


        private final Id sourceId;
        private final String edgeName;


        /**
         * Should be used for de-serializing
         * @param sourceId
         * @param edgeName
         */
        private TargetEdgeSerializer( final Id sourceId, final String edgeName ) {
            this.sourceId = sourceId;
            this.edgeName = edgeName;
        }


        /**
         * Should be used for serialized
         */
        private TargetEdgeSerializer(){
            sourceId = null;
            edgeName = null;
        }


        @Override
        public ByteBuffer toByteBuffer( final Edge edge ) {
            final long[] targetEdgeTypes = EdgeHasher.createEdgeHash(edge.getType());


            DynamicComposite composite = new DynamicComposite();

            for ( long hash : targetEdgeTypes ) {
                composite.add( hash );
            }

            ID_COL_SERIALIZER.toComposite( composite, edge.getTargetNode() );

            composite.addComponent( edge.getVersion(), UUID_SERIALIZER );


            return composite.serialize();
        }


        @Override
        public Edge fromByteBuffer( final ByteBuffer byteBuffer ) {
           DynamicComposite composite = DynamicComposite.fromByteBuffer( byteBuffer );

            Iterator<AbstractComposite.Component<?>> iterator = composite.getComponents().iterator();

            //skip the hashed data
            Preconditions.checkArgument( iterator.hasNext(), "First hash element should be present" );
            iterator.next();
            Preconditions.checkArgument( iterator.hasNext(), "Second hash element should be present" );
            iterator.next();

            final Id targetId = ID_COL_SERIALIZER.fromComposite( iterator );

            Preconditions.checkArgument( iterator.hasNext(), "Version element should be present" );

            final UUID version = iterator.next().getValue(UUID_SERIALIZER);

            return new SimpleEdge(sourceId, edgeName, targetId, version);
        }
    }


    private static class TargetEdgeTypeSerializer extends AbstractSerializer<Edge> {

        @Override
        public ByteBuffer toByteBuffer( final Edge edge ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public Edge fromByteBuffer( final ByteBuffer byteBuffer ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }


}
