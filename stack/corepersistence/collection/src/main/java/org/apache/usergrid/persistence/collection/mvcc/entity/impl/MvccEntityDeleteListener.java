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

package org.apache.usergrid.persistence.collection.mvcc.entity.impl;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.apache.usergrid.persistence.collection.guice.MvccEntityDelete;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

/**
 * Listens for delete entity event then deletes entity for real this time
 */
public class MvccEntityDeleteListener implements MessageListener<MvccEntityEvent<MvccEntity>,MvccEntityEvent<MvccEntity>> {

    private static final Logger LOG = LoggerFactory.getLogger(MvccEntityDeleteListener.class);

    private final MvccEntitySerializationStrategy entityMetadataSerialization;

    public MvccEntityDeleteListener(final MvccEntitySerializationStrategy entityMetadataSerialization,
                                    @MvccEntityDelete final AsyncProcessor<MvccEntityEvent<MvccEntity>> entityDelete){
        this.entityMetadataSerialization = entityMetadataSerialization;
        entityDelete.addListener( this );
    }

    @Override
    public Observable<MvccEntityEvent<MvccEntity>> receive(final MvccEntityEvent<MvccEntity> entityEvent) {
        final MvccEntity entity = entityEvent.getData();
        return Observable.from(entity).map( new Func1<MvccEntity, MutationBatch>() {
            @Override
            public MutationBatch call(MvccEntity mvccEntity) {
               return entityMetadataSerialization.delete(entityEvent.getCollectionScope(),entity.getId(),entity.getVersion());
            }
        }).map(new Func1<MutationBatch, MvccEntityEvent<MvccEntity>>() {
            @Override
            public MvccEntityEvent<MvccEntity> call(final MutationBatch mutationBatch) {

                //actually delete the edge from both the commit log and
                try {
                    mutationBatch.execute();
                } catch (ConnectionException e) {
                    throw new RuntimeException("Unable to execute mutation", e);
                }

                return entityEvent;
            }
        });
    }
}
