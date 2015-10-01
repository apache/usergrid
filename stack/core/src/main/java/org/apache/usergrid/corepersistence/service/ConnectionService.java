/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.corepersistence.service;


import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;


/**
 * Interface for operating on connections
 */
public interface ConnectionService {


    /**
     * Search a collection and return an observable of results pages
     * @param search The search to perform
     * @return An observable with results page entries for the stream
     */
    Observable<ResultsPage<Entity>> searchConnection(final ConnectionSearch search);


    /**
     * Search the connections and return ids instead of entities in results pages
     * @param search
     * @return
     */
    Observable<ResultsPage<ConnectionRef>> searchConnectionAsRefs( final ConnectionSearch search );


    /**
     * An observable that will remove duplicate edges from the graph that represent connections.  All emitted scopes are scopes that have been deleted.
     *
     * @param applicationScopeObservable
     * @return
     */
    Observable<ConnectionScope> deDupeConnections(final Observable<ApplicationScope> applicationScopeObservable);
}
