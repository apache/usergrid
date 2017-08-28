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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;

/**
 * Event that will signal to finish the actual delete (post-mark delete) for an Entity
 * It will mark if this is for a collection delete
 */
public final class EntityDeleteEvent extends AsyncEvent {


    @JsonProperty
    protected EntityIdScope entityIdScope;

    @JsonProperty
    private long updatedBefore;

    @JsonProperty
    private boolean isCollectionDelete;

    public EntityDeleteEvent() {
        super();
    }

    public EntityDeleteEvent(String sourceRegion, EntityIdScope entityIdScope) {
        super(sourceRegion);
        this.entityIdScope = entityIdScope;
        this.updatedBefore = Long.MAX_VALUE;
        this.isCollectionDelete = false;
    }

    public EntityDeleteEvent(String sourceRegion, EntityIdScope entityIdScope,
                             boolean isCollectionDelete, long updatedBefore) {
        super(sourceRegion);
        this.entityIdScope = entityIdScope;
        this.updatedBefore = updatedBefore;
        this.isCollectionDelete = isCollectionDelete;
    }


    public EntityIdScope getEntityIdScope() {
        return entityIdScope;
    }

    public long getUpdatedBefore() {
        return updatedBefore;
    }

    public boolean isCollectionDelete() {
        return isCollectionDelete;
    }
}
