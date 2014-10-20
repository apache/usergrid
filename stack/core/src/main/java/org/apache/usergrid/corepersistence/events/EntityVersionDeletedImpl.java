/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

package org.apache.usergrid.corepersistence.events;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.event.EntityVersionDeleted;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.impl.IndexScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.List;


/**
 * Purge old entity versions
 */
public class EntityVersionDeletedImpl implements EntityVersionDeleted{

    private final EntityIndexBatch entityIndexBatch;
    private final SerializationFig serializationFig;

    public EntityVersionDeletedImpl(EntityIndexBatch entityIndexBatch, SerializationFig fig){
        this.entityIndexBatch = entityIndexBatch;
        this.serializationFig = fig;
    }

    @Override
    public void versionDeleted(
            final CollectionScope scope, final Id entityId, final List<MvccEntity> entityVersions) {
        final IndexScope indexScope = new IndexScopeImpl(
                new SimpleId(scope.getOwner().getUuid(),scope.getOwner().getType()),
                scope.getName()
        );
        rx.Observable.from(entityVersions)
                .subscribeOn(Schedulers.io())
                .buffer(serializationFig.getBufferSize())
                .map(new Func1<List<MvccEntity>,List<MvccEntity>>() {
                    @Override
                    public List<MvccEntity> call(List<MvccEntity> entityList) {
                        for(MvccEntity entity : entityList){
                             entityIndexBatch.deindex(indexScope,entityId,entity.getVersion());
                        }
                        entityIndexBatch.execute();
                        return entityList;
                    }
                }).toBlocking().last();
    }
}
