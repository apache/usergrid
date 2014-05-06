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
import org.apache.cassandra.cql3.CQL3Type;
import org.apache.usergrid.persistence.collection.guice.MvccEntityDelete;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccDeleteMessageListener;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityEvent;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.query.Results;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.*;

public class EsEntityIndexDeleteListener implements MvccDeleteMessageListener {

    private final EntityIndex entityIndex;
    private final SerializationFig serializationFig;

    public EsEntityIndexDeleteListener(EntityIndex entityIndex,
                                       @MvccEntityDelete final AsyncProcessor entityDelete,
                                       SerializationFig serializationFig) {
        this.entityIndex = entityIndex;
        this.serializationFig = serializationFig;
        entityDelete.addListener(this);
    }

    @Override
    public Observable<MvccEntity> receive(final MvccEntityEvent<MvccEntity> event) {
        return Observable.create(new ObservableIterator<MvccEntity>("deleteEsIndexVersions") {
            @Override
            protected Iterator<MvccEntity> getIterator() {
                Results results= entityIndex.getEntityVersions(event.getVersion(), event.getCollectionScope());
                Iterator<MvccEntity> iterator = Collections.emptyListIterator();
                if(results!=null) {
                    List<Entity> entities = results.getEntities();
                    List<MvccEntity> mvccEntities = new ArrayList<>();
                    for (Entity entity : entities) {
                        mvccEntities.add((MvccEntity) new EsMvccEntityImpl(entity));
                    }
                    iterator = mvccEntities.iterator();
                }
                return iterator;
            }
        }).subscribeOn(Schedulers.io())
                .buffer(serializationFig.getBufferSize())
                .flatMap(new Func1<List<MvccEntity>, Observable<MvccEntity>>() {
                    @Override
                    public Observable<MvccEntity> call(List<MvccEntity> entities) {
                        for (MvccEntity entity : entities) {
                            entityIndex.deindex(event.getCollectionScope(), entity.getEntity().get());
                        }
                        return Observable.from(entities);
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
