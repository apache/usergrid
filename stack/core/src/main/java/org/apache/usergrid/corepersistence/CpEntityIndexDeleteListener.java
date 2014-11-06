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
package org.apache.usergrid.corepersistence;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityDeleteEvent;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.entity.EntityVersion;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.impl.IndexScopeImpl;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.index.query.CandidateResults;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Listener for cleans up old indexes and deletes from indexer
 */
@Singleton
public class CpEntityIndexDeleteListener {

    private final SerializationFig serializationFig;
    private final EntityIndexFactory entityIndexFactory;


    @Inject
    public CpEntityIndexDeleteListener(final EntityIndexFactory entityIndexFactory,
                                       SerializationFig serializationFig) {
        this.entityIndexFactory = entityIndexFactory;
        this.serializationFig = serializationFig;
    }


    public Observable<EntityVersion> receive(final MvccEntityDeleteEvent event) {

        final CollectionScope collectionScope = event.getCollectionScope();
        final IndexScope indexScope = 
                new IndexScopeImpl(collectionScope.getOwner(), collectionScope.getName(), entityType );
        final EntityIndex entityIndex = entityIndexFactory.createEntityIndex(
                new ApplicationScopeImpl( collectionScope.getApplication()));

        return Observable.create(new ObservableIterator<CandidateResult>("deleteEsIndexVersions") {
            @Override
            protected Iterator<CandidateResult> getIterator() {
                CandidateResults results = 
                        entityIndex.getEntityVersions(indexScope, event.getEntity().getId());
                return results.iterator();
            }
        }).subscribeOn(Schedulers.io())
                .buffer(serializationFig.getBufferSize())
                .flatMap(new Func1<List<CandidateResult>, Observable<? extends EntityVersion>>() {

                    @Override
                    public Observable<? extends EntityVersion> call(List<CandidateResult> crs) {
                        List<EntityVersion> versions = new ArrayList<>();
                        for (CandidateResult entity : crs) {
                            //filter find entities <= current version
                            if (entity.getVersion().timestamp() <= event.getVersion().timestamp()) {
                                versions.add(entity);
                                entityIndex.createBatch()
                                        .deindex(indexScope, entity.getId(), entity.getVersion());
                            }
                        }
                        return Observable.from(versions);
                    }
                });
    }
}
