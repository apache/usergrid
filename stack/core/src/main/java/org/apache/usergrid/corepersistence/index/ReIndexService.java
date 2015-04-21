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


import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.usergrid.corepersistence.rx.impl.AllEntityIdsObservable;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import com.google.common.base.Optional;

import rx.Observable;
import rx.Observer;


/**
 * An interface for re-indexing all entities in an application
 */
public interface ReIndexService {


    /**
     * Reindex all applications using the cursor provided
     *
     * @param startTimestamp The timestamp to start seeking from
     *
     * @return a cursor that can be used to resume the operation on the next run
     */
    IndexResponse reIndex( final rx.Observable<ApplicationScope> applicationScopes, final Optional<String> cursor,
                    final Optional<Long> startTimestamp, final IndexAction indexAction );


    /**
     * The response when requesting a re-index operation
     */
    class IndexResponse {
        final String cursor;
        final Observable<Long> indexedEdgecount;


        public IndexResponse( final String cursor, final Observable<Long> indexedEdgecount ) {
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
         * Return the observable long count of all edges indexed
         * @return
         */
        public Observable<Long> getCount() {
            return indexedEdgecount;
        }
    }




    /**
     * Callback to perform an index operation based on an scope during bulk re-index operations
     */
    @FunctionalInterface
    interface IndexAction {

        void index( final EntityIdScope entityIdScope );
    }
}
