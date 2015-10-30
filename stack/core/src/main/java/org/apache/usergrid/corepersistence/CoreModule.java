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


import org.apache.usergrid.persistence.cache.guice.CacheModule;
import org.safehaus.guicyfig.GuicyFigModule;

import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.asyncevents.AsyncIndexProvider;
import org.apache.usergrid.corepersistence.asyncevents.EventBuilder;
import org.apache.usergrid.corepersistence.asyncevents.EventBuilderImpl;
import org.apache.usergrid.corepersistence.index.ApplicationIndexBucketLocator;
import org.apache.usergrid.corepersistence.index.CoreIndexFig;
import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactory;
import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactoryImpl;
import org.apache.usergrid.corepersistence.index.IndexProcessorFig;
import org.apache.usergrid.corepersistence.index.IndexService;
import org.apache.usergrid.corepersistence.index.IndexServiceImpl;
import org.apache.usergrid.corepersistence.index.ReIndexService;
import org.apache.usergrid.corepersistence.index.ReIndexServiceImpl;
import org.apache.usergrid.corepersistence.migration.CoreMigration;
import org.apache.usergrid.corepersistence.migration.CoreMigrationPlugin;
import org.apache.usergrid.corepersistence.migration.DeDupConnectionDataMigration;
import org.apache.usergrid.corepersistence.migration.MigrationModuleVersionPlugin;
import org.apache.usergrid.corepersistence.pipeline.PipelineModule;
import org.apache.usergrid.corepersistence.rx.impl.AllApplicationsObservable;
import org.apache.usergrid.corepersistence.rx.impl.AllApplicationsObservableImpl;
import org.apache.usergrid.corepersistence.rx.impl.AllEntitiesInSystemImpl;
import org.apache.usergrid.corepersistence.rx.impl.AllEntityIdsObservable;
import org.apache.usergrid.corepersistence.rx.impl.AllEntityIdsObservableImpl;
import org.apache.usergrid.corepersistence.rx.impl.AllNodesInGraphImpl;
import org.apache.usergrid.corepersistence.service.AggregationService;
import org.apache.usergrid.corepersistence.service.AggregationServiceFactory;
import org.apache.usergrid.corepersistence.service.AggregationServiceImpl;
import org.apache.usergrid.corepersistence.service.ApplicationService;
import org.apache.usergrid.corepersistence.service.ApplicationServiceImpl;
import org.apache.usergrid.corepersistence.service.CollectionService;
import org.apache.usergrid.corepersistence.service.CollectionServiceImpl;
import org.apache.usergrid.corepersistence.service.ConnectionService;
import org.apache.usergrid.corepersistence.service.ConnectionServiceImpl;
import org.apache.usergrid.corepersistence.service.StatusService;
import org.apache.usergrid.corepersistence.service.StatusServiceImpl;
import org.apache.usergrid.persistence.collection.guice.CollectionModule;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.guice.CommonModule;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.core.migration.data.MigrationDataProvider;
import org.apache.usergrid.persistence.core.migration.data.MigrationPlugin;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.guice.GraphModule;
import org.apache.usergrid.persistence.graph.serialization.impl.migration.GraphNode;
import org.apache.usergrid.persistence.index.guice.IndexModule;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;


/**
 * Guice Module that encapsulates Core Persistence.
 */
public class CoreModule  extends AbstractModule {




    @Override
    protected void configure() {

        install( new CommonModule());
        install( new CacheModule());
        install( new CollectionModule() {
            /**
             * configure our migration data provider for all entities in the system
             */
            @Override
           public void configureMigrationProvider() {

                bind(new TypeLiteral<MigrationDataProvider<EntityIdScope>>(){}).to(
                    AllEntitiesInSystemImpl.class );
           }
        } );
        install( new GraphModule() {

            /**
             * Override the observable that needs to be used for migration
             */
            @Override
            public void configureMigrationProvider() {
                bind( new TypeLiteral<MigrationDataProvider<GraphNode>>() {} ).to(
                    AllNodesInGraphImpl.class );
            }
        } );
        install(new IndexModule(){
            @Override
            public void configureMigrationProvider() {
                bind( new TypeLiteral<MigrationDataProvider<ApplicationScope>>() {} ).to(
                    AllApplicationsObservableImpl.class );
            }
        });
       //        install(new MapModule());   TODO, re-enable when index module doesn't depend on queue
       //        install(new QueueModule());

        bind(ManagerCache.class).to( CpManagerCache.class );
        bind(ApplicationIdCacheFactory.class);


        /**
         * Create our migrations for within our core plugin
         */
        Multibinder<DataMigration> dataMigrationMultibinder =
                    Multibinder.newSetBinder( binder(),
                        new TypeLiteral<DataMigration>() {}, CoreMigration.class );


        dataMigrationMultibinder.addBinding().to( DeDupConnectionDataMigration.class );


        //wire up the collection migration plugin
        final Multibinder<MigrationPlugin> plugins = Multibinder.newSetBinder( binder(), MigrationPlugin.class );
        plugins.addBinding().to( CoreMigrationPlugin.class );
        plugins.addBinding().to( MigrationModuleVersionPlugin.class );

        bind( AllApplicationsObservable.class ).to( AllApplicationsObservableImpl.class );
        bind( AllEntityIdsObservable.class).to( AllEntityIdsObservableImpl.class );


        /*****
         * Indexing service
         *****/


        bind( IndexService.class ).to(IndexServiceImpl.class);

        //bind the event handlers
        bind( EventBuilder.class).to( EventBuilderImpl.class );
        bind(ApplicationIndexBucketLocator.class);

        //bind the queue provider
        bind( AsyncEventService.class ).toProvider( AsyncIndexProvider.class );


        bind( ReIndexService.class).to(ReIndexServiceImpl.class);

        install(new FactoryModuleBuilder()
            .implement(AggregationService.class, AggregationServiceImpl.class)
            .build(AggregationServiceFactory.class));

        bind(IndexLocationStrategyFactory.class).to( IndexLocationStrategyFactoryImpl.class );

        install(new GuicyFigModule(IndexProcessorFig.class));

        install(new GuicyFigModule(CoreIndexFig.class));



        install( new GuicyFigModule( ApplicationIdCacheFig.class ) );

        install( new GuicyFigModule( EntityManagerFig.class ) );

        //install our pipeline modules
        install(new PipelineModule());

        /**
         * Install our service operations
         */

        bind( CollectionService.class).to( CollectionServiceImpl.class );

        bind( ConnectionService.class).to( ConnectionServiceImpl.class);

        bind( ApplicationService.class ).to( ApplicationServiceImpl.class );

        bind( StatusService.class ).to( StatusServiceImpl.class );


    }

}
