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
package org.apache.usergrid.persistence.graph.guice;


import org.safehaus.guicyfig.GuicyFigModule;

import org.apache.usergrid.persistence.collection.guice.CollectionModule;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.mvcc.event.PostProcessObserver;
import org.apache.usergrid.persistence.graph.EdgeManager;
import org.apache.usergrid.persistence.graph.EdgeManagerFactory;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.impl.CollectionIndexObserver;
import org.apache.usergrid.persistence.graph.impl.EdgeManagerImpl;
import org.apache.usergrid.persistence.graph.serialization.CassandraConfig;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.CassandraConfigImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeMetadataSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.NodeSerializationImpl;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;


public class GraphModule extends AbstractModule {

    @Override
    protected void configure() {

        //configure collections and our core astyanax framework
        install(new CollectionModule());

        //install our configuration
        install (new GuicyFigModule( GraphFig.class ));

        bind( PostProcessObserver.class ).to( CollectionIndexObserver.class );

        bind( EdgeMetadataSerialization.class).to( EdgeMetadataSerializationImpl.class);
        bind( EdgeSerialization.class).to( EdgeSerializationImpl.class );
        bind( NodeSerialization.class).to( NodeSerializationImpl.class );


        bind( CassandraConfig.class).to( CassandraConfigImpl.class );

        // create a guice factory for getting our collection manager
        install( new FactoryModuleBuilder().implement( EdgeManager.class, EdgeManagerImpl.class )
                                           .build( EdgeManagerFactory.class ) );



        //do multibindings for migrations
        Multibinder<Migration> migrationBinding = Multibinder.newSetBinder( binder(), Migration.class );
        migrationBinding.addBinding().to( EdgeMetadataSerializationImpl.class );
        migrationBinding.addBinding().to( EdgeSerializationImpl.class );
        migrationBinding.addBinding().to( NodeSerializationImpl.class );


        /**
         * Graph event bus, will need to be refactored into it's own classes
         */

        final EventBus eventBus = new EventBus("asyncCleanup");
        bind(EventBus.class).toInstance(eventBus);

        //auto register every impl on the event bus
        bindListener( Matchers.any(), new TypeListener() {
           @Override
           public <I> void hear(@SuppressWarnings("unused") final TypeLiteral<I> typeLiteral, final TypeEncounter<I> typeEncounter) {
               typeEncounter.register(new InjectionListener<I>() {
                   @Override public void afterInjection(final I instance) {
                       eventBus.register(instance);
                   }
               });
           }
        });


    }
}
