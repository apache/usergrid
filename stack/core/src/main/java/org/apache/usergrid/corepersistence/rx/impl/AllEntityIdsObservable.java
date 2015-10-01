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

import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;

import rx.Observable;


/**
 * An implementation that will provide all entityId scopes in the system
 */
public interface AllEntityIdsObservable {

    /**
     * Return an observable of scopes from the given appScopes
     * @param appScopes
     * @return An observable of entityId scopes
     */
    Observable<EntityIdScope> getEntities( final Observable<ApplicationScope> appScopes );

    /**
     * Get all edges that represent edges to entities in the system
     * @param appScopes
     * @param edgeType The edge type to use (if specified)
     * @param lastEdge The edge to resume processing from
     * @return
     */
    Observable<EdgeScope> getEdgesToEntities(final Observable<ApplicationScope> appScopes, final Optional<String> edgeType, final Optional<Edge> lastEdge);

}
