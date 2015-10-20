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


import org.apache.usergrid.corepersistence.index.ReIndexAction;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;
import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Low level queue service for events in the entity.  These events are fire and forget, and will always be asynchronous
 */
public interface AsyncEventService extends ReIndexAction {


    /**
     * Initialize index for creation
     * @param applicationScope
     */
    void queueInitializeApplicationIndex( final ApplicationScope applicationScope );

    /**
     * Queue an entity to be indexed.  This will start processing immediately. For implementations that are realtime (akka, in memory)
     * We will return a distributed future.  For SQS impls, this will return immediately, and the result will not be available.
     * After SQS is removed, the tests should be enhanced to ensure that we're processing our queues correctly.
     * @param applicationScope
     * @param entity The entity to index.  Should be fired when an entity is updated
     */
    void queueEntityIndexUpdate( final ApplicationScope applicationScope, final Entity entity);


    /**
     * Fired when a new edge is added to an entity. Such as initial entity creation, adding to a collection, or creating a connection
     *
     * TODO: We shouldn't take an entity here, only the id.  It doesn't make sense in a distributed context to pass the entity
     *
     * @param applicationScope
     * @param entity
     * @param newEdge
     */
    void queueNewEdge(final ApplicationScope applicationScope, final Entity entity, final Edge newEdge);

    /**
     * Queue the deletion of an edge
     * @param applicationScope
     * @param edge
     */
    void queueDeleteEdge(final ApplicationScope applicationScope, final Edge edge);

    /**
     * The entity has been deleted, queue it's cleanup
     * @param applicationScope
     * @param entityId
     */
    void queueEntityDelete(final ApplicationScope applicationScope, final Id entityId);


    /**
     * current queue depth
     * @return
     */
    long getQueueDepth();



}
