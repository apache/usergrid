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

package org.apache.usergrid.persistence.graph.impl;


import java.util.Iterator;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.EdgeManager;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.SearchIdType;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ObservableIterator;
import org.apache.usergrid.persistence.graph.serialization.stage.GraphIoEvent;
import org.apache.usergrid.persistence.graph.serialization.stage.write.EdgeWriteStage;

import com.fasterxml.uuid.UUIDComparator;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import rx.Observable;
import rx.Scheduler;
import rx.util.functions.Func1;


/**
 *
 *
 */
public class EdgeManagerImpl implements EdgeManager {


    private final OrganizationScope scope;

    private final Scheduler scheduler;

    private final EdgeWriteStage edgeWriteStage;

    private final EdgeMetadataSerialization edgeMetadataSerialization;

    private final EdgeSerialization edgeSerialization;


    @Inject
    public EdgeManagerImpl( final EdgeWriteStage edgeWriteStage, final Scheduler scheduler,
                            final EdgeMetadataSerialization edgeMetadataSerialization,
                            final EdgeSerialization edgeSerialization, @Assisted final OrganizationScope scope ) {
        this.edgeWriteStage = edgeWriteStage;
        this.scheduler = scheduler;
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.edgeSerialization = edgeSerialization;


        ValidationUtils.validateOrganizationScope( scope );


        this.scope = scope;
    }


    @Override
    public Observable<Edge> writeEdge( final Edge edge ) {
        return Observable.from( new GraphIoEvent<Edge>( scope, edge ) ).subscribeOn( scheduler ).map( edgeWriteStage );
    }


    @Override
    public void deleteEdge( final Edge edge ) {
        throw new UnsupportedOperationException( "Not yet implemented" );
    }


    @Override
    public Observable<Edge> loadEdgesFromSource( final SearchByEdgeType search ) {
        Observable<Edge> iterator = Observable.create( new ObservableIterator<Edge>() {
            @Override
            protected Iterator<Edge> getIterator() {
                return edgeSerialization.getEdgesFromSource( scope, search );
            }
        } );

        return filter( iterator, search.getMaxVersion() );
    }


    @Override
    public Observable<Edge> loadEdgesToTarget( final SearchByEdgeType search ) {
        Observable<Edge> iterator = Observable.create( new ObservableIterator<Edge>() {
            @Override
            protected Iterator<Edge> getIterator() {
                return edgeSerialization.getEdgesToTarget( scope, search );
            }
        } );

        return filter( iterator, search.getMaxVersion() );
    }


    @Override
    public Observable<Edge> loadEdgesFromSourceByType( final SearchByIdType search ) {
        Observable<Edge> iterator = Observable.create( new ObservableIterator<Edge>() {
            @Override
            protected Iterator<Edge> getIterator() {
                return edgeSerialization.getEdgesFromSourceByTargetType( scope, search );
            }
        } );

        return filter( iterator, search.getMaxVersion() );
    }


    @Override
    public Observable<Edge> loadEdgesToTargetByType( final SearchByIdType search ) {
        Observable<Edge> iterator = Observable.create( new ObservableIterator<Edge>() {
            @Override
            protected Iterator<Edge> getIterator() {
                return edgeSerialization.getEdgesToTargetBySourceType( scope, search );
            }
        } );

        return filter( iterator, search.getMaxVersion() );
    }


    @Override
    public Observable<String> getEdgeTypesFromSource( final SearchEdgeType search ) {

        return Observable.create( new ObservableIterator<String>() {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getEdgeTypesFromSource( scope, search );
            }
        } );
    }


    @Override
    public Observable<String> getIdTypesFromSource( final SearchIdType search ) {
        return Observable.create( new ObservableIterator<String>() {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getIdTypesFromSource( scope, search );
            }
        } );
    }


    @Override
    public Observable<String> getEdgeTypesToTarget( final SearchEdgeType search ) {

        return Observable.create( new ObservableIterator<String>() {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getEdgeTypesToTarget( scope, search );
            }
        } );
    }


    @Override
    public Observable<String> getIdTypesToTarget( final SearchIdType search ) {
        return Observable.create( new ObservableIterator<String>() {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getIdTypesToTarget( scope, search );
            }
        } );
    }


    /**
     * If not max version is specified, just return the original observable.  If one is
     * @param observable
     * @param maxVersion
     * @return
     */
    private Observable<Edge> filter( final Observable<Edge> observable, final UUID maxVersion ) {
          return observable.filter( new Func1<Edge, Boolean>() {
            @Override
            public Boolean call( final Edge edge ) {
                //our edge version needs to be <= max Version
                return UUIDComparator.staticCompare( edge.getVersion(), maxVersion ) < 1;
            }
        } );
    }
}
