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
package org.apache.usergrid.persistence.collection.guice;


import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import com.netflix.astyanax.Keyspace;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccDeleteMessageListener;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityDeleteListener;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityEvent;
import org.apache.usergrid.persistence.core.consistency.*;
import org.safehaus.guicyfig.GuicyFigModule;

import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerSync;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerImpl;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerSyncImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.UniqueValueSerializationStrategyImpl;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.impl.SerializationModule;
import org.apache.usergrid.persistence.collection.service.impl.ServiceModule;
import org.apache.usergrid.persistence.core.guice.CommonModule;

import com.google.inject.assistedinject.FactoryModuleBuilder;



/**
 * Simple module for wiring our collection api
 *
 * @author tnine
 */
public class CollectionModule extends AbstractModule {


    @Override
    protected void configure() {
        install( new CommonModule());
        //noinspection unchecked
        install( new GuicyFigModule(
                SerializationFig.class ) );

        install( new SerializationModule() );
        install( new ServiceModule() );

        // create a guice factor for getting our collection manager
        install( new FactoryModuleBuilder()
                .implement( EntityCollectionManager.class, EntityCollectionManagerImpl.class )
                .implement( EntityCollectionManagerSync.class, EntityCollectionManagerSyncImpl.class )
                .build( EntityCollectionManagerFactory.class ) );

        bind( UniqueValueSerializationStrategy.class ).to( UniqueValueSerializationStrategyImpl.class );
        Multibinder<MessageListener> messageListenerMultibinder = Multibinder.newSetBinder(binder(), MessageListener.class);

        messageListenerMultibinder.addBinding().toProvider( MvccEntityDeleteListenerProvider.class ).asEagerSingleton();
    }

    @Provides
    @Singleton
    @Inject
    @MvccEntityDelete
    public AsyncProcessor<MvccEntityEvent<MvccEntity>> edgeDelete(@MvccEntityDelete final TimeoutQueue<MvccEntityEvent<MvccEntity>> queue, final ConsistencyFig consistencyFig) {
        return new AsyncProcessorImpl<>(queue, consistencyFig);
    }


    @Provides
    @Inject
    @Singleton
    @MvccEntityDelete
    public TimeoutQueue<MvccEntityEvent<MvccEntity>> edgeDeleteQueue(final TimeService timeService) {
        return new LocalTimeoutQueue<>(timeService);
    }


    /**
     * Create the provider for the entity delete listener
     */
    public static class MvccEntityDeleteListenerProvider
            implements Provider<MvccDeleteMessageListener> {


        private final MvccEntitySerializationStrategy entitySerialization;
        private final AsyncProcessor<MvccEntityEvent<MvccEntity>> entityDelete;
        private final Keyspace keyspace;
        private final SerializationFig serializationFig;


        @Inject
        public MvccEntityDeleteListenerProvider( final MvccEntitySerializationStrategy entitySerialization,
                                           @MvccEntityDelete final AsyncProcessor<MvccEntityEvent<MvccEntity>> entityDelete,
                                           final Keyspace keyspace,
                                           final SerializationFig serializationFig) {
            this.entitySerialization = entitySerialization;
            this.entityDelete = entityDelete;
            this.serializationFig = serializationFig;
            this.keyspace = keyspace;
        }

        @Override
        public MvccDeleteMessageListener get() {
            return new MvccEntityDeleteListener( entitySerialization,entityDelete,keyspace,serializationFig  );
        }
    }
}
