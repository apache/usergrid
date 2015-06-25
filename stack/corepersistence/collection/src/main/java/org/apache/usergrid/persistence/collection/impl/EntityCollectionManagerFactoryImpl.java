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

package org.apache.usergrid.persistence.collection.impl;


import java.util.concurrent.ExecutionException;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerSync;
import org.apache.usergrid.persistence.collection.cache.CachedEntityCollectionManager;
import org.apache.usergrid.persistence.collection.cache.EntityCacheFig;
import org.apache.usergrid.persistence.collection.guice.CollectionTaskExecutor;
import org.apache.usergrid.persistence.collection.guice.Write;
import org.apache.usergrid.persistence.collection.guice.WriteUpdate;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.MarkCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.MarkStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.RollbackAction;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteOptimisticVerify;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteUniqueVerify;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.core.guice.ProxyImpl;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.task.TaskExecutor;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;



/**
 * returns Entity Collection Managers built to manage caching
 */
@Singleton
public class EntityCollectionManagerFactoryImpl implements EntityCollectionManagerFactory {


    private final WriteStart writeStart;
    private final WriteStart writeUpdate;
    private final WriteUniqueVerify writeVerifyUnique;
    private final WriteOptimisticVerify writeOptimisticVerify;
    private final WriteCommit writeCommit;
    private final RollbackAction rollback;
    private final MarkStart markStart;
    private final MarkCommit markCommit;
    private final MvccEntitySerializationStrategy entitySerializationStrategy;
    private final UniqueValueSerializationStrategy uniqueValueSerializationStrategy;
    private final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy;
    private final Keyspace keyspace;
    private final EntityVersionTaskFactory entityVersionTaskFactory;
    private final TaskExecutor taskExecutor;
    private final EntityCacheFig entityCacheFig;
    private final MetricsFactory metricsFactory;

    private LoadingCache<CollectionScope, EntityCollectionManager> ecmCache =
        CacheBuilder.newBuilder().maximumSize( 1000 )
                    .build( new CacheLoader<CollectionScope, EntityCollectionManager>() {
                        public EntityCollectionManager load( CollectionScope scope ) {

                                  //create the target EM that will perform logic
                            final EntityCollectionManager target = new EntityCollectionManagerImpl( writeStart, writeUpdate, writeVerifyUnique,
                                writeOptimisticVerify, writeCommit, rollback, markStart, markCommit,
                                entitySerializationStrategy, uniqueValueSerializationStrategy,
                                mvccLogEntrySerializationStrategy, keyspace, entityVersionTaskFactory,
                                taskExecutor, scope, metricsFactory );


                            final EntityCollectionManager proxy = new CachedEntityCollectionManager(entityCacheFig, target  );

                            return proxy;
//                            return target;
                        }
                    } );


    @Inject
    public EntityCollectionManagerFactoryImpl( @Write final WriteStart writeStart,
                                               @WriteUpdate final WriteStart writeUpdate,
                                               final WriteUniqueVerify writeVerifyUnique,
                                               final WriteOptimisticVerify writeOptimisticVerify,
                                               final WriteCommit writeCommit, final RollbackAction rollback,
                                               final MarkStart markStart, final MarkCommit markCommit, @ProxyImpl
                                               final MvccEntitySerializationStrategy entitySerializationStrategy,
                                               final UniqueValueSerializationStrategy uniqueValueSerializationStrategy,
                                               final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy,
                                               final Keyspace keyspace,
                                               final EntityVersionTaskFactory entityVersionTaskFactory,
                                               @CollectionTaskExecutor final TaskExecutor taskExecutor,
                                              final EntityCacheFig entityCacheFig,
                                               MetricsFactory metricsFactory) {

        this.writeStart = writeStart;
        this.writeUpdate = writeUpdate;
        this.writeVerifyUnique = writeVerifyUnique;
        this.writeOptimisticVerify = writeOptimisticVerify;
        this.writeCommit = writeCommit;
        this.rollback = rollback;
        this.markStart = markStart;
        this.markCommit = markCommit;
        this.entitySerializationStrategy = entitySerializationStrategy;
        this.uniqueValueSerializationStrategy = uniqueValueSerializationStrategy;
        this.mvccLogEntrySerializationStrategy = mvccLogEntrySerializationStrategy;
        this.keyspace = keyspace;
        this.entityVersionTaskFactory = entityVersionTaskFactory;
        this.taskExecutor = taskExecutor;
        this.entityCacheFig = entityCacheFig;
        this.metricsFactory = metricsFactory;
    }


    @Override
    public EntityCollectionManager createCollectionManager( CollectionScope collectionScope ) {
        Preconditions.checkNotNull( collectionScope );
        try {
            return ecmCache.get( collectionScope );
        }
        catch ( ExecutionException ee ) {
            throw new RuntimeException( ee );
        }
    }


    @Override
    public EntityCollectionManagerSync createCollectionManagerSync( CollectionScope collectionScope ) {
        return new EntityCollectionManagerSyncImpl( this, collectionScope );
    }
}
