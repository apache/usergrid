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

package org.apache.usergrid.corepersistence.asyncevents;


import java.util.List;

import org.apache.usergrid.corepersistence.index.EntityIndexOperation;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;


/**
 * Interface for constructing an observable stream to perform asynchonous events
 */
public interface EventBuilder {

    /**
     * Return the cold observable of entity index update operations
     * @param applicationScope
     * @param entity
     * @return
     */
    Observable<IndexOperationMessage> buildEntityIndexUpdate( ApplicationScope applicationScope, Entity entity );

    /**
     * Return the cold observable of the new edge operation
     * @param applicationScope
     * @param entity
     * @param newEdge
     * @return
     */
    Observable<IndexOperationMessage> buildNewEdge( ApplicationScope applicationScope, Entity entity, Edge newEdge );

    /**
     * Return the cold observable of the deleted edge operations
     * @param applicationScope
     * @param edge
     * @return
     */
    Observable<IndexOperationMessage> buildDeleteEdge( ApplicationScope applicationScope, Edge edge );

    /**
     * Return a ben with 2 obervable streams for entity delete.
     * @param applicationScope
     * @param entityId
     * @return
     */
    EventBuilderImpl.EntityDeleteResults buildEntityDelete( ApplicationScope applicationScope, Id entityId );

    /**
     * Re-index an entity in the scope provided
     * @param entityIndexOperation
     * @return
     */
    Observable<IndexOperationMessage> buildEntityIndex( EntityIndexOperation entityIndexOperation );

    /**
     * A bean to hold both our observables so the caller can choose the subscription mechanism.  Note that
     * indexOperationMessages should be subscribed and completed BEFORE the getEntitiesCompacted is subscribed
     */
    final class EntityDeleteResults {
        private final Observable<IndexOperationMessage> indexOperationMessageObservable;
        private final Observable<List<MvccLogEntry>> entitiesCompacted;


        public EntityDeleteResults( final Observable<IndexOperationMessage> indexOperationMessageObservable,
                                    final Observable<List<MvccLogEntry>> entitiesCompacted ) {
            this.indexOperationMessageObservable = indexOperationMessageObservable;
            this.entitiesCompacted = entitiesCompacted;
        }


        public Observable<IndexOperationMessage> getIndexObservable() {
            return indexOperationMessageObservable;
        }


        public Observable<List<MvccLogEntry>> getEntitiesCompacted() {
            return entitiesCompacted;
        }
    }
}
