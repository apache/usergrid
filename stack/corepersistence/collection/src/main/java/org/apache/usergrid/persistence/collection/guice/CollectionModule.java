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

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerSync;
import org.apache.usergrid.persistence.collection.astyanax.CassandraFig;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerImpl;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerSyncImpl;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerFig;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.RollbackAction;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.UniqueValueSerializationStrategyImpl;
import org.apache.usergrid.persistence.collection.rx.CassandraThreadScheduler;
import org.apache.usergrid.persistence.collection.rx.RxFig;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccEntitySerializationStrategyImpl;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccLogEntrySerializationStrategyImpl;
import org.apache.usergrid.persistence.collection.serialization.impl.SerializationModule;
import org.apache.usergrid.persistence.collection.service.impl.ServiceModule;
import org.safehaus.guicyfig.GuicyFigModule;
import rx.Scheduler;

/**
 * Simple module for wiring our collection api
 *
 * @author tnine
 */
public class CollectionModule extends AbstractModule {

    @Override
    protected void configure() {

        // noinspection unchecked
        install( new GuicyFigModule( 
                RxFig.class, 
                MigrationManagerFig.class,
                CassandraFig.class, 
                SerializationFig.class ) );

        install( new SerializationModule() );
        bind( MvccEntitySerializationStrategy.class ).to( MvccEntitySerializationStrategyImpl.class );
        bind( MvccLogEntrySerializationStrategy.class ).to( MvccLogEntrySerializationStrategyImpl.class );
        bind( UniqueValueSerializationStrategy.class ).to( UniqueValueSerializationStrategyImpl.class );
        
        install( new ServiceModule() );

        // create a guice factor for getting our collection manager
        install( new FactoryModuleBuilder()
                .implement( EntityCollectionManager.class, EntityCollectionManagerImpl.class )
                .implement( EntityCollectionManagerSync.class, EntityCollectionManagerSyncImpl.class )
                .build( EntityCollectionManagerFactory.class ) );

        bind( Scheduler.class ).toProvider( CassandraThreadScheduler.class );

        bind( RollbackAction.class );
    }
}
