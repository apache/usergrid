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
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.usergrid.corepersistence.rx.impl.AllEntityIdsObservable;
import org.apache.usergrid.corepersistence.rx.impl.EdgeScope;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import com.google.common.base.Optional;

import rx.Observable;
import rx.Observer;
import rx.observables.ConnectableObservable;


/**
 * An interface for re-indexing all entities in an application
 */
public interface ReIndexService {


    /**
     * Perform an index rebuild
     * @param appId
     * @param collection
     * @return
     */
    IndexResponse rebuildIndex( final Optional<UUID> appId, final Optional<String> collection, final Optional<String> collectionName, final Optional<String> cursor,
                        final Optional<Long> startTimestamp );



    /**
     * The response when requesting a re-index operation
     */
    class IndexResponse {
        final String cursor;
        final ConnectableObservable<EdgeScope> indexedEdgecount;


        public IndexResponse( final String cursor, final ConnectableObservable<EdgeScope> indexedEdgecount ) {
            this.cursor = cursor;
            this.indexedEdgecount = indexedEdgecount;
        }


        /**
         * Get the cursor used to resume this operation
         * @return
         */
        public String getCursor() {
            return cursor;
        }


        /**
         * Return the observable of all edges to be indexed.
         *
         * Note that after subscribing "connect" will need to be called to ensure that processing begins
         * @return
         */
        public ConnectableObservable<EdgeScope> getCount() {
            return indexedEdgecount;
        }
    }
}
