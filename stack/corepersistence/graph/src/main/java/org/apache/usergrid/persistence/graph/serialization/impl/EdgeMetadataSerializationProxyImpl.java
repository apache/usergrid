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


import java.util.Collection;
import java.util.Iterator;

import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.guice.CurrentImpl;
import org.apache.usergrid.persistence.core.guice.PreviousImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.SearchIdType;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;


@Singleton
public class EdgeMetadataSerializationProxyImpl implements EdgeMetadataSerialization {

    private EdgeMetadataSerialization previous;
    private EdgeMetadataSerialization current;


    /**
     * TODO fin
     */
    @Inject
    public EdgeMetadataSerializationProxyImpl( @PreviousImpl final EdgeMetadataSerialization previous,
                                               @CurrentImpl final EdgeMetadataSerialization current ) {
        this.previous = previous;
        this.current = current;
    }


    @Override
    public MutationBatch writeEdge( final ApplicationScope scope, final Edge edge ) {
        return null;
    }


    @Override
    public MutationBatch removeEdgeTypeFromSource( final ApplicationScope scope, final Edge edge ) {
        return null;
    }


    @Override
    public MutationBatch removeEdgeTypeFromSource( final ApplicationScope scope, final Id sourceNode, final String type,
                                                   final long timestamp ) {
        return null;
    }


    @Override
    public MutationBatch removeIdTypeFromSource( final ApplicationScope scope, final Edge edge ) {
        return null;
    }


    @Override
    public MutationBatch removeIdTypeFromSource( final ApplicationScope scope, final Id sourceNode, final String type,
                                                 final String idType, final long timestamp ) {
        return null;
    }


    @Override
    public MutationBatch removeEdgeTypeToTarget( final ApplicationScope scope, final Edge edge ) {
        return null;
    }


    @Override
    public MutationBatch removeEdgeTypeToTarget( final ApplicationScope scope, final Id targetNode, final String type,
                                                 final long timestamp ) {
        return null;
    }


    @Override
    public MutationBatch removeIdTypeToTarget( final ApplicationScope scope, final Edge edge ) {
        return null;
    }


    @Override
    public MutationBatch removeIdTypeToTarget( final ApplicationScope scope, final Id targetNode, final String type,
                                               final String idType, final long timestamp ) {
        return null;
    }


    @Override
    public Iterator<String> getEdgeTypesFromSource( final ApplicationScope scope, final SearchEdgeType search ) {
        return null;
    }


    @Override
    public Iterator<String> getIdTypesFromSource( final ApplicationScope scope, final SearchIdType search ) {
        return null;
    }


    @Override
    public Iterator<String> getEdgeTypesToTarget( final ApplicationScope scope, final SearchEdgeType search ) {
        return null;
    }


    @Override
    public Iterator<String> getIdTypesToTarget( final ApplicationScope scope, final SearchIdType search ) {
        return null;
    }


    @Override
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies() {
        return null;
    }
}
