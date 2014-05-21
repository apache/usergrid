/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.corepersistence;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerSync;
import org.apache.usergrid.persistence.collection.guice.MvccEntityDelete;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerImpl;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerSyncImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.UniqueValueSerializationStrategyImpl;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.impl.SerializationModule;
import org.apache.usergrid.persistence.collection.service.impl.ServiceModule;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessorImpl;
import org.apache.usergrid.persistence.core.consistency.ConsistencyFig;
import org.apache.usergrid.persistence.core.consistency.LocalTimeoutQueue;
import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.consistency.TimeoutQueue;
import org.apache.usergrid.persistence.graph.guice.GraphModule;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.impl.EsEntityIndexImpl;
import org.safehaus.guicyfig.GuicyFigModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Guice Module that encapsulates Core Persistence.
 */
public class GuiceModule  extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger( GuiceModule.class );

    @Override
    protected void configure() {

        // Graph Module
        install( new GraphModule() );

        // Collection modules
        install( new GuicyFigModule( SerializationFig.class ) );
        install( new SerializationModule() );
        install( new ServiceModule() );
        install( new FactoryModuleBuilder()
            .implement( EntityCollectionManager.class, EntityCollectionManagerImpl.class )
            .implement( EntityCollectionManagerSync.class, EntityCollectionManagerSyncImpl.class )
            .build( EntityCollectionManagerFactory.class ) );
        bind( UniqueValueSerializationStrategy.class ).to( UniqueValueSerializationStrategyImpl.class );

        // QueryIndex modules
        install (new GuicyFigModule( IndexFig.class ));
        install( new FactoryModuleBuilder()
            .implement( EntityIndex.class, EsEntityIndexImpl.class )
            .build( EntityIndexFactory.class ) );

    }   

    @Provides
    @Singleton
    @Inject
    @MvccEntityDelete
    public AsyncProcessor<MvccEntityEvent<MvccEntity>> edgeDelete(
            @MvccEntityDelete final TimeoutQueue<MvccEntityEvent<MvccEntity>> queue, 
            final ConsistencyFig consistencyFig) {

        return new AsyncProcessorImpl<>(queue, consistencyFig);
    }

    @Provides
    @Inject
    @Singleton
    @MvccEntityDelete
    public TimeoutQueue<MvccEntityEvent<MvccEntity>> edgeDeleteQueue(final TimeService timeService) {
        return new LocalTimeoutQueue<>(timeService);
    }
}
