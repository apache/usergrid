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


import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.core.rx.AllEntitiesInSystemObservable;
import org.apache.usergrid.persistence.core.rx.ApplicationObservable;
import org.apache.usergrid.persistence.core.scope.ApplicationEntityGroup;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.scope.EntityIdScope;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.serialization.TargetIdObservable;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;
import rx.functions.Func1;


/**
 * An observable that will emit every entity Id stored in our entire system across all apps.
 * Note that this only walks each application applicationId graph, and emits edges from the applicationId and it's edges as the s
 * source node
 */
public class AllEntitiesInSystemObservableImpl implements AllEntitiesInSystemObservable<CollectionScope> {

    private final ApplicationObservable applicationObservable;
    private final GraphManagerFactory graphManagerFactory;
    private final TargetIdObservable targetIdObservable;

    @Inject
    public AllEntitiesInSystemObservableImpl(ApplicationObservable applicationObservable, GraphManagerFactory graphManagerFactory, TargetIdObservable targetIdObservable){

        this.applicationObservable = applicationObservable;
        this.graphManagerFactory = graphManagerFactory;
        this.targetIdObservable = targetIdObservable;
    }

    public  Observable<ApplicationEntityGroup<CollectionScope>> getAllEntitiesInSystem(final int bufferSize) {
        return getAllEntitiesInSystem(applicationObservable.getAllApplicationIds( ),bufferSize);

    }

    public  Observable<ApplicationEntityGroup<CollectionScope>> getAllEntitiesInSystem(final Observable<Id> appIdsObservable, final int bufferSize) {
        //traverse all nodes in the graph, load all source edges from them, then re-save the meta data
        return appIdsObservable.flatMap(new Func1<Id, Observable<ApplicationEntityGroup<CollectionScope>>>() {
            @Override
            public Observable<ApplicationEntityGroup<CollectionScope>> call(final Id applicationId) {

                //set up our application scope and graph manager
                final ApplicationScope applicationScope = new ApplicationScopeImpl(
                    applicationId);

                final GraphManager gm = graphManagerFactory.createEdgeManager(applicationScope);

                //load all nodes that are targets of our application node.  I.E.
                // entities that have been saved
                final Observable<Id> entityNodes =
                    targetIdObservable.getTargetNodes(gm, applicationId);


                //get scope here


                //emit Scope + ID

                //create our application node to emit since it's an entity as well
                final Observable<Id> applicationNode = Observable.just(applicationId);

                //merge both the specified application node and the entity node
                // so they all get used
                return Observable
                    .merge(applicationNode, entityNodes)
                    .buffer(bufferSize)
                    .map(new Func1<List<Id>, List<EntityIdScope<CollectionScope>>>() {
                        @Override
                        public List<EntityIdScope<CollectionScope>> call(List<Id> ids) {
                            List<EntityIdScope<CollectionScope>> scopes = new ArrayList<>(ids.size());
                            for (Id id : ids) {
                                CollectionScope scope = CpNamingUtils.getCollectionScopeNameFromEntityType(applicationId, id.getType());
                                EntityIdScope<CollectionScope> idScope = new EntityIdScope<>(id, scope);
                                scopes.add(idScope);
                            }
                            return scopes;
                        }
                    })
                    .map(new Func1<List<EntityIdScope<CollectionScope>>, ApplicationEntityGroup<CollectionScope>>() {
                        @Override
                        public ApplicationEntityGroup<CollectionScope> call(final List<EntityIdScope<CollectionScope>> scopes) {
                            return new ApplicationEntityGroup<>(applicationScope, scopes);
                        }
                    });
            }
                                    } );
    }



}
