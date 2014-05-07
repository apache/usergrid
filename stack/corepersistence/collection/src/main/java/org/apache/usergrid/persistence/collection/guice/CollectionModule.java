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


import org.safehaus.guicyfig.GuicyFigModule;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerSync;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerImpl;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerListener;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerSyncImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.load.Load;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.UniqueValueSerializationStrategyImpl;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.impl.SerializationModule;
import org.apache.usergrid.persistence.collection.service.impl.ServiceModule;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessorImpl;
import org.apache.usergrid.persistence.core.consistency.ConsistencyFig;
import org.apache.usergrid.persistence.core.consistency.LocalTimeoutQueue;
import org.apache.usergrid.persistence.core.consistency.MessageListener;
import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.consistency.TimeoutQueue;
import org.apache.usergrid.persistence.core.guice.CommonModule;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;


/**
 * Simple module for wiring our collection api
 *
 * @author tnine
 */
public class CollectionModule extends AbstractModule {


    @Override
    protected void configure() {
        install( new CommonModule() );
        //noinspection unchecked
        install( new GuicyFigModule( SerializationFig.class ) );

        install( new SerializationModule() );
        install( new ServiceModule() );

        // create a guice factor for getting our collection manager
        install(
                new FactoryModuleBuilder().implement( EntityCollectionManager.class, EntityCollectionManagerImpl.class )
                                          .implement( EntityCollectionManagerSync.class,
                                                  EntityCollectionManagerSyncImpl.class )
                                          .build( EntityCollectionManagerFactory.class ) );

        bind( UniqueValueSerializationStrategy.class ).to( UniqueValueSerializationStrategyImpl.class );

        /**
         * Bindings to fire up all our message listener implementations.  These will bind their dependencies
         */
        Multibinder<MessageListener> messageListenerMultibinder =
                Multibinder.newSetBinder( binder(), MessageListener.class );

        messageListenerMultibinder.addBinding().toProvider( EntityCollectionListenerProvider.class ).asEagerSingleton();
    }

    /**
     * Create the processor for edge deletes
     */
    @Provides
    @Singleton
    @Inject
    @EntityUpdate
    public AsyncProcessor<Entity> entityUpdate( @EntityUpdate final TimeoutQueue<Entity> queue,
                                                       final ConsistencyFig consistencyFig ) {
        return new AsyncProcessorImpl<>( queue, consistencyFig );
    }

    @Provides
    @Inject
    @Singleton
    @EntityUpdate
    public TimeoutQueue<Entity> entityUpdateQueue( final TimeService timeService ) {
        return new LocalTimeoutQueue<>( timeService );
    }


    /**
     * Create the provider for the node delete listener
     */
    public static class EntityCollectionListenerProvider implements Provider<MessageListener<Entity, Entity>>  {

        private final Load load;
        private final CollectionScope collectionScope;
        private final AsyncProcessor<Entity> entityUpdate;



        @Inject
        public EntityCollectionListenerProvider( final Load load,
                                                 final CollectionScope collectionScope,
                                                 @EntityUpdate final AsyncProcessor<Entity> entityUpdate) {

            this.load = load;
            this.collectionScope = collectionScope;
            this.entityUpdate = entityUpdate;
        }


        @Override
        public MessageListener<Entity, Entity> get() {
            return new EntityCollectionManagerListener(collectionScope,load,entityUpdate);
        }
    }
}
