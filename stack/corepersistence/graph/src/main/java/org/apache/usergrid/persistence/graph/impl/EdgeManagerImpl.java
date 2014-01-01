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

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.EdgeManager;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.SearchEdgeType;
import org.apache.usergrid.persistence.graph.SearchIdType;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.parse.ObservableIterator;
import org.apache.usergrid.persistence.graph.serialization.stage.write.EdgeWriteStage;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import rx.Observable;
import rx.Scheduler;


/**
 *
 *
 */
public class EdgeManagerImpl implements EdgeManager {


    private final OrganizationScope scope;

    private final Scheduler scheduler;

    private final EdgeWriteStage edgeWriteStage;

    private final EdgeMetadataSerialization edgeMetadataSerialization;


    @Inject
    public EdgeManagerImpl( final EdgeWriteStage edgeWriteStage, final Scheduler scheduler,
                            @Assisted final OrganizationScope scope,
                            final EdgeMetadataSerialization edgeMetadataSerialization ) {
        this.edgeWriteStage = edgeWriteStage;
        this.scheduler = scheduler;
        this.edgeMetadataSerialization = edgeMetadataSerialization;

        ValidationUtils.validateOrganizationScope( scope );


        this.scope = scope;
    }


    @Override
    public Observable<Edge> writeEdge( final Edge edge ) {
        Observable observable = Observable.just( edge ).subscribeOn( scheduler );

        observable.subscribe( edgeWriteStage );

        return observable;
    }


    @Override
    public void deleteEdge( final Edge edge ) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public Observable<Edge> loadSourceEdges( final SearchByEdgeType search ) {

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public Observable<Edge> loadTargetEdges( final SearchByEdgeType search ) {


        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public Observable<Edge> loadSourceEdges( final SearchByIdType search ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public Observable<Edge> loadTargetEdges( final SearchByIdType search ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public Observable<String> getSourceEdgeTypes( final SearchEdgeType search ) {
        return Observable.create( new ObservableIterator<String>() {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getSourceEdgeTypes( scope, search );
            }
        } );
    }


    @Override
    public Observable<String> getSourceIdTypes( final SearchIdType search ) {
        return Observable.create( new ObservableIterator<String>() {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getSourceIdTypes( scope, search );
            }
        } );
    }


    @Override
    public Observable<String> getTargetEdgeTypes( final SearchEdgeType search ) {

        return Observable.create( new ObservableIterator<String>() {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getTargetEdgeTypes( scope, search );
            }
        } );
    }


    @Override
    public Observable<String> getTargetIdTypes( final SearchIdType search ) {
        return Observable.create( new ObservableIterator<String>() {
            @Override
            protected Iterator<String> getIterator() {
                return edgeMetadataSerialization.getTargetIdTypes( scope, search );
            }
        } );
    }
}
