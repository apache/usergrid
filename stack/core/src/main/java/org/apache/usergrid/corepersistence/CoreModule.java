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
import com.google.inject.Provider;
import com.google.inject.multibindings.Multibinder;

import org.apache.usergrid.corepersistence.migration.EntityDataMigration;
import org.apache.usergrid.corepersistence.migration.EntityTypeMappingMigration;
import org.apache.usergrid.corepersistence.migration.GraphShardVersionMigration;
import org.apache.usergrid.corepersistence.events.EntityDeletedHandler;
import org.apache.usergrid.corepersistence.events.EntityVersionCreatedHandler;
import org.apache.usergrid.corepersistence.events.EntityVersionDeletedHandler;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.collection.event.EntityDeleted;
import org.apache.usergrid.persistence.collection.event.EntityVersionCreated;
import org.apache.usergrid.persistence.collection.event.EntityVersionDeleted;
import org.apache.usergrid.persistence.collection.guice.CollectionModule;
import org.apache.usergrid.persistence.core.guice.CommonModule;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.graph.guice.GraphModule;
import org.apache.usergrid.persistence.index.guice.IndexModule;
import org.apache.usergrid.persistence.map.guice.MapModule;
import org.apache.usergrid.persistence.queue.guice.QueueModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;


/**
 * Guice Module that encapsulates Core Persistence.
 */
public class CoreModule  extends AbstractModule {

    /**
     * TODO this is a circular dependency, and should be refactored
     */
    private LazyEntityManagerFactoryProvider lazyEntityManagerFactoryProvider;

    public static final String EVENTS_DISABLED = "corepersistence.events.disabled";



    public CoreModule( final ApplicationContext context ) {
        this.lazyEntityManagerFactoryProvider = new LazyEntityManagerFactoryProvider( context );
    }

    @Override
    protected void configure() {


        //See TODO, this is fugly
        bind(EntityManagerFactory.class).toProvider( lazyEntityManagerFactoryProvider );

        install( new CommonModule());
        install(new CollectionModule());
        install(new GraphModule());
        install(new IndexModule());
        install(new MapModule());
        install(new QueueModule());

        bind(ManagerCache.class).to( CpManagerCache.class );

        Multibinder<DataMigration> dataMigrationMultibinder =
                Multibinder.newSetBinder( binder(), DataMigration.class );
        dataMigrationMultibinder.addBinding().to( EntityTypeMappingMigration.class );
        dataMigrationMultibinder.addBinding().to( GraphShardVersionMigration.class );
        dataMigrationMultibinder.addBinding().to( EntityDataMigration.class );

        Multibinder<EntityDeleted> entityBinder =
            Multibinder.newSetBinder(binder(), EntityDeleted.class);
        entityBinder.addBinding().to(EntityDeletedHandler.class);

        Multibinder<EntityVersionDeleted> versionBinder =
            Multibinder.newSetBinder(binder(), EntityVersionDeleted.class);
        versionBinder.addBinding().to(EntityVersionDeletedHandler.class);

        Multibinder<EntityVersionCreated> versionCreatedMultibinder =
            Multibinder.newSetBinder( binder(), EntityVersionCreated.class );
        versionCreatedMultibinder.addBinding().to(EntityVersionCreatedHandler.class);


    }


    /**
     * TODO, this is a hack workaround due to the guice/spring EMF circular dependency
     * Once the entity managers have been refactored and moved into guice, remove this dependency.
     *
     */
    public static class LazyEntityManagerFactoryProvider implements Provider<EntityManagerFactory>{

        private final ApplicationContext context;


        public LazyEntityManagerFactoryProvider( final ApplicationContext context ) {this.context = context;}



        @Override
        public EntityManagerFactory get() {
            return this.context.getBean( EntityManagerFactory.class );
        }
    }

}
