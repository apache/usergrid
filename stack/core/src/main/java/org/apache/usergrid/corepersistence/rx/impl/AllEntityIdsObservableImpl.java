/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.rx.impl;




import  com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.serialization.EdgesObservable;
import org.apache.usergrid.persistence.graph.serialization.TargetIdObservable;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;


/**
 * An implementation that will provide all entityId scopes in the system
 */
@Singleton
public class AllEntityIdsObservableImpl implements AllEntityIdsObservable {
    private final GraphManagerFactory graphManagerFactory;
    private final TargetIdObservable targetIdObservable;
    private final EdgesObservable edgesObservable;


    @Inject
    public AllEntityIdsObservableImpl( final GraphManagerFactory graphManagerFactory,
                                       final TargetIdObservable targetIdObservable,
                                       final EdgesObservable edgesObservable ) {
        this.graphManagerFactory = graphManagerFactory;
        this.targetIdObservable = targetIdObservable;
        this.edgesObservable = edgesObservable;
    }


    @Override
    public Observable<EntityIdScope> getEntities( final Observable<ApplicationScope> appScopes ) {

        return appScopes.flatMap( applicationScope -> {
            final GraphManager gm = graphManagerFactory.createEdgeManager( applicationScope );
            final Id applicationId = applicationScope.getApplication();

            //load all nodes that are targets of our application node.  I.E.
            // entities that have been saved
            final Observable<Id> entityNodes = targetIdObservable.getTargetNodes( gm, applicationId );


            //create our application node to emit since it's an entity as well
            final Observable<Id> applicationNode = Observable.just( applicationId );

            //merge both the specified application node and the entity node
            // so they all get used
            return Observable.merge( applicationNode, entityNodes ).
                map( id -> new EntityIdScope( applicationScope, id ) );
        } );
    }


    @Override
    public Observable<EdgeScope> getEdgesToEntities( final Observable<ApplicationScope> appScopes, final Optional<String> edgeType, final Optional<Edge> lastEdge) {

        return appScopes.flatMap( applicationScope -> {
            final GraphManager gm = graphManagerFactory.createEdgeManager( applicationScope );

            return edgesObservable.edgesFromSourceDescending( gm, applicationScope.getApplication(), edgeType, lastEdge )
                                  .map( edge -> new EdgeScope(applicationScope, edge ));
        } );
    }
}
