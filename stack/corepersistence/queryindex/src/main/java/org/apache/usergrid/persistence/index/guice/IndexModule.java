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

import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.core.migration.data.MigrationPlugin;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.*;
import com.google.inject.AbstractModule;

import org.apache.usergrid.persistence.index.impl.*;
import org.apache.usergrid.persistence.index.migration.EsIndexMappingMigrationPlugin;
import org.apache.usergrid.persistence.index.migration.EsIndexMigrationPlugin;
import org.apache.usergrid.persistence.index.migration.IndexMigration;
import org.apache.usergrid.persistence.map.guice.MapModule;
import org.apache.usergrid.persistence.queue.guice.QueueModule;

import org.safehaus.guicyfig.GuicyFigModule;


public abstract class IndexModule extends AbstractModule {

    @Override
    protected void configure() {

        // install our configuration
        install(new GuicyFigModule(IndexFig.class));

        install(new MapModule());
        install(new QueueModule());

        bind( EntityIndexFactory.class ).to( EsEntityIndexFactoryImpl.class );
        bind(IndexCache.class).to(EsIndexCacheImpl.class);

        bind(IndexProducer.class).to(EsIndexProducerImpl.class).asEagerSingleton();


        //wire up the edg migration. A no-op ATM, but retained for future development
        Multibinder<DataMigration> dataMigrationMultibinder =
                Multibinder.newSetBinder( binder(), new TypeLiteral<DataMigration>() {}, IndexMigration.class );



        //wire up the collection migration plugin
        final Multibinder<MigrationPlugin> plugins = Multibinder.newSetBinder(binder(), MigrationPlugin.class);
        plugins.addBinding().to(EsIndexMigrationPlugin.class);
        plugins.addBinding().to(EsIndexMappingMigrationPlugin.class);

        //invoke the migration plugin config
        configureMigrationProvider();
    }

    /**
     * Gives callers the ability to to configure an instance of
     *
     * MigrationDataProvider<ApplicationScope> for providing data migrations
     */
    public abstract void configureMigrationProvider();

}
