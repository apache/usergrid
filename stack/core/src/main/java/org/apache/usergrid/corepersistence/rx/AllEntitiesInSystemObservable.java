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

package org.apache.usergrid.corepersistence.rx;


import java.util.List;

import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;
import rx.functions.Func1;


/**
 * An observable that will emit every entity Id stored in our entire system across all apps.
 * Note that this only walks each application applicationId graph, and emits edges from the applicationId and it's edges as the s
 * source node
 */
public class AllEntitiesInSystemObservable {


    /**
     * Return an observable that emits all entities in the system.
     * @param managerCache the managerCache to use
     * @param bufferSize The amount of entityIds to buffer into each ApplicationEntityGroup.  Note that if we exceed the buffer size
     * you may be more than 1 ApplicationEntityGroup with the same application and different ids
     */
    public static Observable<ApplicationEntityGroup> getAllEntitiesInSystem( final ManagerCache managerCache, final int bufferSize) {
        //traverse all nodes in the graph, load all source edges from them, then re-save the meta data
        return ApplicationObservable.getAllApplicationIds( managerCache )

                                    .flatMap( new Func1<Id, Observable<ApplicationEntityGroup>>() {
                                        @Override
                                        public Observable<ApplicationEntityGroup> call( final Id applicationId ) {

                                            //set up our application scope and graph manager
                                            final ApplicationScope applicationScope = new ApplicationScopeImpl(
                                                    applicationId );


                                            final GraphManager gm =
                                                    managerCache.getGraphManager( applicationScope );


                                            //load all nodes that are targets of our application node.  I.E.
                                            // entities that have been saved
                                            final Observable<Id> entityNodes =
                                                    TargetIdObservable.getTargetNodes(gm, applicationId );

                                            //create our application node to emit since it's an entity as well
                                            final Observable<Id> applicationNode = Observable.just( applicationId );

                                            //merge both the specified application node and the entity node
                                            // so they all get used
                                            return Observable.merge( applicationNode, entityNodes ).buffer(bufferSize)
                                                             .map( new Func1<List<Id>, ApplicationEntityGroup>() {
                                                                 @Override
                                                                 public ApplicationEntityGroup call( final List<Id> id ) {
                                                                     return new ApplicationEntityGroup( applicationScope, id );
                                                                 }
                                                             } );
                                        }
                                    } );
    }


    /**
     * Get the entity data.  Immutable bean for fast access
     */
    public static final class ApplicationEntityGroup {
        public final ApplicationScope applicationScope;
        public final List<Id> entityIds;


        public ApplicationEntityGroup( final ApplicationScope applicationScope, final List<Id> entityIds ) {
            this.applicationScope = applicationScope;
            this.entityIds = entityIds;
        }
    }
}
