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
package org.apache.usergrid.persistence.index.impl;

import com.google.common.base.Optional;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.guice.MvccEntityDelete;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccDeleteMessageListener;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityEvent;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.entity.EntityVersion;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.*;
import org.apache.usergrid.persistence.index.query.CandidateResult;

public class EsEntityIndexDeleteListener implements MvccDeleteMessageListener {

    private final SerializationFig serializationFig;
    private final EntityIndexFactory entityIndexFactory;
    private final EntityCollectionManager entityCollectionManager;

    public EsEntityIndexDeleteListener(EntityIndexFactory entityIndexFactory,
                                       @MvccEntityDelete final AsyncProcessor entityDelete,
                                       SerializationFig serializationFig,
                                       EntityCollectionManager collectionManager) {
        this.entityIndexFactory = entityIndexFactory;
        this.serializationFig = serializationFig;
        this.entityCollectionManager = collectionManager;
        entityDelete.addListener(this);
    }

    @Override
    public Observable<EntityVersion> receive(final MvccEntityEvent<MvccEntity> event) {
        CollectionScope collectionScope = event.getCollectionScope();
        IndexScope indexScope = new IndexScopeImpl(collectionScope.getApplication(),collectionScope.getOwner(),collectionScope.getName());
        final EntityIndex entityIndex = entityIndexFactory.createEntityIndex(indexScope);
        return Observable.create(new ObservableIterator<CandidateResult>("deleteEsIndexVersions") {
            @Override
            protected Iterator<CandidateResult> getIterator() {
                CandidateResults results = entityIndex.getEntityVersions(event.getData().getId());
                return results.iterator();
            }
        }).subscribeOn(Schedulers.io())
                .buffer(serializationFig.getBufferSize())
                .flatMap( new Func1<List<CandidateResult>, Observable<? extends EntityVersion>>() {
                    @Override
                    public Observable<? extends EntityVersion> call(List<CandidateResult> candidateResults) {
                        List<EntityVersion> versions = new ArrayList<>();
                        for (CandidateResult entity : candidateResults) {
                            //filter find entities <= current version
                            if(entity.getVersion().timestamp() <= event.getVersion().timestamp()) {
                                versions.add(entity);
                                entityIndex.deindex(entity.getId(),entity.getVersion());
                            }
                        }
                        return Observable.from(versions);
                    }
                });
    }

    public class EsMvccEntityImpl implements MvccEntity{

        UUID version;
        Id id;
        Entity entity;

        public EsMvccEntityImpl(Entity entity){
            this.entity = entity;
            this.id = entity.getId();
            this.version = entity.getVersion();
        }

        @Override
        public Optional<Entity> getEntity() {
            return Optional.fromNullable(entity) ;
        }

        @Override
        public UUID getVersion() {
            return version;
        }

        @Override
        public Id getId() {
            return id;
        }

        @Override
        public Status getStatus() {
            return Status.DELETED;
        }
    }
}
