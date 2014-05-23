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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityDeleteEvent;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessorFactory;
import org.apache.usergrid.persistence.core.consistency.MessageListener;
import org.apache.usergrid.persistence.core.entity.EntityVersion;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Singleton
public class CpEntityDeleteListener implements MessageListener<MvccEntityDeleteEvent, EntityVersion> {
    private static final Logger LOG = LoggerFactory.getLogger(CpEntityDeleteListener.class);

    private final MvccEntitySerializationStrategy entityMetadataSerialization;
    private final Keyspace keyspace;
    private final SerializationFig serializationFig;

    @Inject
    public CpEntityDeleteListener(final MvccEntitySerializationStrategy entityMetadataSerialization,
                                    final AsyncProcessorFactory asyncProcessorFactory,
                                    final Keyspace keyspace,
                                    final SerializationFig serializationFig){
        this.entityMetadataSerialization = entityMetadataSerialization;
        this.keyspace = keyspace;
        this.serializationFig = serializationFig;
        asyncProcessorFactory.getProcessor( MvccEntityDeleteEvent.class ).addListener( this );
    }

    @Override
    public Observable<EntityVersion> receive(final MvccEntityDeleteEvent entityEvent) {
        final MvccEntity entity = entityEvent.getEntity();
        return Observable.create( new ObservableIterator<MvccEntity>( "deleteEntities" ) {
            @Override
            protected Iterator<MvccEntity> getIterator() {
                Iterator<MvccEntity> iterator = entityMetadataSerialization.loadHistory( entityEvent.getCollectionScope(), entity.getId(), entity.getVersion(), serializationFig.getHistorySize() );
                return iterator;
            }
        } ).subscribeOn(Schedulers.io())
                .buffer(serializationFig.getBufferSize())
                .flatMap(new Func1<List<MvccEntity>, Observable<EntityVersion>>() {
                    @Override
                    public Observable<EntityVersion> call(List<MvccEntity> mvccEntities) {
                        MutationBatch mutationBatch = keyspace.prepareMutationBatch();
                        List<EntityVersion> versions = new ArrayList<>();
                        //actually delete the edge from both the commit log and
                        for (MvccEntity mvccEntity : mvccEntities) {
                            versions.add(mvccEntity);
                            mutationBatch.mergeShallow(entityMetadataSerialization.delete(entityEvent.getCollectionScope(), mvccEntity.getId(), mvccEntity.getVersion()));
                        }
                        try {
                            mutationBatch.execute();
                        } catch (ConnectionException e) {
                            throw new RuntimeException("Unable to execute mutation", e);
                        }
                        return Observable.from(versions);
                    }
                });
    }
}
