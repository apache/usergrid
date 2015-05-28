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

package org.apache.usergrid.corepersistence.asyncevents.model;

import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.model.entity.Id;

/**
 * Created by Jeff West on 5/25/15.
 */
public class AsyncEvent {

    protected EventType eventType;

    protected EntityIdScope entityIdScope;
    protected ApplicationScope applicationScope;
    protected Id entityId;
    protected Edge edge;

    /**
     * required for jackson, do not delete
     */

    protected AsyncEvent() {
    }

    public AsyncEvent(final EventType eventType,
                      final EntityIdScope entityIdScope) {

        this.eventType = eventType;
        this.entityIdScope = entityIdScope;
    }

    public AsyncEvent(EventType eventType, ApplicationScope applicationScope, Edge edge) {
        this.eventType = eventType;
        this.applicationScope = applicationScope;
        this.edge = edge;
    }

    public AsyncEvent(EventType eventType, ApplicationScope applicationScope, Id entityId, Edge edge) {
        this.eventType = eventType;
        this.applicationScope = applicationScope;
        this.edge = edge;
        this.entityId = entityId;
    }

    public final Id getEntityId() {
        return entityId;
    }

    protected void setEntityId(Id entityId) {
        this.entityId = entityId;
    }

    public final EventType getEventType() {
        return eventType;
    }

    protected void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public EntityIdScope getEntityIdScope() {
        return entityIdScope;
    }

    protected void setEntityIdScope(EntityIdScope entityIdScope) {
        this.entityIdScope = entityIdScope;
    }

    public ApplicationScope getApplicationScope() {
        return applicationScope;
    }

    protected void setApplicationScope(ApplicationScope applicationScope) {
        this.applicationScope = applicationScope;
    }

    public Edge getEdge() {
        return edge;
    }

    protected void setEdge(Edge edge) {
        this.edge = edge;
    }

    public enum EventType {
        EDGE_DELETE,
        EDGE_INDEX,
        ENTITY_DELETE,
        ENTITY_INDEX;


        public String asString() {
            return toString();
        }
    }
}
