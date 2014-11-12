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

package org.apache.usergrid.corepersistence.migration;


import java.util.List;

import org.apache.usergrid.corepersistence.rx.ApplicationObservable;
import org.apache.usergrid.corepersistence.rx.EdgesFromSourceObservable;
import org.apache.usergrid.corepersistence.rx.TargetIdObservable;
import org.apache.usergrid.persistence.core.guice.CurrentImpl;
import org.apache.usergrid.persistence.core.guice.PreviousImpl;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;


/**
 * Migration for migrating graph edges to the new Shards
 */
public class GraphShardVersionMigration implements DataMigration {


    private final EdgeMetadataSerialization v1Serialization;


    private final EdgeMetadataSerialization v2Serialization;

    private final GraphManagerFactory graphManagerFactory;


    @Inject
    public GraphShardVersionMigration( @PreviousImpl final EdgeMetadataSerialization v1Serialization,
                                       @CurrentImpl final EdgeMetadataSerialization v2Serialization,
                                       final GraphManagerFactory graphManagerFactory ) {
        this.v1Serialization = v1Serialization;
        this.v2Serialization = v2Serialization;
        this.graphManagerFactory = graphManagerFactory;
    }


    @Override
    public void migrate( final ProgressObserver observer ) throws Throwable {

//    TODO, finish this
// get each applicationid in our system
//        Observable.create( new ApplicationObservable( graphManagerFactory ) ) .doOnNext( new Action1<Id>() {
//                    @Override
//                    public void call( final Id id ) {
//
//                        //set up our application scope and graph manager
//                        final ApplicationScope applicationScope = new ApplicationScopeImpl( id );
//
//
//                        final GraphManager gm = graphManagerFactory.createEdgeManager( applicationScope );
//
//
//                        //load all nodes that are targets of our application node.  I.E. entities that have been saved
//                        final Observable<Edge> entityNodes = Observable.create( new EdgesFromSourceObservable( applicationScope, id, gm ) );
//
//                        //create our application node
//                        final Observable<Id> applicationNode = Observable.just( id );
//
//                        //merge both the specified application node and the entity node so they all get used
//                        Observable.merge( applicationNode, entityNodes ).doOnNext( new Action1<Id>() {
//                            //load all meta types from and to-and re-save them
//
//
//                            @Override
//                            public void call( final Id id ) {
//                                //get the edge types from the source, buffer them, then re-save them.  This implicity
//                                // updates target edges as well
//                                gm.loadEdgesFromSource( new SimpleSearchByEdgeType()
//                                new SimpleSearchEdgeType( id, null, null )).buffer( 1000 ).doOnNext(
//
//                                        new Action1<List<String>>() {
//                                            @Override
//                                            public void call( final List<String> strings ) {
//                                                v2Serialization.writeEdge( applicationScope, )
//                                            }
//                                        } )
//                            }
//                        } );
//                    }
//                } );
    }


    @Override
    public int getVersion() {
        return Versions.VERSION_1;
    }
}
