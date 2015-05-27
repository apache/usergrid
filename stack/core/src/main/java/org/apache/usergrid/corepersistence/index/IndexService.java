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

package org.apache.usergrid.corepersistence.index;


import java.util.UUID;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;
import rx.observables.ConnectableObservable;


/**
 * Our low level indexing service operations
 */
public interface IndexService {


    /**
     *  Perform an index update of the entity's state from Cassandra
     *
     * @param applicationScope The scope of the entity
     * @param entity The entity
     *
     * @return An Observable with executed batch future as an observable.  Note that this a cold observable
     * and must be subscribed to in order to perform the index operations.  This also makes no assumptions on scheduling.  It is up to the caller
     * to assign the correct scheduler to the observable based on their threading needs
     */
    Observable<IndexOperationMessage> indexEntity( final ApplicationScope applicationScope, final Entity entity );


    /**
     * Index the edge when an edge is created or destroyed
     * @param applicationScope
     * @param entity
     * @param edge
     * @return
     */
    Observable<IndexOperationMessage> indexEdge(final ApplicationScope applicationScope, final Entity entity, final Edge edge);


    /**
     * Delete an index edge from the specified scope
     * @param applicationScope
     * @param edge
     * @return
     */
    Observable<IndexOperationMessage> deleteIndexEdge(final ApplicationScope applicationScope, final Edge edge);




    /**
     * Delete all indexes with the specified entityId
     *
     * @param applicationScope
     * @param entityId
     * @return
     */
    Observable<IndexOperationMessage> deleteEntityIndexes(final ApplicationScope applicationScope, final Id entityId,
                                                         final UUID markedVersion);



}
