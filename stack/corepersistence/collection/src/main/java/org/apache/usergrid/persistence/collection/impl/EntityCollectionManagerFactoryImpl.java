/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.collection.impl;


import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import org.apache.usergrid.persistence.actorsystem.ActorSystemManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.MarkCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.MarkStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.UniqueCleanup;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.VersionCompact;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.*;
import org.apache.usergrid.persistence.collection.scheduler.CollectionExecutorScheduler;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.uniquevalues.UniqueValuesService;
import org.apache.usergrid.persistence.core.astyanax.CassandraConfig;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;



/**
 * returns Entity Collection Managers built to manage caching
 */
@Singleton
public class EntityCollectionManagerFactoryImpl implements EntityCollectionManagerFactory {

    private final WriteStart writeStart;
    private final WriteUniqueVerify writeVerifyUnique;
    private final WriteOptimisticVerify writeOptimisticVerify;
    private final WriteCommit writeCommit;
    private final RollbackAction rollback;
    private final MarkStart markStart;
    private final MarkCommit markCommit;
    private final UniqueCleanup uniqueCleanup;
    private final VersionCompact versionCompact;
    private final SerializationFig serializationFig;
    private final MvccEntitySerializationStrategy entitySerializationStrategy;
    private final UniqueValueSerializationStrategy uniqueValueSerializationStrategy;
    private final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy;
    private final Keyspace keyspace;
    private final MetricsFactory metricsFactory;
    private final RxTaskScheduler rxTaskScheduler;
    private final ActorSystemManager actorSystemManager;
    private final UniqueValuesService uniqueValuesService;

    private final CassandraConfig cassandraConfig;

    private LoadingCache<ApplicationScope, EntityCollectionManager> ecmCache =
        CacheBuilder.newBuilder().maximumSize( 1000 )
                    .build( new CacheLoader<ApplicationScope, EntityCollectionManager>() {
                        public EntityCollectionManager load( ApplicationScope scope ) {
                            //create the target EM that will perform logic
                            final EntityCollectionManager target = new EntityCollectionManagerImpl(

                                writeStart,
                                writeVerifyUnique,
                                writeOptimisticVerify,
                                writeCommit,
                                rollback,
                                markStart,
                                markCommit,
                                uniqueCleanup,
                                versionCompact,

                                entitySerializationStrategy,
                                uniqueValueSerializationStrategy,
                                mvccLogEntrySerializationStrategy,

                                keyspace,
                                metricsFactory,
                                serializationFig,
                                rxTaskScheduler,
                                actorSystemManager,
                                uniqueValuesService,
                                cassandraConfig,
                                scope );

                            return target;
                        }
                    } );


    @Inject
    public EntityCollectionManagerFactoryImpl(
            final WriteStart            writeStart,
            final WriteUniqueVerify     writeVerifyUnique,
            final WriteOptimisticVerify writeOptimisticVerify,
            final WriteCommit           writeCommit,
            final RollbackAction        rollback,
            final MarkStart             markStart,
            final MarkCommit            markCommit,
            final UniqueCleanup         uniqueCleanup,
            final VersionCompact        versionCompact,
            final SerializationFig      serializationFig,
            final MvccEntitySerializationStrategy   entitySerializationStrategy,
            final UniqueValueSerializationStrategy  uniqueValueSerializationStrategy,
            final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy,
            final Keyspace              keyspace,
            final MetricsFactory        metricsFactory,
            @CollectionExecutorScheduler
            final RxTaskScheduler       rxTaskScheduler,
            final ActorSystemManager    actorSystemManager,
            final UniqueValuesService   uniqueValuesService,
            final CassandraConfig       cassandraConfig ) {

        this.writeStart =               writeStart;
        this.writeVerifyUnique =        writeVerifyUnique;
        this.writeOptimisticVerify =    writeOptimisticVerify;
        this.writeCommit =              writeCommit;
        this.rollback =                 rollback;
        this.markStart =                markStart;
        this.markCommit =               markCommit;
        this.uniqueCleanup =            uniqueCleanup;
        this.versionCompact =           versionCompact;
        this.serializationFig =         serializationFig;
        this.entitySerializationStrategy =       entitySerializationStrategy;
        this.uniqueValueSerializationStrategy =  uniqueValueSerializationStrategy;
        this.mvccLogEntrySerializationStrategy = mvccLogEntrySerializationStrategy;
        this.keyspace =                 keyspace;
        this.metricsFactory =           metricsFactory;
        this.rxTaskScheduler =          rxTaskScheduler;
        this.actorSystemManager =       actorSystemManager;
        this.uniqueValuesService =      uniqueValuesService;
        this.cassandraConfig =          cassandraConfig;
    }

    @Override
    public EntityCollectionManager createCollectionManager(ApplicationScope applicationScope) {
        Preconditions.checkNotNull(applicationScope);
        try {
            return ecmCache.get(applicationScope);
        } catch (ExecutionException ee) {
            throw new RuntimeException(ee);
        }
    }


    @Override
    public void invalidate() {
        ecmCache.invalidateAll();
    }
}
