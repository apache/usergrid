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
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.asyncevents.AsyncIndexProvider;
import org.apache.usergrid.corepersistence.asyncevents.EventBuilder;
import org.apache.usergrid.corepersistence.asyncevents.EventBuilderImpl;
import org.apache.usergrid.corepersistence.index.*;
import org.apache.usergrid.corepersistence.migration.CoreMigration;
import org.apache.usergrid.corepersistence.migration.CoreMigrationPlugin;
import org.apache.usergrid.corepersistence.migration.EntityTypeMappingMigration;
import org.apache.usergrid.corepersistence.migration.MigrationModuleVersionPlugin;
import org.apache.usergrid.corepersistence.pipeline.PipelineModule;
import org.apache.usergrid.corepersistence.rx.impl.*;
import org.apache.usergrid.corepersistence.service.*;
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
import org.safehaus.guicyfig.GuicyFigModule;


/**
 * Guice Module that encapsulates Core Persistence.
 */
public class CoreModule  extends AbstractModule {



    public static final String EVENTS_DISABLED = "corepersistence.events.disabled";



    @Override
    protected void configure() {


        install( new CommonModule());
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
        Multibinder<DataMigration<EntityIdScope>> dataMigrationMultibinder =
                    Multibinder.newSetBinder( binder(),
                        new TypeLiteral<DataMigration<EntityIdScope>>() {}, CoreMigration.class );


        dataMigrationMultibinder.addBinding().to( EntityTypeMappingMigration.class );


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
