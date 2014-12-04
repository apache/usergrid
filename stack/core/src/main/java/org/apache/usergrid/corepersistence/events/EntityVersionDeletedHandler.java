/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
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

import com.google.inject.Inject;
import java.util.List;
import java.util.UUID;
import org.apache.usergrid.corepersistence.CpEntityManagerFactory;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.event.EntityVersionDeleted;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.impl.IndexScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Remove Entity index when specific version of Entity is deleted.
 * TODO: do we need this? Don't our version-created and entity-deleted handlers take care of this? 
 * If we do need it then it should be wired in via GuiceModule in the corepersistence package.
 */
public class EntityVersionDeletedHandler implements EntityVersionDeleted {

    private final SerializationFig serializationFig;

    private EntityManagerFactory emf;

    @Inject
    public EntityVersionDeletedHandler(SerializationFig fig, EntityManagerFactory emf) {
        this.serializationFig = fig;
    }

    @Override
    public void versionDeleted(
            final CollectionScope scope, final Id entityId, final List<MvccEntity> entityVersions) {

        CpEntityManagerFactory cpemf = (CpEntityManagerFactory)emf;

        final EntityIndex ei = cpemf.getManagerCache().getEntityIndex(scope);
        
        final EntityIndexBatch eibatch = ei.createBatch();

        final IndexScope indexScope = new IndexScopeImpl(
                new SimpleId(scope.getOwner().getUuid(), scope.getOwner().getType()),
                scope.getName()
        );
        rx.Observable.from(entityVersions)
            .subscribeOn(Schedulers.io())
            .buffer(serializationFig.getBufferSize())
            .map(new Func1<List<MvccEntity>, List<MvccEntity>>() {
                @Override
                public List<MvccEntity> call(List<MvccEntity> entityList) {
                    for (MvccEntity entity : entityList) {
                        eibatch.deindex(indexScope, entityId, entity.getVersion());
                    }
                    eibatch.execute();
                    return entityList;
                }
            }).toBlocking().last();
    }

}
