/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.index.guice;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.multibindings.Multibinder;
import org.apache.usergrid.persistence.collection.guice.MvccEntityDelete;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityEvent;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.MessageListener;
import org.apache.usergrid.persistence.index.IndexFig;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.apache.usergrid.persistence.collection.guice.CollectionModule;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.impl.EsEntityIndexDeleteListener;
import org.apache.usergrid.persistence.index.impl.EsEntityIndexImpl;
import org.safehaus.guicyfig.GuicyFigModule;


public class IndexModule extends AbstractModule {

    @Override
    protected void configure() {

        // configure collections and our core astyanax framework
        install(new CollectionModule());

        // install our configuration
        install (new GuicyFigModule( IndexFig.class ));

        install( new FactoryModuleBuilder()
            .implement( EntityIndex.class, EsEntityIndexImpl.class )
            .build( EntityIndexFactory.class ) );

        Multibinder<MessageListener> messageListenerMultibinder = Multibinder.newSetBinder(binder(), MessageListener.class);

        messageListenerMultibinder.addBinding().toProvider( EsEntityIndexDeleteListenerProvider.class ).asEagerSingleton();
    }

    /**
     * Create the provider for the node delete listener
     */
    public static class EsEntityIndexDeleteListenerProvider
            implements Provider<MessageListener<MvccEntityEvent<MvccEntity>, MvccEntity>> {


        private final AsyncProcessor<MvccEntityEvent<MvccEntity>> entityDelete;
        private final EntityIndex entityIndex;


        @Inject
        public EsEntityIndexDeleteListenerProvider( final EntityIndex entityIndex,
                                                 @MvccEntityDelete final AsyncProcessor<MvccEntityEvent<MvccEntity>> entityDelete) {
            this.entityDelete = entityDelete;
            this.entityIndex = entityIndex;
        }

        @Override
        public MessageListener<MvccEntityEvent<MvccEntity>, MvccEntity> get() {
            return new EsEntityIndexDeleteListener(entityIndex,entityDelete);
        }
    }
}
