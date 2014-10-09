/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.event;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.event.EntityDeleted;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.impl.IndexScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import java.util.UUID;

/**
 * purge most current entity
 */
public class EntityDeletedImpl implements EntityDeleted {

    private final EntityIndexBatch entityIndex;

    public EntityDeletedImpl(EntityIndexBatch entityIndex){
        this.entityIndex = entityIndex;
    }

    @Override
    public void deleted(CollectionScope scope, Id entityId, UUID version) {
        IndexScope indexScope = new IndexScopeImpl(
                new SimpleId(scope.getOwner().getUuid(),scope.getOwner().getType()),
                scope.getName()
        );
        entityIndex.deindex(indexScope,entityId,version);

    }
}
