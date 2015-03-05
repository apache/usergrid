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
package org.apache.usergrid.persistence.core.guice;


import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.metrics.MetricsFactoryImpl;
import org.apache.usergrid.persistence.core.metrics.MetricsFig;
import org.apache.usergrid.persistence.core.migration.data.*;
import org.safehaus.guicyfig.GuicyFigModule;

import org.apache.usergrid.persistence.core.astyanax.AstyanaxKeyspaceProvider;
import org.apache.usergrid.persistence.core.astyanax.CassandraConfig;
import org.apache.usergrid.persistence.core.astyanax.CassandraConfigImpl;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.consistency.TimeServiceImpl;
import org.apache.usergrid.persistence.core.migration.data.MigrationPlugin;
import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.core.migration.schema.MigrationManager;
import org.apache.usergrid.persistence.core.migration.schema.MigrationManagerFig;
import org.apache.usergrid.persistence.core.migration.schema.MigrationManagerImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;
import com.netflix.astyanax.Keyspace;


/**
 * Simple module for configuring our core services.  Cassandra etc
 */
public class CommonModule extends AbstractModule {


    @Override
    protected void configure() {
        //noinspection unchecked
        install( new GuicyFigModule( MigrationManagerFig.class, CassandraFig.class ) );

        // bind our keyspace to the AstyanaxKeyspaceProvider
        bind( Keyspace.class ).toProvider( AstyanaxKeyspaceProvider.class ).asEagerSingleton();

        // bind our migration manager
        bind( MigrationManager.class ).to( MigrationManagerImpl.class );



        //do multibindings for migrations
        Multibinder<Migration> migrationBinding = Multibinder.newSetBinder( binder(), Migration.class );
        migrationBinding.addBinding().to( Key.get( MigrationInfoSerialization.class ) );

        bind( TimeService.class ).to( TimeServiceImpl.class );

        bind( CassandraConfig.class ).to( CassandraConfigImpl.class );

        /**
         * Data migration beans
         */
        bind( MigrationInfoSerialization.class ).to( MigrationInfoSerializationImpl.class );

        bind( DataMigrationManager.class ).to( DataMigrationManagerImpl.class );

        bind( MetricsFactory.class ).to( MetricsFactoryImpl.class );

        bind (MigrationInfoCache.class).to( MigrationInfoCacheImpl.class );
        install(new GuicyFigModule(MetricsFig.class));


        //do multibindings for migrations
        //create the empty multibinder so other plugins can use it
         Multibinder.newSetBinder( binder(), MigrationPlugin.class);
    }


}
