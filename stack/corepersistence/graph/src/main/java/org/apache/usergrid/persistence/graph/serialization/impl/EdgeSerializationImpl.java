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

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.astynax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.collection.astynax.MultiTennantColumnFamily;
import org.apache.usergrid.persistence.collection.astynax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.serialization.impl.CollectionScopedRowKeySerializer;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.StringSerializer;


/**
 *
 *
 */
@Singleton
public class EdgeSerializationImpl implements EdgeSerialization, Migration {


    //row key serializers
    private static final IdRowCompositeSerializer ID_SER = IdRowCompositeSerializer.get();
    private static final CollectionScopedRowKeySerializer<Id> ID_ROW_KEY_SER = new CollectionScopedRowKeySerializer<Id>( ID_SER );

    private static final StringSerializer STRING_SERIALIZER = StringSerializer.get();


    private static final MultiTennantColumnFamily<CollectionScope, Id, String> CF_SOURCE_EDGES =
            new MultiTennantColumnFamily<CollectionScope, Id, String>( "Graph_Source_Edges", ID_ROW_KEY_SER,
                    STRING_SERIALIZER );


    private static final MultiTennantColumnFamily<CollectionScope, Id, String> CF_TARGET_EDGES =
            new MultiTennantColumnFamily<CollectionScope, Id, String>( "Graph_Target_Edges", ID_ROW_KEY_SER,
                    STRING_SERIALIZER );


    private static final MultiTennantColumnFamily<CollectionScope, Id, String> CF_SOURCE_TYPE_EDGES =
            new MultiTennantColumnFamily<CollectionScope, Id, String>( "Graph_SourceType_Edges", ID_ROW_KEY_SER,
                    STRING_SERIALIZER );


    private static final MultiTennantColumnFamily<CollectionScope, Id, String> CF_TARGET_TYPE_EDGES =
            new MultiTennantColumnFamily<CollectionScope, Id, String>( "Graph_TargetType_Edges", ID_ROW_KEY_SER,
                    STRING_SERIALIZER );


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public MutationBatch writeEdge( final OrganizationScope scope, final Edge edge ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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


    private static class SourceEdgeSerializer extends AbstractSerializer<Edge> {

        @Override
        public ByteBuffer toByteBuffer( final Edge edge ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public Edge fromByteBuffer( final ByteBuffer byteBuffer ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }


    private static class SourceEdgeTypeSerializer extends AbstractSerializer<Edge> {

        @Override
        public ByteBuffer toByteBuffer( final Edge edge ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public Edge fromByteBuffer( final ByteBuffer byteBuffer ) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }


    private static class TargetEdgeSerializer extends AbstractSerializer<Edge> {

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
