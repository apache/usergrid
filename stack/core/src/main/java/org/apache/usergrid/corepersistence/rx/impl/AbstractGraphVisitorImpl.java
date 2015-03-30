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


import org.apache.usergrid.corepersistence.AllApplicationsObservable;
import org.apache.usergrid.persistence.core.migration.data.MigrationDataProvider;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.serialization.TargetIdObservable;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;

import rx.Observable;
import rx.functions.Func1;


/**
 * An observable that will emit every entity Id stored in our entire system across all apps.
 * Note that this only walks each application applicationId graph, and emits edges from the applicationId and it's edges as the s
 * source node
 */
public abstract class AbstractGraphVisitorImpl<T> implements MigrationDataProvider<T> {

    private final AllApplicationsObservable applicationObservable;
    private final GraphManagerFactory graphManagerFactory;
    private final TargetIdObservable targetIdObservable;

    @Inject
    public AbstractGraphVisitorImpl( AllApplicationsObservable applicationObservable,
                                     GraphManagerFactory graphManagerFactory, TargetIdObservable targetIdObservable ) {

        this.applicationObservable = applicationObservable;
        this.graphManagerFactory = graphManagerFactory;
        this.targetIdObservable = targetIdObservable;
    }



    @Override
    public Observable<T> getData() {
        return applicationObservable.getData().flatMap( new Func1<ApplicationScope, Observable<T>>() {
            @Override
            public Observable<T> call( final ApplicationScope applicationScope ) {
                return getAllEntities( applicationScope );
            }
        } );

    }


    private Observable<T> getAllEntities(final ApplicationScope applicationScope) {
        final GraphManager gm = graphManagerFactory.createEdgeManager(applicationScope);
        final Id applicationId = applicationScope.getApplication();

        //load all nodes that are targets of our application node.  I.E.
        // entities that have been saved
        final Observable<Id> entityNodes =
            targetIdObservable.getTargetNodes(gm, applicationId);

        //emit Scope + ID

        //create our application node to emit since it's an entity as well
        final Observable<Id> applicationNode = Observable.just(applicationId);

        //merge both the specified application node and the entity node
        // so they all get used
        return Observable
            .merge( applicationNode, entityNodes ).
            map( new Func1<Id, T>() {
                @Override
                public T call( final Id id ) {
                   return generateData(applicationScope, id);
                }
            } );
    }


    /**
     * Generate the data for the observable stream from the scope and the node id
     * @param applicationScope
     * @param nodeId
     * @return
     */
    protected abstract T generateData(final ApplicationScope applicationScope, final Id nodeId);


}
